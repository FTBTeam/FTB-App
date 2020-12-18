<template>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full">
    <div class="absolute z-50 display-mode-switcher ml-auto top-0" style="right: 118px; -webkit-app-region: no-drag;">
        <div :class="`cursor-pointer px-2 py-1 ${settingsState.settings.listMode === true || settingsState.settings.listMode === 'true' ? 'bg-gray-600' : 'bg-background-lighten'}`" @click="changeToList">
          <font-awesome-icon icon="list" class="cursor-pointer"></font-awesome-icon>
        </div>
        <div :class="`cursor-pointer px-2 py-1 ${settingsState.settings.listMode === false || settingsState.settings.listMode === 'false' ? 'bg-gray-600' : 'bg-background-lighten'}`"  @click="changeToGrid">
          <font-awesome-icon icon="th" class="cursor-pointer"></font-awesome-icon>
        </div>
    </div>
    <!--    <h1 class="text-3xl">Browse Modpacks</h1>-->
    <div class="w-1/2 self-center text-center">
      <FTBSearchBar
              v-model="searchValue"
              placeholder="Search"
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
          <pack-card-wrapper
                  v-for="(modpack, index) in modpacks.search.filter((m) => m.versions.length > 0)"
                  :key="index"
                  :list-mode="settingsState.settings.listMode"
                  :packID="modpack.id"
                  :art="modpack.art.length > 0 ? getArt(modpack) : ''"
                  :installed="false"
                  :minecraft="'1.7.10'"
                  :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
                  :versionID="modpack.versions[0].id"
                  :name="modpack.name"
                  :authors="modpack.authors"
                  :versions="modpack.versions"
                  :tags="modpack.tags"
                  :description="modpack.synopsis"
          >{{modpack.id}}
          </pack-card-wrapper>
        </div>

        <div v-else>
          <div class="flex flex-row items-center w-full mt-4">
            <h1 @click="changeTab('featured')" :class="`cursor-pointer text-2xl mr-4 ${currentTab === 'featured' ? '' : 'text-gray-600'} hover:text-gray-500 border-red-700`">Featured</h1>
            <h1 @click="changeTab('topInstalls')" :class="`cursor-pointer text-2xl mr-4 ${currentTab === 'topInstalls' ? '' : 'text-gray-600'} hover:text-gray-500 border-red-700`">Top Installs</h1>
            <h1 @click="changeTab('topPlays')" :class="`cursor-pointer text-2xl mr-4 ${currentTab === 'topPlays' ? '' : 'text-gray-600'} hover:text-gray-500 border-red-700`">Top Plays</h1>
            <h1 v-if="modpacks.privatePacks.length > 0" @click="changeTab('privatePacks')" :class="`cursor-pointer text-2xl mr-4 ${currentTab === 'privatePacks' ? '' : 'text-gray-600'} hover:text-gray-500 border-red-700`">Private Packs</h1>
            <h1 @click="changeTab('all')" :class="`cursor-pointer text-2xl mr-4 ${currentTab === 'all' ? '' : 'text-gray-600'} hover:text-gray-500 border-red-700`">All</h1>
          </div>
          <transition name="fade" mode="out-in">
            <div v-if="currentTab === 'featured'" key="featured">
              <transition-group
                      name="list"
                      tag="div"
                      class="flex pt-1 flex-wrap overflow-x-auto items-stretch"
                      appear
              >
                <pack-card-wrapper
                        v-for="modpack in modpacks.featuredPacks.slice(0,cardsToShow)"
                        :key="`featured-${modpack.id}`"
                        :list-mode="settingsState.settings.listMode"
                        :packID="modpack.id"
                        :art="modpack.art.length > 0 ? getArt(modpack) : ''"
                        :installed="false"
                        :versions="modpack.versions"
                        :minecraft="'1.7.10'"
                        :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
                        :versionID="modpack.versions[0].id"
                        :name="modpack.name"
                        :authors="modpack.authors"
                        :tags="modpack.tags"
                        :description="modpack.synopsis"
                >{{modpack.id}}
                </pack-card-wrapper>
              </transition-group>
            </div>
            <div v-else-if="currentTab === 'topInstalls'" key="topInstalls">
              <transition-group
                      name="list"
                      tag="div"
                      class="flex pt-1 flex-wrap overflow-x-auto items-stretch"
                      appear
              >
              <pack-card-wrapper
                      v-for="modpack in modpacks.popularInstalls.slice(0,cardsToShow)"
                      :key="`installs-${modpack.id}`"
                    :list-mode="settingsState.settings.listMode"
                      :versions="modpack.versions"
                      :packID="modpack.id"
                      :art="modpack.art.length > 0 ? getArt(modpack) : ''"
                      :installed="false"
                      :minecraft="'1.7.10'"
                      :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
                      :name="modpack.name"
                      :authors="modpack.authors"
                      :tags="modpack.tags"
                      :description="modpack.synopsis"
              >{{modpack.id}}
              </pack-card-wrapper>
              </transition-group>
            </div>
            <div v-else-if="currentTab === 'topPlays'" key="topPlays">
              <transition-group
                      name="list"
                      tag="div"
                      class="flex pt-1 flex-wrap overflow-x-auto items-stretch"
                      appear
              >
                <pack-card-wrapper
                        v-for="modpack in modpacks.popularPlays.slice(0,cardsToShow)"
                        :key="`plays-${modpack.id}`"
                        :list-mode="settingsState.settings.listMode"
                        :packID="modpack.id"
                        :versions="modpack.versions"
                        :art="modpack.art.length > 0 ? modpack.art.filter((art) => art.type === 'square')[0].url : ''"
                        :installed="false"
                        :minecraft="'1.7.10'"
                        :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
                        :versionID="modpack.versions[0].id"
                        :name="modpack.name"
                        :authors="modpack.authors"
                        :tags="modpack.tags"
                        :description="modpack.synopsis"
                >{{modpack.id}}
                </pack-card-wrapper>
              </transition-group>
            </div>
            <div v-else-if="currentTab === 'all'" key="all">
              <transition-group
                      name="list"
                      tag="div"
                      class="flex pt-1 flex-wrap overflow-x-auto items-stretch"
                      appear
              >
                <pack-card-wrapper
                        v-for="modpack in modpacks.all"
                        :key="`plays-${modpack.id}`"
                        :list-mode="settingsState.settings.listMode"
                        :packID="modpack.id"
                        :versions="modpack.versions"
                        :art="modpack.art.length > 0 ? modpack.art.filter((art) => art.type === 'square')[0].url : ''"
                        :installed="false"
                        :minecraft="'1.7.10'"
                        :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
                        :versionID="modpack.versions[0].id"
                        :name="modpack.name"
                        :authors="modpack.authors"
                        :tags="modpack.tags"
                        :description="modpack.synopsis"
                >{{modpack.id}}
                </pack-card-wrapper>
              </transition-group>
            </div>
            <div v-else-if="currentTab === 'privatePacks'" key="privatePacks">
              <transition-group
                      name="list"
                      tag="div"
                      class="flex pt-1 flex-wrap overflow-x-auto items-stretch"
                      appear
              >
                <pack-card-wrapper
                        v-for="modpack in modpacks.privatePacks"
                        :key="`plays-${modpack.id}`"
                        :list-mode="settingsState.settings.listMode"
                        :packID="modpack.id"
                        :versions="modpack.versions"
                        :art="modpack.art.length > 0 ? modpack.art.filter((art) => art.type === 'square')[0].url : ''"
                        :installed="false"
                        :minecraft="'1.7.10'"
                        :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
                        :versionID="modpack.versions[0].id"
                        :name="modpack.name"
                        :authors="modpack.authors"
                        :tags="modpack.tags"
                        :description="modpack.synopsis"
                >{{modpack.id}}
                </pack-card-wrapper>
              </transition-group>
            </div>
          </transition>
        </div>
      </div>
      <div v-else class="flex flex-1">
        <Loading/>
      </div>
    </transition>
  </div>
