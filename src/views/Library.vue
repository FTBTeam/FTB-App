<template>
  <div class="mod-packs h-full">
    <div class="page-spacing" v-if="!loading && instances.length > 0">
      <header class="flex gap-4 mb-6 items-end">
        <FTBSearchBar v-model="searchTerm" placeholder="Search" class="flex-1" />
        <selection2 label="Group by" :options="groupByOptions" v-model="groupBy" />
        <selection2 label="Sort by" :options="sortByOptions" v-model="sortBy" />
      </header>

      <div class="categories">
        <div class="category" v-for="(category, index) in groupedPacks" :key="`category-${index}`" :class="{'collapsed': collapsedGroups.includes(index)}">
          <header v-if="Object.keys(groupedPacks).length > 1">
            <h2>{{ index }}</h2>
            <span />
            <div class="collapse" @click="collapseGroup(index)">
              <font-awesome-icon icon="chevron-down" />
            </div>
          </header>
          <div class="pack-card-grid" v-if="!collapsedGroups.includes(index)">
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
import {Component, Vue} from 'vue-property-decorator';
import FTBSearchBar from '@/components/atoms/input/FTBSearchBar.vue';
import {Getter} from 'vuex-class';
import {ns} from '@/core/state/appState';
import {SugaredInstanceJson} from '@/core/@types/javaApi';
import PackCard2 from '@/components/core/modpack/PackCard2.vue';
import {containsIgnoreCase} from '@/utils/helpers/stringHelpers';
import Loader from '@/components/atoms/Loader.vue';
import Selection2, {SelectionOptions} from '@/components/atoms/input/Selection2.vue';
import {resolveModloader} from '@/utils/helpers/packHelpers';

const groupByOptions = [
  {
    label: 'Category',
    value: 'category'
  },
  {
    label: 'Mod Loader',
    value: 'modloader'
  },
  {
    label: 'Minecraft Version',
    value: 'mcversion'
  },
] as SelectionOptions

const sortOptions = [
  ['Name', 'name'],
  ['Last Played', 'lastPlayed'],
  ['Total Playtime', 'totalPlaytime'],
]

const sortByOptions: SelectionOptions = sortOptions
  .flatMap(([label, value]) => [
    [`${label}`, value, 'Asc'],
    [`${label}`, `-${value}`, 'Des']
  ])
  .map(([label, value, dir]) => ({label, value, badge: {color: '#008BF8', text: dir}}))

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
  
  sortByOptions = sortByOptions;
  groupByOptions = groupByOptions;
  
  groupBy = 'category';
  sortBy = 'name';
  
  collapsedGroups: string[] = [];
  
  mounted() {
    const libraryData = localStorage.getItem("library-data");
    if (libraryData) {
      const parsed = JSON.parse(libraryData);
      if (parsed?.collapsedGroups) {
        this.collapsedGroups = parsed.collapsedGroups;
      }
    }
  }

  collapseGroup(group: string) {
    if (this.collapsedGroups.includes(group)) {
      this.collapsedGroups = this.collapsedGroups.filter(e => e !== group);
    } else {
      this.collapsedGroups.push(group);
    }
    
    // TODO: (M#01) Support more data
    localStorage.setItem("library-data", JSON.stringify({collapsedGroups: this.collapsedGroups}));
  }
  
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
    
    for (const instance of this.sortedInstances) {
      let groupKey = '';
      switch (this.groupBy) {
        case 'category':
          groupKey = instance.category;
          break;
        case 'modloader':
          groupKey = resolveModloader(instance)
          break;
        case 'mcversion':
          groupKey = instance.mcVersion;
          break;
        default:
          groupKey = instance.category;
          break;
      } 
      
      if (!grouped[groupKey]) {
        grouped[groupKey] = [];
      }
      grouped[groupKey].push(instance);
    }
    
    return grouped;
  }
  
  get sortedInstances() {
    const sortDirection = this.sortBy.includes("-") ? 'dec' : 'asc';
    const sortByKey = this.sortBy.includes("-") ? this.sortBy.split('-')[1] : this.sortBy;
    
    const sortKey = this.instanceJsonKey(sortByKey);
    
    return this.instances.sort((a, b) => {
      if (a[sortKey] === b[sortKey]) {
        return 0;
      }
      
      if (a[sortKey] > b[sortKey]) {
        return sortDirection === 'asc' ? 1 : -1;
      }
      return sortDirection === 'asc' ? -1 : 1;
    });
  }
  
  instanceJsonKey(sortKey: String): Partial<keyof SugaredInstanceJson> {
    switch (sortKey) {
      case "name": return "name";
      case "lastPlayed": return "lastPlayed";
      case "totalPlaytime": return "totalPlayTime";
      default: return "name";
    }
  }
}
</script>

<style lang="scss" scoped>
.categories {
  .category {
    &:not(:last-child) {
      margin-bottom: 2rem;
    }
    
    &.collapsed {
      header {
        .collapse {
          svg {
            transform: rotateZ(180deg);
          }
        }
      }
    }
    
    header {
      display: flex;
      align-items: center;
      gap: 2rem;
      margin-bottom: 1rem;
      
      h2 {
        font-size: 1.1275rem;
        font-weight: bold;
      }
      
      span {
        display: block;
        flex: 1;
        height: 2px;
        border-radius: 2px;
        background-color: rgba(white, .2);
      }
      
      .collapse {
        cursor: pointer;
        background-color: pink;
        padding: .18rem 1rem;
        border-radius: 3px;
        background-color: rgba(white, .1);
        transition: background-color .25s ease-in-out;
        
        &:hover {
          background-color: rgba(white, .2);
        }
        
        svg {
          transition: transform .25s ease-in-out;
          font-size: 12px;
        }
      }
    }
  }
}

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
