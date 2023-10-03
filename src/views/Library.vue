<template>
  <div class="mod-packs h-full">
    <div class="page-spacing" v-if="!loading && instances.length > 0">
      <header class="flex gap-4 mb-6 items-end">
        <FTBSearchBar v-model="searchTerm" placeholder="Search" class="flex-1" />
        <selection2 label="Group by" />
        <selection2 label="Filter" />
        <selection2 label="Sort by" />
      </header>

      <div class="categories">
        <div class="category" v-for="(category, index) in groupedPacks" :key="`category-${index}`">
          <h2 v-if="Object.keys(groupedPacks).length > 1">{{ index }}</h2>
          <div class="pack-card-grid">
            <template v-for="instance in category">
              <pack-card2
                v-show="filteredInstance === null || filteredInstance.includes(instance.uuid)"
                :key="instance.uuid"
                :instance="instance"
              />
            </template>
          </div>
        </div>
      </div>
    </div>
    
    <loader v-else-if="loading" />

    <div class="flex flex-1 flex-wrap justify-center flex-col items-center no-packs" v-else>
      <div class="message flex flex-1 flex-wrap items-center flex-col mt-32">
        <font-awesome-icon icon="heart-broken" size="6x" />
        <h1 class="text-5xl">Oh no!</h1>
        <span class="mb-4 w-3/4 text-center">
          Would you look at that! Looks like you've got no modpacks installed yet... If you know what you want, click
          Browse and search through our collection and all of CurseForge modpacks, otherwise, use Discover we've got
          some great recommended packs.</span
        >
        <div class="flex flex-row justify-between my-2">
          <router-link to="/browseModpacks">
            <ftb-button color="info" class="py-2 px-8 mx-2">
              <font-awesome-icon icon="search" class="mr-2" />
              Browse
            </ftb-button>
          </router-link>
          <router-link to="/browseModpacks">
            <ftb-button color="info" class="py-2 px-8 mx-2">
              <font-awesome-icon icon="download" class="mr-2" />
              Import
            </ftb-button>
          </router-link>
<!--          <router-link to="/discover">-->
<!--            <ftb-button color="primary" class="py-2 px-6 mx-2">Discover</ftb-button>-->
<!--          </router-link>-->
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import FTBSearchBar from '@/components/atoms/input/FTBSearchBar.vue';
import { Getter } from 'vuex-class';
import {ns} from '@/core/state/appState';
import {SugaredInstanceJson} from '@/core/@types/javaApi';
import PackCard2 from '@/components/core/modpack/PackCard2.vue';
import {containsIgnoreCase} from '@/utils/helpers/stringHelpers';
import Loader from '@/components/atoms/Loader.vue';
import Selection2 from '@/components/atoms/input/Selection2.vue';

@Component({
  components: {
    Selection2,
    Loader,
    PackCard2,
    FTBSearchBar,
  },
})
export default class Library extends Vue {
  @Getter('instances', ns("v2/instances")) instances!: SugaredInstanceJson[];
  @Getter('isLoadingInstances', ns("v2/instances")) loading!: boolean;

  searchTerm: string = '';
  
  get filteredInstance(): string[] | null {
    if (this.searchTerm === '') {
      return null;
    }
    
    return this.instances
      .filter(e => containsIgnoreCase(e.name, this.searchTerm))
      .map(e => e.uuid);
  }

  get groupedPacks(): Record<string, SugaredInstanceJson[]> {
    const grouped: Record<string, SugaredInstanceJson[]> = {};
    for (const instance of this.instances) {
      if (!grouped[instance.category]) {
        grouped[instance.category] = [];
      }
      grouped[instance.category].push(instance);
    }
    return grouped;
  }
}
</script>

<style lang="scss" scoped>
.drop-area {
  margin-top: 1rem;
  padding: 2.5rem 2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 2px dashed rgba(white, 0.2);
  border-radius: 5px;

  hr {
    margin: 1rem 0;
  }

  > svg {
    margin-bottom: 1rem;
  }
}

.file {
  background-color: rgba(white, 0.1);
  border-radius: 5px;

  .text {
    .name {
      word-wrap: break-word;
    }
  }
}

.no-packs {
  position: relative;
  height: 100%;
  
  border-left: 1px solid rgba(white, 0.1);
  border-right: 1px solid rgba(white, 0.1);

  &::before {
    content: '';
    top: 0;
    left: 0;
    position: absolute;
    width: 100%;
    height: 100%;

    background: url('../assets/images/no-pack-bg.webp') center center no-repeat;
    background-size: auto 100%;
    z-index: -1;
    opacity: 0.3;
  }
}
</style>
