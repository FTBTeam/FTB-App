<template>
  <div class="px-6 py-4 h-full">
    <div class="search-and-switcher mb-4">
      <div class="switcher shadow">
        <div
          @click="changeTab('ftbsearch')"
          class="btn-icon bg-navbar ftb"
          :class="{ active: currentTab === 'ftbsearch' }"
        >
          <img src="@/assets/ftb-white-logo.svg" alt="" />
        </div>

        <div
          @click="changeTab('cursesearch')"
          class="btn-icon bg-navbar eww-orange"
          :class="{ active: currentTab === 'cursesearch' }"
        >
          <img src="@/assets/curse-logo.svg" alt="" />
        </div>
      </div>
      <FTBSearchBar
        v-model="searchValue"
        :placeholder="`Search ${currentTab === 'ftbsearch' ? 'FTB Modpacks' : 'Curseforge Modpacks'}`"
        :min="3"
      />
    </div>
    <div v-if="modpacks.error" class="m-4 p-3" style="background-color: #e55812">
      <span>{{ modpacks.errorMsg }}</span>
    </div>
    <div
      class="flex flex-col flex-1"
      v-if="!modpacks.loading && !(modpacks.search.length <= 0 && modpacks.curseLoading)"
    >
      <div
        class="flex pt-1 flex-wrap overflow-x-auto items-stretch"
        v-if="modpacks.search.length > 0 || modpacks.searchCurse.length > 0"
      >
        <div v-if="currentTab === 'ftbsearch'" key="ftbsearch" class="w-full">
          <div>
            <div v-if="modpacks.search.length < 1" class="m-4 p-3" style="background-color: #e55812">
              <span>No modpacks found</span>
            </div>
            <div class="flex pt-1 flex-wrap overflow-x-auto items-stretch" appear>
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
                type="0"
              >
                {{ modpack.id }}
              </pack-card-list>
            </div>
          </div>
        </div>
        <div v-else-if="currentTab === 'cursesearch'" key="cursesearch">
          <div v-if="!modpacks.curseLoading">
            <div v-if="modpacks.searchCurse.length < 1" class="m-4 p-3 w-full" style="background-color: #e55812">
              <span>No modpacks found</span>
            </div>
            <div class="flex pt-1 flex-wrap overflow-x-auto items-stretch">
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
                type="1"
                :versions="modpack.versions"
                :tags="modpack.tags"
                :description="modpack.synopsis"
                >{{ modpack.id }}
              </pack-card-list>
            </div>
          </div>
          <div v-else class="flex flex-1 justify-center items-center">
            <Loading class="mt-8" />
          </div>
        </div>
      </div>
      <div v-else>
        <div class="flex pt-1 flex-wrap overflow-x-auto items-stretch" appear>
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
        </div>
      </div>
    </div>
    <div v-else class="flex flex-1 justify-center items-center">
      <Loading class="mt-8" />
    </div>
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

  @Watch('searchValue')
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

.search-and-switcher {
  display: flex;
  .switcher {
    margin-right: 1rem;
    display: flex;
    border-radius: 5px;
    overflow: hidden;

    .btn-icon {
      padding: 0 1.2rem;
      display: flex;
      align-items: center;
      cursor: pointer;
      transition: background-color 0.25s ease-in-out;

      img {
        width: 40px;
        opacity: 0.5;
        transition: opacity 0.25s ease-in-out;
      }

      &.ftb img {
        margin-top: -4px;
      }

      &.active {
        background-color: var(--color-primary-button);
        img {
          opacity: 1;
        }
      }

      &.eww-orange.active {
        background-color: #ff784d;
      }

      &:hover {
        img {
          opacity: 1;
        }
      }
    }
  }
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
