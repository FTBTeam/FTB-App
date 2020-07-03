<template>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5" style="margin-bottom: 60px;" v-if="isLoaded">
    <div class="sm:mt-auto lg:mt-unset flex flex-col flex-grow" v-if="recentlyPlayed.length >= 1">
      <h1 class="text-2xl">Recently Played Packs</h1>
      <transition-group
        name="list"
        tag="div"
        class="flex pt-1 flex-wrap overflow-x-auto flex-grow items-stretch"
        appear
      >
        <pack-card-wrapper
          v-for="modpack in recentlyPlayed"
          :list-mode="settingsState.settings.listMode"
          :key="modpack.uuid"
          :versions="modpack.versions"
          :art="modpack.art"
          :installed="true"
          :version="modpack.version"
          :name="modpack.name"
          :instance="modpack"
          :instanceID="modpack.uuid"
        ></pack-card-wrapper>
      </transition-group>
    </div>
    <div class="sm:mb-auto lg:mb-unset flex flex-col flex-grow">
      <h1 class="text-2xl">Featured Packs</h1>
      <transition-group
        name="list"
        tag="div"
        class="flex pt-1 flex-wrap overflow-x-auto flex-grow items-stretch"
        appear
      >
        <pack-card-wrapper
          v-for="(modpack, index) in modpacks.featuredPacks.slice(0,cardsToShow)"
          :key="index"
          :list-mode="settingsState.settings.listMode"
          :packID="modpack.id"
          :versions="modpack.versions"
          :art="modpack.art.length > 0 ? modpack.art.filter((art) => art.type === 'square')[0].url : ''"
          :installed="false"
          :minecraft="'1.7.10'"
          :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
          :versionID="modpack.versions[0].id"
          :name="modpack.name"
          :description="modpack.synopsis"
        >{{modpack.id}}</pack-card-wrapper>
      </transition-group>
    </div>
  </div>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full" v-else>
    <!-- TODO: Add some kinda of loady spinner thing in the middle of the screen -->
    <strong>Loading</strong>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import PackCardWrapper from '@/components/packs/PackCardWrapper.vue';
import { asyncForEach } from '@/utils';
import { State, Action } from 'vuex-class';
import { ModpackState } from '@/modules/modpacks/types';
import { SettingsState } from '@/modules/settings/types';

const namespace: string = 'modpacks';

@Component({
  components: {
    PackCardWrapper,
  },
})
export default class Home extends Vue {
  @State('settings') public settingsState!: SettingsState;
  @State('modpacks') public modpacks: ModpackState | undefined = undefined;
  @Action('loadFeaturedPacks', { namespace }) public loadFeaturedPacks: any;
  private isLoaded: boolean = false;
  private cardsToShow = 3;

  private async mounted() {
    if (this.modpacks == null || this.modpacks.featuredPacks.length <= 0) {
      await this.loadFeaturedPacks();
    }
    this.isLoaded = true;
    const cardSize =  this.settingsState.settings.packCardSize || 2;
    // @ts-ignore
    switch (parseInt(cardSize, 10)) {
      case 5:
        this.cardsToShow = 3;
        break;
      case 4:
        this.cardsToShow = 4;
        break;
      case 3:
        this.cardsToShow = 5;
        break;
      case 2:
        this.cardsToShow = 7;
        break;
      case 1:
        this.cardsToShow = 10;
        break;
      default:
        this.cardsToShow = 10;
        break;
    }
  }

  get recentlyPlayed() {
    return this.modpacks !== undefined
      ? this.modpacks.installedPacks
          .sort((a, b) => {
            return b.lastPlayed - a.lastPlayed;
          })
          .slice(0, 4)
      : [];
  }
}
</script>
