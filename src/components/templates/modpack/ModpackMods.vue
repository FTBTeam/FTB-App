<template>
  <div class="modpack-mods">
    <div class="flex mb-8 items-center">
      <f-t-b-search-bar :alpha="true" placeholder="Search..." :value="search" class="w-full" @input="onSearch" />
      <div class="actions flex ml-6" v-if="packInstalled">
        <ftb-button color="primary" class="py-3 px-4 whitespace-no-wrap" @click="() => $emit('searchForMods')">
          <font-awesome-icon icon="book" class="mr-2" />
          Add more mods
        </ftb-button>
        <div class="refresh ml-4" aria-label="Refresh mod list" data-balloon-pos="down-right">
          <ftb-button
            @click="() => $emit('getModList', true)"
            class="py-3 px-4"
            color="info"
            :disabled="updatingModlist"
          >
            <font-awesome-icon icon="sync" />
          </ftb-button>
        </div>
      </div>
    </div>
    <div v-for="(file, index) in modlist" :key="index">
      <div class="flex flex-row my-4 items-center" v-show="!filteredModShas.includes(file.sha1)">
        <p
          :class="{ 'opacity-50': packInstalled ? !file.enabled : false }"
          class="duration-150 transition-opacity"
          :title="`Version ${file.version}`"
        >
          {{ (file.fileName ? file.fileName : file.name).replace('.jar', '') }}
        </p>
        <div class="ml-auto flex items-center">
          <span
            :class="{ 'opacity-50': packInstalled ? !file.enabled : false }"
            class="duration-150 transition-opacity rounded text-sm bg-gray-600 py-1 px-2 clean-font"
            >{{ prettyBytes(file.size) }}</span
          >
          <ftb-toggle v-if="packInstalled" :value="file.enabled" :disabled="togglingShas.includes(file.sha1)" @change="() => toggleMod(file)" />

          <!-- TODO: Add matching to sha1 hashes, this isn't valid. // color: isMatched ? 'green' : 'red' -->
          <!-- TODO:Lfind where sha1 data is stored and provide it in a copy action -->
          <!--          <span class="sha-check">-->
          <!--            <font-awesome-icon icon="check-circle" color="lightgreen" class="ml-2 mr-1" /> SHA1-->
          <!--          </span>-->
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { prettyByteFormat } from '@/utils/helpers';
import { Action } from 'vuex-class';
import { Prop, Watch } from 'vue-property-decorator';
import FindMods from '@/components/templates/modpack/FindMods.vue';
import FTBSearchBar from '@/components/atoms/input/FTBSearchBar.vue';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import { Instance } from '@/modules/modpacks/types';
import { sendMessage } from '@/core/websockets/websocketsApi';
import {ModInfo} from '@/core/@types/javaApi';
import {containsIgnoreCase} from '@/utils/helpers/stringHelpers';
import {alertController} from '@/core/controllers/alertController';

@Component({
  components: {
    FTBSearchBar,
    FindMods,
    'ftb-toggle': FTBToggle,
  },
})
export default class ModpackMods extends Vue {
  @Action('sendMessage') public sendMessage!: any;

  @Prop() modlist!: ModInfo[];
  @Prop() updatingModlist!: boolean;
  @Prop() packInstalled!: boolean;
  @Prop() instance!: Instance;
  
  togglingShas: string[] = [];

  filteredModList: string[] = [];
  search = '';

  prettyBytes = prettyByteFormat;
  
  async toggleMod(file: ModInfo) {
    this.togglingShas.push(file.sha1)
    try {
      // TODO: Error check
      const toggleQuery = await sendMessage("instanceModToggle", {
        uuid: this.instance.uuid,
        fileName: file.fileName,
        fileId: file.fileId
      });
      
      file.enabled = !file.enabled;
    } catch (e) {
      // TODO: Show error
      alertController.warning( `Failed to ${!file.enabled ? 'enable' : 'disable'} ${file.fileName ?? (file as any).name}`)
    } finally {
      this.togglingShas.splice(this.togglingShas.indexOf(file.sha1), 1);
    }
  }

  onSearch(value: string) {
    if (value === '') {
      this.filteredModList = [];
      return;
    }

    this.filteredModList = this.modlist
      .filter((e) => !containsIgnoreCase(e.fileName, value))
      .map((e) => e.sha1);

  }
  
  get filteredModShas() {
    return this.filteredModList;
  }
}
</script>
