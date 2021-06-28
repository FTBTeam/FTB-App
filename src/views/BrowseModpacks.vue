<template>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full">
    <div class="w-1/2 self-center text-center">
      <FTBSearchBar v-model="searchValue" placeholder="Search" :doSearch="onSearch" />
    </div>
    <div v-if="modpacks.error" class="m-4 p-3" style="background-color: #e55812">
      <span>{{ modpacks.errorMsg }}</span>
    </div>
    <transition name="fade" appear mode="out-in">
      <div class="flex flex-col" v-if="!modpacks.loading && !(modpacks.search.length <= 0 && modpacks.curseLoading)">
        <div
          class="flex pt-1 flex-wrap overflow-x-auto items-stretch"
          v-if="modpacks.search.length > 0 || modpacks.searchCurse.length > 0"
        >
          <div class="flex flex-row items-center w-full mt-4">
            <h1
              @click="changeTab('ftbsearch')"
              :class="
                `cursor-pointer text-2xl mr-4 ${
                  currentTab === 'ftbsearch' ? '' : 'text-gray-600'
                } hover:text-gray-500 border-red-700`
              "
            >
              FTB Modpacks
            </h1>
            <h1
              @click="changeTab('cursesearch')"
              :class="
                `cursor-pointer text-2xl mr-4 ${
                  currentTab === 'cursesearch' ? '' : 'text-gray-600'
                } hover:text-gray-500 border-red-700`
              "
            >
              Curseforge Modpacks
            </h1>
          </div>
          <transition name="fade" mode="out-in">
            <div v-if="currentTab === 'ftbsearch'" key="ftbsearch" class="w-full">
              <div>
                <div v-if="modpacks.search.length < 1" class="m-4 p-3" style="background-color: #e55812">
                  <span>No modpacks found</span>
                </div>
                <transition-group
                  name="list"
                  tag="div"
                  class="flex pt-1 flex-wrap overflow-x-auto items-stretch"
                  appear
                >
                  <pack-card-list
                    v-for="(modpack, index) in modpacks.search.filter(m => m.versions.length > 0)"
                    :key="index"
                    :packID="modpack.id"
                    :art="modpack.art"
                    :installed="false"
                    :minecraft="'1.7.10'"
                    :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
                    :versionID="modpack.versions[0].id"
                    :name="modpack.name"
                    :authors="modpack.authors"
                    :versions="modpack.versions"
                    :tags="modpack.tags"
                    :description="modpack.synopsis"
                    >{{ modpack.id }}
                  </pack-card-list>
                </transition-group>
              </div>
            </div>
            <div v-else-if="currentTab === 'cursesearch'" key="cursesearch">
              <div v-if="!modpacks.curseLoading">
                <div v-if="modpacks.searchCurse.length < 1" class="m-4 p-3 w-full" style="background-color: #e55812">
                  <span>No modpacks found</span>
                </div>
                <transition-group
                  name="list"
                  tag="div"
                  class="flex pt-1 flex-wrap overflow-x-auto items-stretch"
                  appear
                >
                  <pack-card-list
                    v-for="(modpack, index) in modpacks.searchCurse.filter(m => m.versions.length > 0)"
                    :key="index"
                    :packID="modpack.id"
                    :art="modpack.art"
                    :installed="false"
                    :minecraft="'1.7.10'"
                    :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
                    :versionID="modpack.versions[0].id"
                    :name="modpack.name"
                    :authors="modpack.authors"
                    :type="1"
                    :versions="modpack.versions"
                    :tags="modpack.tags"
                    :description="modpack.synopsis"
                    >{{ modpack.id }}
                  </pack-card-list>
                </transition-group>
              </div>
              <div v-else class="flex flex-1">
                <Loading />
              </div>
            </div>
          </transition>
        </div>
        <div v-else>
          <transition name="fade" mode="out-in">
            <transition-group name="list" tag="div" class="flex pt-1 flex-wrap overflow-x-auto items-stretch" appear>
              <pack-card-list
                v-for="modpack in modpacks.all"
                :key="`plays-${modpack.id}`"
                :list-mode="settingsState.settings.listMode"
                :packID="modpack.id"
                :versions="modpack.versions"
                :art="modpack.art"
                :installed="false"
                :minecraft="'1.7.10'"
                :version="modpack.versions.length > 0 ? modpack.versions[0].name : 'unknown'"
                :versionID="modpack.versions[0].id"
                :name="modpack.name"
                :authors="modpack.authors"
                :tags="modpack.tags"
                :description="modpack.synopsis"
                :featured="modpack.featured"
                >{{ modpack.id }}
              </pack-card-list>
            </transition-group>
          </transition>
        </div>
      </div>
      <div v-else class="flex flex-1">
        <Loading />
      </div>
    </transition>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import { Action, State } from 'vuex-class';
