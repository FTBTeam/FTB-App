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
          <div class="text">Buffered</div>
          <div class="value">{{ resultsBuffer.length }}</div>
        </div>
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
        <template v-if="search !== '' && results.length">
          <mod-card v-for="(mod, index) in results" :key="index" :mod="mod" :instance="instance" :target="target" />
        </template>
        <!-- This is so over the top -->
        <div class="no-results" v-else-if="!results.length && search !== '' && !loadingTerm && !loadingExtra">
          <div
            class="fancy"
            data-balloon-length="large"
            aria-label="I'm a missing texture if you didn't know... Get it?"
            data-balloon-blunt
            data-balloon-pos="down"
          >
            <svg viewBox="0 0 268 298" fill="none" xmlns="http://www.w3.org/2000/svg" class="missing-box">
              <path d="M134 134L0 67V230L134 298V134Z" class="inverse" />
              <path d="M134 133.645L67 100V181.853L134 216V133.645Z" class="prim-light" />
              <path d="M67 181.645L0 148V230L67 264V181.645Z" class="prim-light" />
              <path d="M134 134L268 67V230L134 298V134Z" class="inverse" />
              <path d="M134 215.645L201 182V264L134 298V215.645Z" class="prim-dark" />
              <path d="M201 100.645L268 67V148.853L201 182V100.645Z" class="prim-dark" />
              <path d="M134 134L0 67L134 0L268 67L134 134Z" class="inverse" />
              <path d="M67 101L0 67L67 33.5L134 67.5L67 101Z" class="prim-light" />
              <path d="M201 101L134 67.5L201 33.5L268 67L201 101Z" class="prim-light" />
            </svg>
          </div>
          <div class="text">
            <p class="title">No results found 😢</p>
            <div>
              Looks like nothing was found for <code>{{ search }}</code
              >, try a different sarch.
            </div>
          </div>
        </div>
        <div class="make-a-search" v-else-if="search === ''">
          <div class="search-prompt">
            <div class="icon-container">
              <font-awesome-icon icon="search" class="primary" />
              <font-awesome-icon icon="search" class="secondary" />
            </div>
            <p>Use the search box above to find new mods</p>
          </div>
        </div>
        <div class="loading" v-else>
          <font-awesome-icon spin icon="spinner" />
          <p>Loading results</p>
        </div>
      </div>
      <div class="more-btn flex justify-center">
        <ftb-button
          v-if="results.length > 0"
          color="info"
          :disabled="!hasResults || loadingExtra"
          @click="loadMore"
          class="px-10 py-2 inline-block"
        >
          <font-awesome-icon spin icon="spinner" class="mr-2" v-if="hasResults && loadingExtra" />
          {{ !hasResults ? 'No more results' : `${loadingExtra ? 'Loading' : 'Load'} more results` }}
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

  // Handles loading the packs version data
  loading = true;

  // Handles loading results
  loadingExtra = false;

  // Handles loading the inital search term
  loadingTerm = false;

  target: string = '';

  timeTaken = 0;
  initialTimeTaken = 0;

  results: Mod[] = [];
  resultingIds: number[] = [];
  resultsBuffer: Mod[] = [];

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

    this.loadingTerm = true;
    const start = new Date().getTime();
    const searchResults = await axios.get<ModSearchResults>(
      `${process.env.VUE_APP_MODPACK_API}/public/mod/search/${this.target || 'all'}/100?term=${this.search}`,
    );

    this.loadingTerm = false;

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
   * Maintaines an offset and hard limit of the results, at the same time, after
   * each fetch, we make another request to buffer the next 5 results. This makes
   * the 'load more' button seem near instant for anyone not going to fast.
   *
   * NOTE: the buffer could logically get really big but as we're only able
   * to handle 100 results at the max right now, this is a non-issue
   */
  private async loadResultsProgressively() {
    if (!this.hasResults || this.loadingExtra) {
      return;
    }

    this.loadingExtra = true;
    const start = new Date().getTime();
    for (let i = this.offset; i < this.offset + 5; i++) {
      if (!this.resultingIds[i]) {
        break;
      }

      // Pull from the buffer unless it's the first go or something isn't in the buffer
      if (this.resultsBuffer[i]) {
        this.results.push(this.resultsBuffer[i]);
      } else {
        const res = await this.getModFromId(this.resultingIds[i]);
        if (res) {
          this.results.push(res);
        }
      }
    }

    this.offset += 5;
    this.loadingExtra = false;
    this.timeTaken += new Date().getTime() - start;

    this.bufferNextFive();
  }

  /**
   * After one request, we buffer the next 5 to speed up TTL on more results button
   */
  async bufferNextFive() {
    if (!this.hasResults) {
      return;
    }

    // ensure the button can't be pressed until the buffer is finished otherwise weird things will happen
    this.loadingExtra = true;
    const start = new Date().getTime();
    for (let i = this.offset; i < this.offset + 5; i++) {
      if (!this.resultingIds[i]) {
        break;
      }

      const res = await this.getModFromId(this.resultingIds[i]);
      if (res) {
        this.resultsBuffer.push(res);
      }
    }
    this.timeTaken += new Date().getTime() - start;
    this.loadingExtra = false;
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
    this.resultsBuffer = [];
    this.loadingTerm = false;
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

  private async getModFromId(modId: number): Promise<Mod | null> {
    try {
      return (await axios.get<Mod>(`${process.env.VUE_APP_MODPACK_API}/public/mod/${modId}`)).data;
    } catch {
      return null;
    }
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

.loading {
  padding-top: 5rem;
  text-align: center;

  svg {
    margin-bottom: 1rem;
    font-size: 3rem;
  }
}

.search-prompt {
  padding-top: 5rem;
  text-align: center;

  div.icon-container {
    position: relative;
    margin-bottom: 2rem;
    svg {
      position: relative;
      font-size: 4rem;
    }

    .primary {
      animation: moveplz 5s ease-in-out infinite;
    }

    .secondary {
      position: absolute;
      top: 0;
      left: 50%;
      animation: moveplz 5s ease-in-out infinite;
      animation-direction: reverse;
      animation-delay: 0.5s;
      opacity: 0.2;
      font-size: 3rem;
    }
  }

  p {
    font-size: 1.2rem;
  }

  @keyframes moveplz {
    0%,
    100% {
      transform: translateX(0) translateY(0) rotateZ(0deg);
    }

    25% {
      transform: translateX(-10px) translateY(10px) rotateZ(-30deg);
    }

    50% {
      transform: translateX(10px) translateY(-10px) rotateZ(180deg);
    }

    75% {
      transform: translateX(10px) translateY(10px) rotateZ(140deg);
    }
  }
}

.no-results {
  display: flex;
  width: 70%;
  margin: 0 auto;
  align-items: center;
  margin-top: 5rem;

  .text {
    .title {
      font-size: 1.2rem;
      font-weight: bold;
      margin-bottom: 0.5rem;
    }
  }

  .fancy {
    margin-right: 2rem;
    text-align: center;

    .missing-box {
      width: 100px;
      .inverse {
        fill: black;
        animation: box-one 8s ease-in-out infinite;
      }

      .prim-light {
        fill: #f200f3;
        animation: box-two 8s ease-in-out infinite;
        animation-delay: 1s;
      }

      .prim-dark {
        fill: #980098;
        animation: box-three 8s ease-in-out infinite;
        animation-delay: 1s;
      }

      @keyframes box-one {
        0%,
        100% {
          fill: black;
        }
        50% {
          fill: #f200f3;
        }
      }

      @keyframes box-two {
        0%,
        100% {
          fill: #f200f3;
        }
        50% {
          fill: #161616;
        }
      }

      @keyframes box-three {
        0%,
        100% {
          fill: #980098;
        }
        50% {
          fill: black;
        }
      }
    }
  }
}
</style>
