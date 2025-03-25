<script lang="ts" setup>
// import FindMods from '@/components/groups/instance/FindMods.vue';
// import FTBSearchBar from '@/components/ui/input/FTBSearchBar.vue';
// import {sendMessage} from '@/core/websockets/websocketsApi';
// import {
//   BaseData,
//   CurseMetadata,
//   InstanceJson,
//   ModInfo,
//   SugaredInstanceJson,
//   UpdateAvailable
// } from '@/core/@types/javaApi';
// import {containsIgnoreCase, stringIsEmpty} from '@/utils/helpers/stringHelpers';
// import {alertController} from '@/core/controllers/alertController';
// import UiButton from '@/components/ui/UiButton.vue';
// import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
// import {JavaFetch} from '@/core/javaFetch';
// import Loader from '@/components/ui/Loader.vue';
// import UiToggle from '@/components/ui/UiToggle.vue';
// import {emitter} from '@/utils';
// import ClosablePanel from '@/components/ui/ClosablePanel.vue';
// import {createLogger} from '@/core/logger';
//
// @Component({
//   methods: {stringIsEmpty, containsIgnoreCase},
//   components: {
//     ClosablePanel,
//     UiToggle,
//     Loader,
//     UiButton,
//     FTBSearchBar,
//     FindMods,
//   },
// })
// export default class ModpackMods extends Vue {
//   private logger = createLogger("ModpackMods.vue")
//
//   @Prop() packInstalled!: boolean;
//   @Prop() instance!: InstanceJson;
//   @Prop() apiPack!: ModPack;
//
//   updatingModlist = false;
//   togglingShas: string[] = [];
//
//   hiddenMods: string[] = [];
//   search = '';
//
//   modsLoading = false;
//   apiMods: ApiMod[] = [];
//   modlist: ModInfo[] = [];
//
//   searchingForMods = false;
//
//   // Sha1 => [ModInfo, CurseMetadata]
//   modUpdates: Record<string, [ModInfo, CurseMetadata]> = {};
//   modUpdatesAvailableKeys: string[] = [];
//
//   updatingModShas: string[] = [];
//
//   // We're forced to track the packets as we don't get the success response in the same packet as the request response
//   // We also do not get any file data in the response, so we have to track the sha1
//   updatingModPacketToShas: Record<string, string> = {};
//
//   async mounted() {
//     if (this.packInstalled) {
//       emitter.on('ws.message', this.onModUpdateEvent)
//     }
//
//     if (!this.packInstalled && this.apiPack) {
//       const latestVersion = this.apiPack.versions.sort((a, b) => b.id - a.id).find(e => !e.private);
//       if (!latestVersion) {
//         return;
//       }
//
//       const provider = this.apiPack.provider === "modpacks.ch" ? "modpack" : "curseforge";
//       const mods = await toggleBeforeAndAfter(() => JavaFetch.modpacksCh(`${provider}/${this.apiPack.id}/${latestVersion.id}/mods`).execute(), s => this.modsLoading = s)
//       const apiMods = mods?.json<{ mods: ApiMod[] }>()?.mods;
//       console.log(mods?.json<{ mods: ApiMod[] }>())
//       if (!apiMods) {
//         return;
//       }
//
//       this.apiMods = apiMods.sort((a, b) => (a.name ?? a.filename).localeCompare((b.name ?? b.filename)))
//     } else {
//       this.getModList().catch(e => this.logger.error(e))
//     }
//   }
//
//   private async getModList(showAlert = false) {
//     try {
//       const mods = await toggleBeforeAndAfter(() => sendMessage("instanceMods", {
//         uuid: this.instance?.uuid ?? "",
//         _private: this.instance?._private ?? false,
//       }), state => this.updatingModlist = state)
//
//       // try and keep the previous curse data
//       if (this.modlist) {
//         for (const mod of mods.files) {
//           const previous = this.modlist.find(e => e.sha1 === mod.sha1);
//           if (previous) {
//             mod.curse = previous.curse;
//           }
//         }
//       }
//
//       this.modlist = mods.files;
//     } catch (e) {
//       alertController.error("Unable to load mods for this instance...")
//     }
//
//     if (showAlert) {
//       alertController.success('The mods list has been updated')
//     }
//   }
//
//   destroyed() {
//     if (this.packInstalled) {
//       emitter.off('ws.message', this.onModUpdateEvent)
//     }
//   }
//
//   onModUpdateEvent(data: BaseData & any) {
//     if (data.type !== "instanceModUpdate" && data.type !== "instanceInstallModReply" && data.type !== "instanceModRichData") {
//       return;
//     }
//
//     if (data.type === "instanceModRichData") {
//       const mod = this.modlist.find(e => e.sha1 === data.file.sha1);
//       if (mod) {
//         // Set mod.curse but update the vue reactive data
//         Vue.set(mod, 'curse', data.richData);
//       }
//
//       return;
//     }
//
//     if (data.type === "instanceInstallModReply") {
//       if (data.status === "success") {
//         this.getModList(false)
//         if (this.updatingModPacketToShas[data.requestId]) {
//           const sha = this.updatingModPacketToShas[data.requestId];
//           this.updatingModShas.splice(this.updatingModShas.indexOf(sha), 1);
//           delete this.updatingModPacketToShas[data.requestId];
//         }
//       }
//
//       return;
//     }
//
//     const typedPayload = data as UpdateAvailable;
//     this.modUpdates[typedPayload.file.sha1] = [typedPayload.file, typedPayload.update];
//     this.modUpdatesAvailableKeys = Object.keys(this.modUpdates);
//   }
//
//   async updateMod(key: string) {
//     if (!this.modUpdates[key]) {
//       return;
//     }
//
//     const [mod, update] = this.modUpdates[key];
//     const result = await sendMessage("instanceInstallMod", {
//       uuid: this.instance?.uuid,
//       modId: update.curseProject,
//       versionId: update.curseFile
//     })
//
//     if (result.status === "success" || result.status === "processing") {
//       if (result.status === "processing") {
//         this.updatingModPacketToShas[result.requestId] = mod.sha1;
//         this.updatingModShas.push(key);
//       } else {
//         this.getModList(false) // This isn't really something that happens
//       }
//     }
//   }
//
//   updateAll() {
//     for (const key of this.modUpdatesAvailableKeys) {
//       this.updateMod(key).catch(e => this.logger.error(e))
//     }
//   }
//
//   async openModsFolder() {
//     if ((this.instance as SugaredInstanceJson).rootDirs.includes("mods")) {
//       await platform.get.io.openFinder(platform.get.io.pathJoin((this.instance as SugaredInstanceJson).path, "mods"));
//     } else {
//       await platform.get.io.openFinder((this.instance as SugaredInstanceJson).path);
//     }
//   }
//
//   async toggleMod(file: ModInfo) {
//     this.togglingShas.push(file.sha1)
//     try {
//       const toggleQuery = await sendMessage("instanceModToggle", {
//         uuid: this.instance.uuid,
//         fileName: file.fileName,
//         fileId: file.fileId
//       });
//
//       if (toggleQuery.successful) {
//         file.enabled = !file.enabled;
//       } else {
//         alertController.warning( `Failed to ${!file.enabled ? 'enable' : 'disable'} ${file.fileName ?? (file as any).name}`)
//       }
//     } catch (e) {
//       alertController.warning( `Failed to ${!file.enabled ? 'enable' : 'disable'} ${file.fileName ?? (file as any).name}`)
//     } finally {
//       this.togglingShas.splice(this.togglingShas.indexOf(file.sha1), 1);
//     }
//   }
//
//   onSearch(value: string) {
//     this.search = value;
//     if (value === '') {
//       this.hiddenMods = [];
//       return;
//     }
//
//     if (this.packMods) {
//       this.hiddenMods = this.packMods.filter((e) => containsIgnoreCase(e.fileName, value)).map((e) => e.fileName.toLowerCase());
//     }
//
//     if (this.apiMods) {
//       this.hiddenMods = this.apiMods.filter((e) => containsIgnoreCase(e.name ?? e.filename, value)).map((e) => (e.name ?? e.filename).toLowerCase());
//     }
//   }
//
//   get packMods(): ModInfo[] | null {
//     const results = this.modlist.filter(e => this.search === '' ? true : containsIgnoreCase(e.curse?.name ?? e.fileName, this.search));
//     if (results.length === 0 && this.search !== '') {
//       return [{
//         fileId: -1,
//         fileName: 'No results found',
//         version: '',
//         enabled: true,
//         size: 0,
//         sha1: '',
//         curse: {} as any,
//         murmurHash: ''
//       }]
//     }
//
//     return results.sort((a, b) => (a.curse?.name ?? a.fileName).localeCompare((b.curse?.name ?? b.fileName)));
//   }
//
//   get simpleMods() {
//     const results = this.apiMods.filter(e => this.search === '' ? true : containsIgnoreCase(e.name ?? e.filename, this.search));
//     if (results.length === 0 && this.search !== '') {
//       return [{
//         name: 'No results found',
//         synopsis: 'Try searching for something else',
//         icon: '',
//         fileId: -1,
//         curseSlug: '',
//         curseProject: -1,
//         curseFile: -1,
//         stored: -1,
//         filename: '',
//       }]
//     }
//
//     return results.sort((a, b) => (a.name ?? a.filename).localeCompare((b.name ?? b.filename)));
//   }
//
//   get installedMods() {
//     return this.packMods?.filter(e => e.curse).map(e => [e.curse.curseProject, e.curse.curseFile])
//   }
// }
</script>