</template>

<script lang="ts">
import {Component, Vue, Watch} from 'vue-property-decorator';
import {Action, State} from 'vuex-class';
import PackCardWrapper from '@/components/packs/PackCardWrapper.vue';
import Loading from '@/components/Loading.vue';
import {SettingsState, Settings} from '@/modules/settings/types';
import FTBSearchBar from '@/components/FTBSearchBar.vue';
import {ModpackState} from '../modules/modpacks/types';
import {debounce} from '@/utils';
import { Route } from 'vue-router';
import { AuthState } from '../modules/auth/types';

const namespace: string = 'modpacks';

@Component({
    components: {
        PackCardWrapper,
        FTBSearchBar,
        Loading,
    },
})
export default class BrowseModpacks extends Vue {
    @State('auth') public authState!: AuthState;
    @State('settings') public settingsState!: SettingsState;
    @State('modpacks') public modpacks: ModpackState | undefined = undefined;
    @Action('loadFeaturedPacks', {namespace}) public loadFeaturedPacks: any;
    @Action('loadAllPacks', {namespace}) public loadAllPacks: any;
    @Action('saveSettings', {namespace: 'settings'}) public saveSettings: any;
    @Action('getPopularInstalls', {namespace}) public getPopularInstalls: any;
    @Action('getPopularPlays', {namespace}) public getPopularPlays: any;
    @Action('getPrivatePacks', {namespace}) public getPrivatePacks: any;
    @Action('doSearch', {namespace}) public doSearch: any;
    @Action('clearSearch', {namespace}) public clearSearch: any;

