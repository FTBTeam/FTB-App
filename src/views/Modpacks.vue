<template>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full">
    <h1 class="text-3xl">My Modpacks</h1>
    <div class="w-1/2 self-center">
      <FTBSearchBar v-model="searchTerm" placeholder="Search" :doSearch="() => {}"/>
    </div>
    <div class="flex flex-col w-full">
      <div class="justify-center">
        <transition-group name="list" tag="div" class="flex pt-1 flex-wrap overflow-x-auto px-auto justify-center" v-if="modpacks.installedPacks.length > 0" appear>
        <pack-card
            v-for="modpack in packs"
            :key="modpack.uuid"
            :art="modpack.art"
            :installed="true"
            :minecraft="'1.7.10'"
            :version="modpack.version"
            :versions="modpack.versions"
            :name="modpack.name"
            :instance="modpack"
            :instanceID="modpack.uuid">
        </pack-card>
        <pack-card :fake=true key="fake_1"></pack-card>
        <pack-card :fake=true key="fake_2"></pack-card>
        <pack-card :fake=true key="fake_3"></pack-card>
        <pack-card :fake=true key="fake_4"></pack-card>
        </transition-group>
      <div class="flex pt-1 flex-wrap overflow-x-auto justify-center" v-else>
        <!-- TODO: Make this pretty -->
        <span>You have no modpacks installed! Find one <router-link to="/browseModpacks"> here</router-link>.</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import PackCard from '@/components/packs/PackCard.vue';
import FTBSearchBar from '@/components/FTBSearchBar.vue';
import { ModpackState, ModPack, Instance } from '@/modules/modpacks/types';
import { State } from 'vuex-class';
import { asyncForEach } from '../utils';

@Component({
    components: {
        PackCard,
        FTBSearchBar,
    },
})
export default class Modpacks extends Vue {
  @State('modpacks') public modpacks: ModpackState | undefined = undefined;
  private searchTerm: string = '';

  get packs(): Instance[] {
    return this.modpacks == null ? [] : this.searchTerm.length > 0 ? this.modpacks.installedPacks.filter((pack) => {
      return pack.name.search(new RegExp(this.searchTerm, 'gi')) !== -1;
    }) : this.modpacks.installedPacks.sort((a, b) => {
      return b.lastPlayed - a.lastPlayed;
    });
  }

}
</script>