<script lang="ts">
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
</script>

<template>
  <div class="modpack-mods">
<!-- TODO: [port] fixme -->
    Put me back please
<!--    <div class="flex mb-8 gap-4 items-center relative z-50">-->
<!--      <f-t-b-search-bar placeholder="Search..." :value="search" class="w-full" @input="onSearch" />-->
<!--      <template v-if="packInstalled">-->
<!--        <ui-button type="success" @click="searchingForMods = true" icon="plus" :data-balloon-length="instance.locked ? 'medium' : undefined" :aria-label="instance.locked ? 'This instance is locked, to add more content you will need to unlock it in settings.' : 'Add more mods'" :disabled="instance.locked" />-->
<!--        <ui-button type="info" @click="updateAll" icon="download" :data-balloon-length="instance.locked ? 'medium' : undefined" :aria-label="instance.locked ? 'This instance is locked, to add more content you will need to unlock it in settings.' : 'Update all mods'" :disabled="instance.locked || modUpdatesAvailableKeys.length === 0" />-->
<!--        <ui-button type="info" icon="sync" aria-label="Refresh mod list" aria-label-pos="down-right" :disabled="updatingModlist" @click="getModList(true)" />-->
<!--        <ui-button type="info" @click="openModsFolder" icon="folder" data-balloon-length="medium" aria-label="Open mods folder" />-->
<!--      </template>-->
<!--    </div>-->
<!--    -->
<!--    <div class="mods">-->
<!--      <loader v-if="!packInstalled && modsLoading" />-->
<!--      <template v-if="!packInstalled">-->
<!--        <recycle-scroller :items="simpleMods" key-field="fileId" :item-size="70" class="select-text scroller" v-slot="{ item }">-->
<!--          <div class="flex items-center gap-6 mb-4" :key="item.fileId">-->
<!--            <div class="flex gap-6 items-start flex-1">-->
<!--              <img v-if="item.icon" :src="item.icon" class="rounded mt-2" width="40" alt="">-->
<!--              <div class="placeholder bg-black rounded mt-2" style="width: 40px; height: 40px" v-else-if="item.fileName !== ''"></div>-->
<!--              <div class="main">-->
<!--                <b class="mb-1 block">{{item.name || item.filename}}</b>-->
<!--                <p class="only-one-line">{{item.synopsis}}</p>-->
<!--              </div>-->
<!--            </div>-->
<!--            <div>-->
<!--              <a @click="safeLinkOpen" class="curse-btn cursor-pointer" aria-label="Open on CurseForge" data-balloon-pos="down-right" v-if="item.curseSlug" :href="`https://curseforge.com/minecraft/mc-mods/${item.curseSlug}`">-->
<!--                <img src="../../../assets/curse-logo.svg" width="24" alt="Open on CurseForge" />-->
<!--              </a>-->
<!--            </div>-->
<!--          </div>-->
<!--        </recycle-scroller>-->
<!--      </template>-->
<!--      -->
<!--      <template v-if="packInstalled">-->
<!--        <recycle-scroller class="complex-mod mod" :items="packMods" :item-size="70" key-field="sha1" v-slot="{ item }">-->
<!--          <div class="flex gap-6 items-center mb-4">-->
<!--            <img v-if="item.curse && item.curse.icon" :src="item.curse.icon" class="rounded" width="40" alt="">-->
<!--            <div class="placeholder bg-black rounded mt-2" style="width: 40px; height: 40px" v-else-if="item.fileName !== ''"></div>-->
<!--            -->
<!--            <div class="main flex-1 transition-opacity duration-200" :class="{'opacity-50': !item.enabled}">-->
<!--              <div class="mb-1 block select-text font-bold">-->
<!--                <div v-if="item.curse?.name" class="flex gap-2 items-center">-->
<!--                  <a @click="safeLinkOpen" class="curse-btn cursor-pointer hover:underline" aria-label="Open on CurseForge" data-balloon-pos="down-left" :href="`https://curseforge.com/minecraft/mc-mods/${item.curse.slug}`">-->
<!--                    {{item.curse.name}}-->
<!--                  </a>-->
<!--                </div>-->
<!--                <template v-else>-->
<!--                  {{item.fileName}}-->
<!--                </template>-->
<!--              </div>-->
<!--              <p class="text-sm opacity-75 select-text" v-if="item.curse?.name">{{item.fileName}}</p>-->
<!--              <p class="text-sm opacity-75 text-muted italic" v-else>(Not found on CurseForge)</p>-->
<!--            </div>-->
<!--            -->
<!--            <div class="meta flex gap-6 items-center">-->
<!--              <div class="update" v-if="!instance.locked && modUpdatesAvailableKeys.includes(item.sha1) && !updatingModShas.includes(item.sha1)" :aria-label="`Update available (${modUpdates[item.sha1][0].fileName} -> ${modUpdates[item.sha1][1].name})`" data-balloon-pos="down-right" @click="updateMod(item.sha1)">-->
<!--                <font-awesome-icon icon="download" :fixed-width="true" />-->
<!--              </div>-->
<!--              <div class="updating" v-if="!instance.locked && updatingModShas.includes(item.sha1)">-->
<!--                <font-awesome-icon :spin="true" icon="circle-notch" :fixed-width="true" />-->
<!--              </div>-->
<!--              <ui-toggle class="mr-1" v-if="item.fileName !== ''" :value="item.enabled" @input="() => toggleMod(item)" :disabled="togglingShas.includes(item.sha1)" />-->
<!--            </div>-->
<!--          </div>-->
<!--        </recycle-scroller>-->
<!--      </template>-->
<!--    </div>-->

<!--    <closable-panel-->
<!--      v-if="packInstalled"-->
<!--      :open="searchingForMods"-->
<!--      @close="searchingForMods = false"-->
<!--      :title="`Add mods to ${apiPack ? apiPack.name : ''}`"-->
<!--      subtitle="You can find mods for this pack using the search area below"-->
<!--    >-->
<!--      <find-mods :instance="instance" :installed-mods="installedMods" @modInstalled="getModList" />-->
<!--    </closable-panel>-->
<!--    -->
<!--    -->
  </div>
</template>

<style scoped lang="scss">
.only-one-line {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
