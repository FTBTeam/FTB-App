<template>
  <div class="modpack-mods">
    <div class="px-4 py-3 bg-blue-600 rounded mb-6" v-if="!packInstalled">
      This mod list is representative of the latest version of this Modpack. Different versions of this Modpack may have
      different mods.
    </div>
    <div class="flex mb-8 items-center">
      <f-t-b-search-bar :alpha="true" placeholder="Search..." :value="search" @input="onSearch" />
      <div class="actions flex ml-6">
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
    <div v-for="(file, index) in filteredModList" :key="index">
      <div class="flex flex-row my-4 items-center">
        <p :title="`Version ${file.version}`">{{ file.name.replace('.jar', '') }}</p>
        <div class="ml-auto">
          <span class="rounded text-sm bg-gray-600 py-1 px-2 clean-font">{{ prettyBytes(parseInt(file.size)) }}</span>

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
import FindMods from '@/components/modpack/FindMods.vue';
import FTBSearchBar from '@/components/FTBSearchBar.vue';

@Component({
  components: {
    FTBSearchBar,
    FindMods,
  },
})
export default class ModpackMods extends Vue {
  @Action('showAlert') public showAlert: any;
  @Action('sendMessage') public sendMessage!: any;

  @Prop() modlist!: any[];
  @Prop() updatingModlist!: boolean;
  @Prop() packInstalled!: boolean;

  filteredModList: any[] = [];
  search = '';

  prettyBytes = prettyByteFormat;

  mounted() {
    this.filteredModList = this.modlist;
  }

  @Watch('modlist')
  onModListChange() {
    this.filteredModList = this.modlist;
  }

  onSearch(value: string) {
    if (value === '') {
      this.filteredModList = this.modlist;
      return;
    }

    this.filteredModList = this.modlist.filter(e => e.name.toLowerCase().includes(value.toLowerCase()));
  }
}
</script>

<style lang="scss" scoped>
.clean-font {
  font-family: Arial, Helvetica, sans-serif;
}
</style>