    private searchValue: string = '';
    private currentTab: string = 'featured';
    private cardsToShow = 3;

    public isActiveTab(tab: string): boolean {
        return tab === 'home' && this.$route.path === '/' ? true : this.$route.path.startsWith(`/${tab}`);
    }

    public goTo(page: string): void {
        // We don't care about this error!
        this.$router.push(page).catch((err) => {
            return;
        });
    }

public changeToList() {
    const settingsCopy = this.settingsState.settings;
    settingsCopy.listMode = true;
    this.saveSettings(settingsCopy);
  }

  public getArt(modpack: any){
    let artP = modpack.art.filter((art: any) => art.type === 'square' || art.type === 'logo')[0];
    if(artP === undefined){
      return "";
    }
    return artP.url;
  }

  public changeToGrid() {
    const settingsCopy = this.settingsState.settings;
    settingsCopy.listMode = false;
    this.saveSettings(settingsCopy);
  }

    @Watch('$route')
    public onPropertyChanged(value: Route, oldValue: Route) {
      if (value.params.search !== oldValue.params.search) {
          this.searchValue = value.params.search;
          this.doSearch(this.searchValue);
      }
    }
    private debounceSearch: () => void = () => {
    }

    private changeTab(tab: string) {
      this.currentTab = tab;
    }

    private async mounted() {
      if (this.$route.params.search) {
          this.searchValue = this.$route.params.search;
          this.doSearch(this.searchValue);
        } else if (this.modpacks !== undefined && this.modpacks.searchString.length > 0) {
          this.searchValue =  this.modpacks.searchString;
          this.doSearch(this.searchValue);
        } else {
          this.searchValue = '';
          this.clearSearch();
        }
      this.debounceSearch = debounce(() => {
            this.doSearch(this.searchValue);
        }, 1000);
      if (
            this.modpacks === undefined ||
            this.modpacks.popularInstalls === undefined ||
            this.modpacks.popularInstalls.length <= 0 ||
			this.modpacks.popularPlays.length <= 0 ||
            this.modpacks.all === undefined ||
            this.modpacks.all.length <= 0
        ) {
            await this.loadFeaturedPacks();
            await this.getPopularInstalls();
            await this.getPopularPlays();
			await this.loadAllPacks();
            if(this.authState.token !== null){
              await this.getPrivatePacks();
            }
        }

      const cardSize = this.settingsState.settings.packCardSize || 2;
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
        if (this.searchValue === '' || this.searchValue == null) {
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

  .fade-enter, .fade-leave-to /* .fade-leave-active below version 2.1.8 */
  {
    opacity: 0;
  }

  .slide-enter-active, .slide-leave-active {
    transition: all .05s ease;
  }
  .slide-enter, .slide-leave-to
  /* .slide-fade-leave-active below version 2.1.8 */ {
    transform: translateX(-10px);
  }
    .display-mode-switcher {
    display: flex;
    flex-direction: row-reverse;
    background-color: #313131;
  }
</style>
