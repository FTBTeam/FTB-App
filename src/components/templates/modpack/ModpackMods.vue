<template>
  <div class="modpack-mods">
    <div class="flex mb-8 gap-4 items-center relative z-50">
      <f-t-b-search-bar placeholder="Search..." :value="search" class="w-full" @input="onSearch" />
      <template v-if="packInstalled">
        <ui-button type="success" @click="searchingForMods = true" icon="plus" data-balloon-length="medium" :aria-label="instance.locked ? 'This instance is locked, to add more content you will need to unlock it in settings.' : 'Add more mods'" :disabled="instance.locked" />
        <ui-button type="info" @click="updateAll" icon="download" data-balloon-length="medium" :aria-label="instance.locked ? 'This instance is locked, to add more content you will need to unlock it in settings.' : 'Update all mods'" :disabled="instance.locked || modUpdatesAvailableKeys.length === 0" />
        <ui-button type="info" icon="sync" aria-label="Refresh mod list" aria-label-pos="down-right" :disabled="updatingModlist" @click="getModList(true)" />
      </template>
    </div>
    
    <div class="mods">
      <loader v-if="!packInstalled && modsLoading" />
      <template v-if="!packInstalled">
        <recycle-scroller :items="simpleMods" key-field="fileId" :item-size="70" class="select-text scroller" v-slot="{ item }">
          <div class=" flex gap-6 items-start mb-4" :key="item.fileId">
            <img v-if="item.icon" :src="item.icon" class="rounded mt-2" width="40" alt="">
            <div class="placeholder bg-black rounded mt-2" style="width: 40px; height: 40px" v-else-if="item.fileName !== ''"></div>
            <div class="main">
              <b class="mb-1 block">{{item.name || item.filename}}</b>
              <p class="only-one-line">{{item.synopsis}}</p>
            </div>
          </div>
        </recycle-scroller>
      </template>
      <template v-if="packInstalled">
        <div class="complex-mod mod" v-for="(file, index) in packMods" :key="index">
          <div class="flex gap-6 items-center mb-4">
            <img v-if="file.curse && file.curse.icon" :src="file.curse.icon" class="rounded" width="40" alt="">
            <div class="placeholder bg-black rounded mt-2" style="width: 40px; height: 40px" v-else-if="file.fileName !== ''"></div>
            
            <div class="main flex-1 transition-opacity duration-200" :class="{'opacity-50': !file.enabled}">
              <b class="mb-1 block">{{file.curse?.name || file.fileName.replace(".jar", "")}}</b>
              <p class="only-one-line">{{file.curse?.synopsis ?? ""}}</p>
            </div>
            
            <div class="meta flex gap-4 items-center">
              <div class="update" v-if="!instance.locked && modUpdatesAvailableKeys.includes(file.sha1)" :aria-label="`Update available (${modUpdates[file.sha1][0].fileName} -> ${modUpdates[file.sha1][1].name})`" data-balloon-pos="down-right" @click="updateMod(file.sha1)">
                <font-awesome-icon icon="download" :fixed-width="true" />
              </div>
              <ui-toggle v-if="file.fileName !== ''" :value="file.enabled" @input="() => toggleMod(file)" :disabled="togglingShas.includes(file.sha1)" />
            </div>
          </div>
        </div>
      </template>
    </div>

    <closable-panel
      v-if="packInstalled"
      :open="searchingForMods"
      @close="searchingForMods = false"
      :title="`Add mods to ${apiPack ? apiPack.name : ''}`"
      subtitle="You can find mods for this pack using the search area below"
    >
      <find-mods :instance="instance" :installed-mods="installedMods" @modInstalled="getModList" />
    </closable-panel>
    
    
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {Prop} from 'vue-property-decorator';
import FindMods from '@/components/templates/modpack/FindMods.vue';
import FTBSearchBar from '@/components/atoms/input/FTBSearchBar.vue';
import {ModPack} from '@/modules/modpacks/types';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {CurseMetadata, InstanceJson, ModInfo, UpdateAvailable} from '@/core/@types/javaApi';
import {containsIgnoreCase, stringIsEmpty} from '@/utils/helpers/stringHelpers';
import {alertController} from '@/core/controllers/alertController';
import UiButton from '@/components/core/ui/UiButton.vue';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {JavaFetch} from '@/core/javaFetch';
import Loader from '@/components/atoms/Loader.vue';
import UiToggle from '@/components/core/ui/UiToggle.vue';
import {emitter} from '@/utils';
import ClosablePanel from '@/components/molecules/ClosablePanel.vue';

export type ApiMod = {
	fileId: number;
	name: string;
	synopsis: string;
	icon: string;
	curseSlug: string;
	curseProject: number;
	curseFile: number;
	stored: number;
	filename: string;
}

@Component({
  methods: {stringIsEmpty, containsIgnoreCase},
  components: {
    ClosablePanel,
    UiToggle,
    Loader,
    UiButton,
    FTBSearchBar,
    FindMods,
  },
})
export default class ModpackMods extends Vue {
  @Prop() packInstalled!: boolean;
  @Prop() instance!: InstanceJson;
  @Prop() apiPack!: ModPack;
  
  updatingModlist = false;
  togglingShas: string[] = [];

  hiddenMods: string[] = [];
  search = '';
  
