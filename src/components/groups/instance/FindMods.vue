<template>
  <div class="find-mods">
    <div class="header flex items-center mt-2">
      <ftb-search :alpha="true" class="w-full flex-1" placeholder="Search for a mod" min="3" v-model="search" />
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
        <template v-if="results.length">
          <mod-card
            v-for="(mod, index) in results"
            :key="index"
            :mod="mod"
            :instance="instance"
            :installed-mods="installedMods"
            :target="target"
            @install="selectedMod = mod"
          />
        </template>
        <!-- This is so over the top -->
        <div class="loading" v-else-if="search !== '' && (loadingTerm || visualLoadingFull)">
          <font-awesome-icon spin icon="spinner" />
          <p>Loading results</p>
        </div>
        <div
          class="no-results"
          v-else-if="search !== '' && !loadingTerm && !visualLoadingFull && !results.length"
        >
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
              >, try a different search term.
            </div>
          </div>
        </div>
        <div class="make-a-search" v-else>
          <div class="search-prompt">
            <div class="icon-container">
              <font-awesome-icon icon="search" class="primary" />
              <font-awesome-icon icon="search" class="secondary" />
            </div>
            <p v-if="!loadingExtra && !loading">Use the search box above to find new mods</p>
            <p v-else>Searching...</p>
          </div>
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
    
    <install-mod-modal
      :open="selectedMod !== null"
      @close="selectedMod = null"
      :mod="selectedMod ?? undefined"
      :mc-version="target" 
      :mod-loader="modLoader"
      :instance="instance"
      @installed="$emit('modInstalled')"
    />
  </div>
</template>

<script lang="ts">
import {Mod} from '@/types';
import {debounce} from '@/utils';
import {Component, Prop, Vue, Watch} from 'vue-property-decorator';
import FTBSearchBar from '@/components/ui/input/FTBSearchBar.vue';
import ModCard from '@/components/groups/modpack/ModCard.vue';
import {modpackApi} from '@/core/pack-api/modpackApi';
import {InstanceJson} from '@/core/@types/javaApi';
import InstallModModal from '@/components/modals/InstallModModal.vue';
import {compatibleCrossLoaderPlatforms, resolveModloader} from '@/utils/helpers/packHelpers';

@Component({
  components: {
    InstallModModal,
    'ftb-search': FTBSearchBar,
    ModCard,
  },
})
export default class FindMods extends Vue {
  // Normal
  @Prop() instance!: InstanceJson;
  @Prop() installedMods!: [number, number][];

  selectedMod: Mod | null = null;
  
  private offset = 0;

  search = '';

  // Handles loading the packs version data
  loading = true;

  // Handles loading results
  loadingExtra = false;

  // Handles loading the inital search term
  loadingTerm = false;
  visualLoadingFull = false;

  target: string = '';
  modLoader: string = '';

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
    // Clean up all data, we have a new request
    this.resetSearch();

    this.loadingTerm = true;
    this.visualLoadingFull = true;
    const start = new Date().getTime();

    let searchResults: number[] = [];
    const targetLoaders = compatibleCrossLoaderPlatforms(this.target, this.modLoader);
    if (targetLoaders.length > 1) {
      for (const loader of targetLoaders) {
        const mods = (await modpackApi.search.modSearch(this.search, this.target, loader as any))?.mods ?? [];
        mods.forEach(e => searchResults.push(e))
      }
    } else {
        searchResults = (await modpackApi.search.modSearch(this.search, this.target, this.modLoader as any))?.mods ?? []; 
    }
    
    this.loadingTerm = false;
    if (!searchResults.length) {
      this.visualLoadingFull = false;
      return;
    }

    this.resultingIds = searchResults || [];
    await this.loadResultsProgressively();
    
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

    if (this.offset === 0) {
      this.visualLoadingFull = false;
    }

    this.offset += 5;
    this.loadingExtra = false;
    this.timeTaken += new Date().getTime() - start;

    await this.bufferNextFive();
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

    await this.loadResultsProgressively();
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
        
    this.target = this.instance.mcVersion;
    this.modLoader = resolveModloader(this.instance)
  }

  private async getModFromId(modId: number): Promise<Mod | null> {
    return modpackApi.search.modFetch(modId)
  }

  get hasResults() {
    return this.resultingIds.length - this.results.length > 0;
  }
}
</script>

<style lang="scss" scoped>
.find-mods {
}

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
