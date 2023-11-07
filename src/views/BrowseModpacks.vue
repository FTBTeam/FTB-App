<template>
  <div class="px-6 py-4 h-full search-container">
    <div class="search-and-switcher mb-4">
      <div class="switcher shadow">
        <div
          @click="changeTab('modpacksch')"
          class="btn-icon bg-navbar ftb"
          :class="{ active: currentTab === 'modpacksch' }"
        >
          <img src="@/assets/images/ftb-logo.svg" alt="" />
        </div>

        <div
          @click="changeTab('curseforge')"
          class="btn-icon bg-navbar eww-orange"
          :class="{ active: currentTab === 'curseforge' }"
        >
          <img src="@/assets/curse-logo.svg" alt="" />
        </div>
      </div>
      <FTBSearchBar
        v-model="searchValue"
        class="w-full"
        :placeholder="`Search ${currentTab === 'modpacksch' ? 'FTB Modpacks' : 'Curseforge Modpacks'}`"
        :min="3"
      />
    </div>

    <message icon="warning" type="danger" class="my-6" v-if="error">
      {{ error }}
    </message>

    <message icon="info" type="info" class="my-6" v-if="!error && searchResults.length === 0 && searchValue.length > 0 && !loading">
      No results found for '{{ searchValue }}'
    </message>

    <loader class="mt-20"  v-if="(!error && searchValue !== '' && loading && !loadingInitialPacks)" />

    <div class="result-cards pb-2" v-if="!error && results.length > 0">
      <pack-preview v-for="(pack, index) in results" :partial-pack="pack" :key="index" :provider="currentTab" />
    </div>
    
    <div class="latest-packs" v-if="searchValue === '' && !loading">
      <div class="result-cards pb-2">
        <pack-preview v-for="(packId, index) in visiblePacks" :pack-id="packId" :key="`${currentPage}:${index}`" />
      </div>

      <div class="flex justify-center pb-8">
        <ui-pagination v-if="ourPackIds.length" v-model="currentPage" :total="ourPackIds.length" :per-page="10" @input="scrollToTop" />
      </div>
      <loader class="mt-20" v-if="loadingInitialPacks" />
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue, Watch} from 'vue-property-decorator';
import {Action, Getter, State} from 'vuex-class';
import FTBSearchBar from '@/components/atoms/input/FTBSearchBar.vue';
import {PackProviders} from '@/modules/modpacks/types';
import {Route} from 'vue-router';
import {AuthState} from '@/modules/auth/types';
import {debounce} from '@/utils';
import {SearchResultPack} from '@/core/@types/modpacks/packSearch';
import {ns} from '@/core/state/appState';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import Loader from '@/components/atoms/Loader.vue';
import PackPreview from '@/components/core/modpack/PackPreview.vue';
import {modpackApi} from '@/core/pack-api/modpackApi';
import UiPagination from '@/components/core/ui/UiPagination.vue';
import {packBlacklist} from '@/core/state/modpacks/modpacksState';

@Component({
  components: {
    UiPagination,
    PackPreview,
    Loader,
    FTBSearchBar
  },
})
export default class BrowseModpacks extends Vue {
  @Getter("latestPacks", ns("v2/modpacks")) latestPacks!: number[];
  @Action('getLatestModpacks', ns("v2/modpacks")) getLatestPacks!: () => Promise<number[]>;
  
  @State('auth') public authState!: AuthState;

  searchValue: string = '';
  currentTab: PackProviders = 'modpacksch';

  searchResults: SearchResultPack[] = [];

  loading = false;
  
  error = '';
  
  loadingInitialPacks = false;
  
  ourPackIds: number[] = [];
  currentPage = 1;
  visiblePacks: number[] = [];

  async mounted() {
    if (this.$route.params.search) {
      this.searchValue = this.$route.params.search;
      await this.searchPacks();
      return;
    }

    await toggleBeforeAndAfter(async () => {
      const data = await Promise.all([
        await modpackApi.modpacks.getModpacks(),
        await modpackApi.modpacks.getPrivatePacks()
      ])
      
      const allPackIds = new Set(data.flatMap(e => e?.packs ?? []));
      this.ourPackIds = [...allPackIds].sort((a, b) => b - a);
      
      // remove the modloader packs
      this.ourPackIds = this.ourPackIds.filter(e => !packBlacklist.includes(e));
      
      this.visiblePacks = this.ourPackIds.slice(0, 10);
    }, (state) => this.loadingInitialPacks = state);
  }

  scrollToTop() {
    // Smooth scroll to top
    document.querySelector('.app-content')?.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  }
  
  @Watch('currentPage')
  onPageChange() {
    this.visiblePacks = this.ourPackIds.slice((this.currentPage - 1) * 10, ((this.currentPage - 1) * 10 + 10));
  }
  
  @Watch('$route')
  public async onPropertyChanged(value: Route, oldValue: Route) {
    if (value.params.search !== oldValue.params.search) {
      this.searchValue = value.params.search;
      await this.searchPacks();
    }
  }

  private debounceSearch = debounce(() => {
    this.searchPacks();
  }, 500);

  async changeTab(tab: PackProviders) {
    this.currentTab = tab;
    this.searchResults = [];
    await this.searchPacks();
  }

  @Watch('searchValue')
  onSearch() {
    this.loading = true;
    this.searchResults = [];
    
    if (this.searchValue === '') {
      this.loading = false;
      return;
    }

    this.error = '';
    this.debounceSearch();
  }

  async searchPacks() {
    if (this.searchValue.length < 2) {
      return;
    }

    this.loading = true;
    
    const results = await toggleBeforeAndAfter(() => modpackApi.search.search(this.searchValue, this.currentTab), v => this.loading = v);
    this.searchResults = results?.packs ?? [];
  }

  get results(): SearchResultPack[] {
    return this.searchResults;
  }
}
</script>

<style scoped lang="scss">
.result-cards {
  position: relative;
  z-index: 1;
}

.search-and-switcher {
  position: relative;
  display: flex;
  z-index: 1;

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
        filter: saturate(0%);
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
</style>