  modsLoading = false;
  apiMods: ApiMod[] = [];
  modlist: ModInfo[] = [];

  searchingForMods = false;
  
  // Sha1 => [ModInfo, CurseMetadata]
  modUpdates: Record<string, [ModInfo, CurseMetadata]> = {};
  modUpdatesAvailableKeys: string[] = [];
  
  async mounted() {
    if (this.packInstalled) {
      emitter.on('ws.message', this.onModUpdateEvent)
    }
    
    if (!this.packInstalled && this.apiPack) {
      const latestVersion = this.apiPack.versions.sort((a, b) => b.id - a.id).find(e => !e.private);
      if (!latestVersion) {
        return;
      }
      
      const mods = await toggleBeforeAndAfter(() => JavaFetch.modpacksCh(`modpack/${this.apiPack.id}/${latestVersion.id}/mods`).execute(), s => this.modsLoading = s)
      const apiMods = mods?.json<{ mods: ApiMod[] }>()?.mods;
      if (!apiMods) {
        return;
      }
      
      this.apiMods = apiMods.sort((a, b) => (a.name ?? a.filename).localeCompare((b.name ?? b.filename)))
    } else {
      this.getModList().catch(console.error)
    }
  }

  private async getModList(showAlert = false) {
    try {
      const mods = await toggleBeforeAndAfter(() => sendMessage("instanceMods", {
        uuid: this.instance?.uuid ?? "",
        _private: this.instance?._private ?? false,
      }), state => this.updatingModlist = state)

      this.modlist = mods.files;
    } catch (e) {
      alertController.error("Unable to load mods for this instance...")
    }

    if (showAlert) {
      alertController.success('The mods list has been updated')
    }
  }
  
  destroyed() {
    if (this.packInstalled) {
      emitter.off('ws.message', this.onModUpdateEvent)
    }
  }
  
  onModUpdateEvent(data: any) {
    if (data.type !== "instanceModUpdate") {
      return;
    }
    
    const typedPayload = data as UpdateAvailable;
    this.modUpdates[typedPayload.file.sha1] = [typedPayload.file, typedPayload.update];
    this.modUpdatesAvailableKeys = Object.keys(this.modUpdates);
  }
  
  async updateMod(key: string) {
    if (!this.modUpdates[key]) {
      return;
    } 
    
    const [mod, update] = this.modUpdates[key];
    const result = await sendMessage("instanceInstallMod", {
      uuid: this.instance?.uuid,
      modId: update.curseProject,
      versionId: update.curseFile
    })

    // TODO: (M#01) FINISH THIS ENDPOINT
    console.log(result)
  }

  updateAll() {
    
  }
  
  async toggleMod(file: ModInfo) {
    this.togglingShas.push(file.sha1)
    try {
      const toggleQuery = await sendMessage("instanceModToggle", {
        uuid: this.instance.uuid,
        fileName: file.fileName,
        fileId: file.fileId
      });
      
      if (toggleQuery.successful) {
        file.enabled = !file.enabled;
      } else {
        alertController.warning( `Failed to ${!file.enabled ? 'enable' : 'disable'} ${file.fileName ?? (file as any).name}`)
      }
    } catch (e) {
      alertController.warning( `Failed to ${!file.enabled ? 'enable' : 'disable'} ${file.fileName ?? (file as any).name}`)
    } finally {
      this.togglingShas.splice(this.togglingShas.indexOf(file.sha1), 1);
    }
  }

  onSearch(value: string) {
    this.search = value;
    if (value === '') {
      this.hiddenMods = [];
      return;
    }

    if (this.packMods) {
      this.hiddenMods = this.packMods.filter((e) => containsIgnoreCase(e.fileName, value)).map((e) => e.fileName.toLowerCase());
    }
    
    if (this.apiMods) {
      this.hiddenMods = this.apiMods.filter((e) => containsIgnoreCase(e.name ?? e.filename, value)).map((e) => (e.name ?? e.filename).toLowerCase());
    }
  }
  
  get packMods(): ModInfo[] | null {
    const results = this.modlist.filter(e => this.search === '' ? true : containsIgnoreCase(e.curse?.name ?? e.fileName, this.search));
    if (results.length === 0 && this.search !== '') {
      return [{
        fileId: -1,
        fileName: 'No results found',
        version: '',
        enabled: true,
        size: 0,
        sha1: '',
        curse: {} as any
      }]
    }
    
    return results.sort((a, b) => (a.curse?.name ?? a.fileName).localeCompare((b.curse?.name ?? b.fileName)));
  }
  
  get simpleMods() {
    const results = this.apiMods.filter(e => this.search === '' ? true : containsIgnoreCase(e.name ?? e.filename, this.search));
    if (results.length === 0 && this.search !== '') {
      return [{
          name: 'No results found',
          synopsis: 'Try searching for something else',
          icon: '',
          fileId: -1,
          curseSlug: '',
          curseProject: -1,
          curseFile: -1,
          stored: -1,
          filename: '',
        }]
    }
    
    return results.sort((a, b) => (a.name ?? a.filename).localeCompare((b.name ?? b.filename)));
  }

  get installedMods() {
    return this.packMods?.filter(e => e.curse).map(e => [e.curse.curseProject, e.curse.curseFile])
  }
}
</script>

<style scoped lang="scss">
.only-one-line {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