import PackCardList from '@/components/packs/PackCardList.vue';
import Loading from '@/components/Loading.vue';
import { SettingsState } from '@/modules/settings/types';
import FTBSearchBar from '@/components/FTBSearchBar.vue';
import { ModpackState } from '../modules/modpacks/types';
import { debounce } from '@/utils';
import { Route } from 'vue-router';
import { AuthState } from '../modules/auth/types';

const namespace: string = 'modpacks';

// TODO: THIS DOESN'T WORK RIGHT NOW, hopefully it will after the vue has been fixed
@Component({
  components: {
    PackCardList,
    FTBSearchBar,
    Loading,
  },
})
export default class BrowseModpacks extends Vue {
  @State('auth') public authState!: AuthState;
  @State('settings') public settingsState!: SettingsState;
  @State('modpacks') public modpacks: ModpackState | undefined = undefined;
  @Action('loadFeaturedPacks', { namespace }) public loadFeaturedPacks: any;
  @Action('loadAllPacks', { namespace }) public loadAllPacks: any;
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @Action('getPopularInstalls', { namespace }) public getPopularInstalls: any;
  @Action('getPopularPlays', { namespace }) public getPopularPlays: any;
  @Action('getPrivatePacks', { namespace }) public getPrivatePacks: any;
  @Action('doSearch', { namespace }) public doSearch: any;
  @Action('clearSearch', { namespace }) public clearSearch: any;

  private searchValue: string = '';
  private currentTab: string = 'ftbsearch';
  private cardsToShow = 20;

  public isActiveTab(tab: string): boolean {
    return tab === 'home' && this.$route.path === '/' ? true : this.$route.path.startsWith(`/${tab}`);
  }

  public goTo(page: string): void {
    // We don't care about this error!
    this.$router.push(page).catch(err => {
      return;
    });
  }

  @Watch('$route')
  public onPropertyChanged(value: Route, oldValue: Route) {
    if (value.params.search !== oldValue.params.search) {
      this.searchValue = value.params.search;
      this.doSearch(this.searchValue);
    }
  }
  private debounceSearch: () => void = () => {};

  private changeTab(tab: string) {
    this.currentTab = tab;
  }

  private async mounted() {
    if (this.$route.params.search) {
      this.searchValue = this.$route.params.search;
      this.doSearch(this.searchValue);
    } else if (this.modpacks !== undefined && this.modpacks.searchString.length > 0) {
      this.searchValue = this.modpacks.searchString;
      this.doSearch(this.searchValue);
    } else {
      this.searchValue = '';
      this.clearSearch();
    }
    this.debounceSearch = debounce(() => {
      if (this.searchValue !== this.modpacks?.searchString) {
        this.doSearch(this.searchValue);
      }
    }, 1000);
    if (this.modpacks === undefined || this.modpacks.all === undefined || this.modpacks.all.length <= 0) {
      await this.loadAllPacks();
      if (this.authState.token !== null) {
        await this.getPrivatePacks();
      }
    }
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
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.05s ease;
}
.slide-enter,
.slide-leave-to {
  transform: translateX(-10px);
}
.display-mode-switcher {
  display: flex;
  flex-direction: row-reverse;
  background-color: #313131;
}
</style>
