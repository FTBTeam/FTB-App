<template>
  <div class="find-mods">
    <div class="header flex items-center">
      <ftb-button class="back" color="info px-4 py-2 mr-4" @click="() => goBack()">
        <font-awesome-icon icon="chevron-left" />
      </ftb-button>
      <ftb-search
        v-if="!loading"
        :darkMode="true"
        class="flex-1"
        placeholder="Search for a mod"
        min="3"
        v-model="search"
      />
      <span v-else>Loading...</span>
    </div>

    <div class="body pt-6">
      <div class="stats-bar" v-if="resultingIds.length">
        <div class="stat">
          <div class="text">Loaded in</div>
          <div class="value">{{ initialTimeTaken.toLocaleString() }}ms</div>
        </div>
        <div class="stat">
          <div class="text">Total Load</div>
          <div class="value">{{ timeTaken.toLocaleString() }}ms</div>
        </div>
        <div class="stat">
          <div class="text">Results</div>
          <div class="value">{{ results.length }} / {{ resultingIds.length }}</div>
        </div>
      </div>
      <div class="results">
        <mod-card v-for="mod in results" :key="mod.id" :mod="mod" :instance="instance" :target="target" />
      </div>
      <div class="more-btn flex justify-center">
        <ftb-button
          v-if="results.length > 0"
          color="info"
          :disabled="!hasResults || loadingExtra"
          @click="loadMore"
          class="px-10 py-2 inline-block"
        >
          {{ !hasResults ? 'No more results' : 'Load more results' }}
        </ftb-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { AuthState } from '@/modules/auth/types';
import { Instance } from '@/modules/modpacks/types';
import { Mod, ModSearchResults } from '@/types';
import { debounce } from '@/utils';
import axios from 'axios';
import { Component, Prop, Vue, Watch } from 'vue-property-decorator';
import { Action, State } from 'vuex-class';
import FTBButton from '../FTBButton.vue';
import FTBSearchBar from '../FTBSearchBar.vue';
import ModCard from './ModCard.vue';

@Component({
  components: {
    'ftb-search': FTBSearchBar,
    'ftb-button': FTBButton,
    ModCard,
  },
})
export default class FindMods extends Vue {
  // vuex
  @Action('showAlert') public showAlert: any;
  @Action('hideAlert') public hideAlert: any;
  @State('auth') public auth!: AuthState;

  // Normal
  @Prop() goBack!: () => void;
  @Prop() instance!: Instance;

  private offset = 0;

  search = '';
  loading = true;
  loadingExtra = false;
  target: string = '';

  timeTaken = 0;
  initialTimeTaken = 0;

  results: Mod[] = [];
  resultingIds: number[] = [];

  searchDebounce: any;

  /**
   * If no instance is set, everything is wrong. The instance in this case is being
   * pulled from the instance page vue.
   *
   * Then load the packs version data to pull from the targets list to get the mc
   * version of the pack
   */
  async mounted() {
    if (!this.instance) {
      this.goBack();
      return;
    }

    this.searchDebounce = debounce(() => this.searchForMod(), 500);
    await this.loadInstanceVersion();
  }

  @Watch('search')
  onSearch() {
    this.searchDebounce();
  }

  private async searchForMod() {
    if (this.search === '') {
      this.resetSearch();
      return;
    }

    const start = new Date().getTime();
    const searchResults = await axios.get<ModSearchResults>(
      `${process.env.VUE_APP_MODPACK_API}/public/mod/search/${this.target || 'all'}/100?term=${this.search}`,
    );

    this.resetSearch();
    if (!searchResults || searchResults.data?.total === 0) {
      return;
    }

    this.resultingIds = searchResults.data?.mods || [];
    await this.loadResultsProgressively();

    // TODO: remove later on, kinda helpful right now
    this.timeTaken = new Date().getTime() - start;
    this.initialTimeTaken = this.timeTaken;
  }

  /**
   * Maintaines an offset and hard limit of the results
   */
  private async loadResultsProgressively() {
    if (!this.hasResults || this.loadingExtra) {
      return;
    }

    this.loadingExtra = true;
    const start = new Date().getTime();
    for (let i = this.offset; i < this.offset + 5; i++) {
      // TODO: add error checking here
      if (!this.resultingIds[i]) {
        break;
      }

      const res = await axios.get<Mod>(`${process.env.VUE_APP_MODPACK_API}/public/mod/${this.resultingIds[i]}`);
      this.results.push(res.data);
    }

    this.offset += 5;
    this.loadingExtra = false;
    this.timeTaken += new Date().getTime() - start;
  }

  async loadMore() {
    if (!this.resultingIds.length || !this.hasResults) {
      return;
    }

    this.loadResultsProgressively();
  }

  private resetSearch() {
    this.resultingIds = [];
    this.results = [];
    this.offset = 0;
    this.timeTaken = 0;
    this.initialTimeTaken = 0;
    this.loadingExtra = false;
  }

  /**
   * We can get rid of this once the vuex store is fixed to hold this info...
   */
  private async loadInstanceVersion() {
    this.loading = true;

    // TODO: prevent this by fixing the instance construction to contain the mc version and more version data.
    let packData;
    try {
      packData = await axios.get(
        `${process.env.VUE_APP_MODPACK_API}/${this.auth.token?.attributes.modpackschkey ?? 'public'}/modpack/${
          this.instance.id
        }/${this.instance.versionId}`,
      );
    } catch {
      console.log('Failed to find pack version data...');
    } finally {
      this.loading = false;
    }

    if (!packData || !packData.data?.updated) {
      this.showAlert({
        title: 'Failed!',
        message: 'Could not find instance version data',
        type: 'warning',
      });
      setTimeout(() => {
        this.hideAlert();
      }, 5000);

      this.goBack();
      return;
    }

    this.target = packData.data.targets.find((e: any) => e.type == 'game')?.version ?? '';
  }

  get hasResults() {
    return this.resultingIds.length - this.results.length > 0;
  }
}
</script>

<style lang="scss" scoped>
.stats-bar {
  display: flex;
  justify-content: flex-end;
  font-size: 0.875rem;
  margin-bottom: 1rem;

  .stat {
    margin-left: 1.5rem;
    display: flex;

    .text {
      opacity: 0.8;
      margin-right: 0.5rem;
    }

    .value {
      font-family: Arial, Helvetica, sans-serif;
    }
  }
}
</style>
