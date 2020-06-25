<template>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full">
    <h1 class="text-3xl">Browse Modpacks</h1>
    <div class="w-1/2 self-center text-center">
      <FTBSearchBar
        v-model="searchValue"
        placeholder="Search (4 characters minimum)"
        :doSearch="onSearch"
      />
    </div>
    <div v-if="modpacks.error" class="m-4 p-3" style="background-color: #e55812">
      <span>{{modpacks.errorMsg}}</span>
    </div>
    <transition name="fade" appear mode="out-in">
      <div class="flex flex-col" v-if="!modpacks.loading">
        <div
          class="flex pt-1 flex-wrap overflow-x-auto items-stretch"
          v-if="modpacks.search.length > 0"
        >
          <pack-card
            v-for="(modpack, index) in modpacks.search"
            :key="index"
            :packID="modpack.id"
            :art="modpack.art.length > 0 ? modpack.art.filter((art) => art.type === 'square')[0].url : ''"
            :installed="false"
            :minecraft="'1.7.10'"
            :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
            :versionID="modpack.versions[0].id"
            :name="modpack.name"
            :versions="modpack.versions"
            :description="modpack.synopsis"
          >{{modpack.id}}</pack-card>
        </div>

        <div v-else>
          <h1 class="text-2xl">Featured</h1>
          <transition-group
            name="list"
            tag="div"
            class="flex pt-1 flex-wrap overflow-x-auto items-stretch"
            appear
          >
            <pack-card
              v-for="(modpack, index) in modpacks.featuredPacks.slice(0,cardsToShow)"
              :key="index"
              :packID="modpack.id"
              :art="modpack.art.length > 0 ? modpack.art.filter((art) => art.type === 'square')[0].url : ''"
              :installed="false"
              :versions="modpack.versions"
              :minecraft="'1.7.10'"
              :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
              :versionID="modpack.versions[0].id"
              :name="modpack.name"
              :description="modpack.synopsis"
            >{{modpack.id}}</pack-card>
          </transition-group>
          <h1 class="text-2xl">Top Installs</h1>
          <transition-group
            name="list"
            tag="div"
            class="flex pt-1 flex-wrap overflow-x-auto items-stretch"
            appear
          >
            <pack-card
              v-for="(modpack, index) in modpacks.popularInstalls.slice(0,cardsToShow)"
              :key="index"
              :versions="modpack.versions"
              :packID="modpack.id"
              :art="modpack.art.length > 0 ? modpack.art.filter((art) => art.type === 'square')[0].url : ''"
              :installed="false"
              :minecraft="'1.7.10'"
              :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
              :name="modpack.name"
              :description="modpack.synopsis"
            >{{modpack.id}}</pack-card>
          </transition-group>

          <h1 class="text-2xl">Top Plays</h1>
          <transition-group
            name="list"
            tag="div"
            class="flex pt-1 flex-wrap overflow-x-auto items-stretch"
            appear
          >
            <pack-card
              v-for="(modpack, index) in modpacks.popularPlays.slice(0,cardsToShow)"
              :key="index"
              :packID="modpack.id"
              :versions="modpack.versions"
              :art="modpack.art.length > 0 ? modpack.art.filter((art) => art.type === 'square')[0].url : ''"
              :installed="false"
              :minecraft="'1.7.10'"
              :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
              :versionID="modpack.versions[0].id"
              :name="modpack.name"
              :description="modpack.synopsis"
            >{{modpack.id}}</pack-card>
          </transition-group>
        </div>
      </div>
      <div v-else>
        <Loading />
      </div>
    </transition>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from "vue-property-decorator";
import { Action, State } from "vuex-class";
import PackCard from "@/components/packs/PackCard.vue";
import Loading from "@/components/Loading.vue";
import { SettingsState, Settings } from "@/modules/settings/types";
import FTBSearchBar from "@/components/FTBSearchBar.vue";
import { ModpackState } from "../modules/modpacks/types";
import { debounce } from "@/utils";

const namespace: string = "modpacks";

@Component({
  components: {
    PackCard,
    FTBSearchBar,
    Loading
  }
})
export default class BrowseModpacks extends Vue {
  @State("settings") public settingsState!: SettingsState;
  @State("modpacks") public modpacks: ModpackState | undefined = undefined;
  @Action("loadFeaturedPacks", { namespace }) public loadFeaturedPacks: any;
  @Action("getPopularInstalls", { namespace }) public getPopularInstalls: any;
  @Action("getPopularPlays", { namespace }) public getPopularPlays: any;
  @Action("doSearch", { namespace }) public doSearch: any;
  @Action("clearSearch", { namespace }) public clearSearch: any;

  private searchValue: string = "";
  private cardsToShow = 3;
  private debounceSearch: () => void = () => {};

  private async mounted() {
    this.debounceSearch = debounce(() => {
      this.doSearch(this.searchValue);
    }, 1000);
    if (
      this.modpacks === undefined ||
      this.modpacks.popularInstalls === undefined ||
      this.modpacks.popularInstalls.length <= 0 ||
      this.modpacks.popularPlays.length <= 0
    ) {
      await this.loadFeaturedPacks();
      await this.getPopularInstalls();
      await this.getPopularPlays();
    }
    
    const cardSize =  this.settingsState.settings.packCardSize || 2
    //@ts-ignore
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

    // if (this.settingsState.settings.packCardSize === 5) {
    //   this.cardsToShow = 4;
    // }
    // if (this.settingsState.settings.packCardSize === 2) {
    //   this.cardsToShow = 7;
    // }
    // if (this.settingsState.settings.packCardSize >= 2) {
    //   this.cardsToShow = 7;
    // }
    // if (this.settingsState.settings.packCardSize < 2) {
    //   this.cardsToShow = 10;
    // }
  }

  private onSearch() {
    if (this.searchValue === "" || this.searchValue == null) {
      this.clearSearch();
    } else {
      this.debounceSearch();
    }
  }
}
</script>

<style lang="scss">
h1 {
  text-transform: capitalize;
  // font-variant: small-caps;
  /*font-weight:bold;*/
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s;
}
.fade-enter, .fade-leave-to /* .fade-leave-active below version 2.1.8 */ {
  opacity: 0;
}
</style>
