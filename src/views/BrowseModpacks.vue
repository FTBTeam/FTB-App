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
    
    <div class="mb-6" v-if="searchValue === ''">
      <div class="flex gap-4 text-sm">
        <selection2 class="flex-1" label="Tags" :options="tagOptions" v-model="selectedTag"></selection2>
        <selection2 class="flex-1" label="Version" :options="versionOptions" v-model="selectedVersion"></selection2>
        <selection2 class="flex-1" label="Loader" :options="loaderOptions" v-model="selectedLoader"></selection2>
        <selection2 class="flex-1" label="Sort by" :options="sortOptions" v-model="selectedSort" :allow-deselect="false"></selection2>
      </div>
    </div>

    <message icon="warning" type="danger" class="my-6" v-if="error">
      {{ error }}
    </message>

    <message icon="info" type="info" class="my-6" v-if="!error && searchResults.length === 0 && !loading">
      No results found for '{{ searchValue }}'
    </message>

    <loader class="mt-20"  v-if="(!error && loading)" />

    <div class="result-cards pb-2" v-if="!error && results.length > 0">
      <pack-preview v-for="(pack, index) in results" :partial-pack="pack" :key="index" :provider="currentTab" />
    </div>

    <div class="pages pb-8" v-if="maxPages > 1 && searchValue === '' && searchResults.length">
      <ui-pagination v-model="currentPage" :total="maxPages * 50" :per-page="50" @input="scrollToTop" />
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue, Watch} from 'vue-property-decorator';
import {Action, Getter} from 'vuex-class';
import FTBSearchBar from '@/components/atoms/input/FTBSearchBar.vue';
import {PackProviders} from '@/modules/modpacks/types';
import {Route} from 'vue-router';
import {debounce} from '@/utils';
import {SearchResultPack} from '@/core/@types/modpacks/packSearch';
import {ns} from '@/core/state/appState';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import Loader from '@/components/atoms/Loader.vue';
import PackPreview from '@/components/core/modpack/PackPreview.vue';
import {modpackApi} from '@/core/pack-api/modpackApi';
import UiPagination from '@/components/core/ui/UiPagination.vue';
import {createLogger} from '@/core/logger';
import Selection2, {SelectionOption} from '@/components/core/ui/Selection2.vue';
import {MiscApi} from '@/core/pack-api/miscApi';
import {alertController} from '@/core/controllers/alertController';

const anyOption: SelectionOption = { label: 'Any', value: 'any' };
const loaderOptions: SelectionOption[] = [
  anyOption,
  { label: 'Neoforge', value: 'neoforge' },
  { label: 'Forge', value: 'forge' },
  { label: 'Fabric', value: 'fabric' },
  { label: 'Quilt', value: 'quilt' },
]

const sortOptions: SelectionOption[] = [
  { label: 'Newest', value: 'new' },
  { label: 'Featured', value: 'featured' },
  { label: 'Popular', value: 'popular' },
  { label: 'Updated', value: 'updated' },
]

@Component({
  components: {
    Selection2,
    UiPagination,
    PackPreview,
    Loader,
    FTBSearchBar
  },
})
export default class BrowseModpacks extends Vue {
  @Getter("latestPacks", ns("v2/modpacks")) latestPacks!: number[];
  @Action('getLatestModpacks', ns("v2/modpacks")) getLatestPacks!: () => Promise<number[]>;
  
  private logger = createLogger("BrowseModpacks.vue")

  selectedTag: string = anyOption.value;
  selectedLoader: string = loaderOptions[0].value;
  selectedVersion: string = anyOption.value;
  selectedSort: string = sortOptions[0].value; 

  /**
   * Expose the data to vue
   */
  loaderOptions = loaderOptions;
  sortOptions = sortOptions;
  versionOptions: SelectionOption[] = [
    anyOption
  ];
  tagOptions: SelectionOption[] = [
    anyOption,
  ];

  searchValue: string = '';
  currentTab: PackProviders = 'modpacksch';

  searchResults: SearchResultPack[] = [];

  loading = false;
  error = '';
  currentPage = 1;
  maxPages = 0;

  async mounted() {
    if (this.$route.params.search) {
      this.searchValue = this.$route.params.search;
      await this.searchPacks();
      return;
    }

    try {
      await toggleBeforeAndAfter(async () => {
        const minecraftVersions = await MiscApi.getMinecraftVersions();
        if (!minecraftVersions) {
          alertController.error("Failed to load minecraft versions");
        } else {
          this.versionOptions = [anyOption, ...minecraftVersions
            .filter(e => e.type === 'release')
            .map(e => ({ label: e.name, value: e.name }))];
        }
        
        const tags = await modpackApi.browse.tags('modpacksch');
        if (!tags) {
          alertController.error("Failed to load tags");
        } else {
          this.tagOptions = [anyOption, ...tags.tags.map(e => ({ label: e.name, value: e.id }))];
        }
        
        await this.updatePacks();
      }, (state) => this.loading = state);
    } catch (error) {
      this.logger.error("Failed to load packs", error);
    }
  }

  private async updatePacks(page = 1) {
    this.searchResults = [];
    this.loading = true;
    const tmpModpacks = await modpackApi.browse.browse(this.currentTab, {
      page: page,
      tag: this.selectedTag == 'any' ? undefined : parseInt(this.selectedTag, 10),
      version: this.selectedVersion == 'any' ? undefined : this.selectedVersion,
      loader: this.selectedLoader == 'any' ? undefined : this.selectedLoader,
      sort: this.selectedSort as any,
    })
    this.loading = false;
    
    this.searchResults = tmpModpacks?.packs ?? [];
    this.maxPages = tmpModpacks?.pages ?? 0;
  }
  
  scrollToTop() {
    // Smooth scroll to top
    document.querySelector('.app-content')?.scrollTo({
      top: 0,
      behavior: 'smooth'
    });
  }
  
  @Watch('selectedTag')
  @Watch('selectedVersion')
  @Watch('selectedLoader')
  @Watch('selectedSort')
  async onFilterChange() {
    this.currentPage = 1;
    await this.updatePacks(this.currentPage);
  }
  
  @Watch('currentPage')
  async onPageChange() {
    await this.updatePacks(this.currentPage)
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
    
    if (this.searchValue === '') {
      this.currentPage = 1;
      await this.updatePacks(this.currentPage)
      return;
    }
    
    this.searchResults = [];
    await this.searchPacks();
  }

  @Watch('searchValue')
  onSearch() {
    this.loading = true;
    this.searchResults = [];
    
    if (this.searchValue === '') {
      this.searchResults = [];
      this.updatePacks();
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
    
    try {
      const results = await toggleBeforeAndAfter(() => modpackApi.search.search(this.searchValue, this.currentTab), v => this.loading = v);
      this.searchResults = results?.packs ?? [];
    } catch (error) {
      this.logger.error("Failed to search packs", error);
      this.error = 'Failed to search packs';
    }
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
