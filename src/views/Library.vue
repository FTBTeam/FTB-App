<script lang="ts" setup>
import {SugaredInstanceJson} from '@/core/types/javaApi';
import PackCard2 from '@/components/groups/modpack/PackCard2.vue';
import {containsIgnoreCase} from '@/utils/helpers/stringHelpers';
import Loader from '@/components/ui/Loader.vue';
import Selection2, {SelectionOptions} from '@/components/ui/Selection2.vue';
import {resolveModloader} from '@/utils/helpers/packHelpers';
import UiButton from '@/components/ui/UiButton.vue';
import PackPreview from '@/components/groups/modpack/PackPreview.vue';
import {RouterNames} from '@/router';
import { useInstanceStore } from '@/store/instancesStore.ts';
import { computed, onMounted, ref, watch } from 'vue';
import { useModpackStore } from '@/store/modpackStore.ts';
import { toggleBeforeAndAfter } from '@/utils/helpers/asyncHelpers.ts';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import {
  faArrowDownAZ,
  faArrowDownZA,
  faChevronDown,
  faFolder, faPlus,
  faSearch,
  faSort,
} from '@fortawesome/free-solid-svg-icons';
import { Input } from '@/components/ui';
import {useGlobalStore} from "@/store/globalStore.ts";
import InstanceSelectActions from "@/components/groups/instanceSelect/InstanceSelectActions.vue";
import {faCheckSquare} from "@fortawesome/free-regular-svg-icons";

const groupOptions = [
  ['Category', 'category'],
  ['Mod Loader', 'modloader'],
  ['Minecraft Version', 'mcversion'],
]

const sortOptions = [
  ['Name', 'name'],
  ['Last Played', 'lastPlayed'],
  ['Total Playtime', 'totalPlaytime'],
]

function createOrderedOptions(options: string[][]): SelectionOptions {
  return options.flatMap(([label, value]) => [
    [`${label}`, value, 'Asc'],
    [`${label}`, `-${value}`, 'Des']
  ])
    .map(([label, value, dir]) => ({label, value, badge: {color: '#008BF8', text: dir, icon: dir === "Asc" ? faArrowDownAZ : faArrowDownZA}}))
}

const sortByOptions = createOrderedOptions(sortOptions)
const groupByOptions = createOrderedOptions(groupOptions)

const instanceStore = useInstanceStore();
const modpackStore = useModpackStore();
const globalStore = useGlobalStore();

const loadingFeatured = ref(false);
const searchTerm = ref('');
const groupBy = ref('category');
const sortBy = ref('name');
const collapsedGroups = ref<string[]>([]);

const selectedInstances = ref<SugaredInstanceJson[]>([]);

onMounted(() => {
  const libraryData = localStorage.getItem("library-data");
  if (libraryData) {
    const parsed = JSON.parse(libraryData);
    if (parsed?.collapsedGroups) {
      collapsedGroups.value = parsed.collapsedGroups;
    }

    if (parsed?.sortBy) {
      sortBy.value = parsed.sortBy;
    }

    if (parsed?.groupBy) {
      groupBy.value = parsed.groupBy;
    }
  }
})

watch(() => instanceStore.loading, async (newValue) => {
  if (newValue) return;
  if (instanceStore.instances.length !== 0) return;
  
  await toggleBeforeAndAfter(async () => await modpackStore.getFeaturedPacks(), v => loadingFeatured.value = v);
})

function collapseGroup(group: string) {
  if (collapsedGroups.value.includes(group)) {
    collapsedGroups.value = collapsedGroups.value.filter(e => e !== group);
  } else {
    collapsedGroups.value.push(group);
  }

  localStorage.setItem("library-data", JSON.stringify({collapsedGroups: collapsedGroups.value, sortBy: sortBy.value, groupBy: groupBy.value}));
}

const filteredInstance = computed(() => {
  if (searchTerm.value === '') {
    return null;
  }

  return instanceStore.instances
    .filter(e => containsIgnoreCase(e.name, searchTerm.value))
    .map(e => e.uuid);
})

const sortedInstances = computed(() => {
  const sortDirection = sortBy.value.includes("-") ? 'dec' : 'asc';
  const sortByKey = sortBy.value.includes("-") ? sortBy.value.split('-')[1] : sortBy.value;

  const sortKey = instanceJsonKey(sortByKey);

  return instanceStore.instances.sort((a, b) => {
    if (a[sortKey] === b[sortKey]) return 0;
    if (a[sortKey] > b[sortKey]) return sortDirection === 'asc' ? 1 : -1;
    
    return sortDirection === 'asc' ? -1 : 1;
  });
});

const groupedPacks = computed(() => {
  const grouped: Record<string, SugaredInstanceJson[]> = {
    "Pinned": [],
  };

  for (const instance of sortedInstances.value) {
    let groupKey = '';
    switch (groupBy.value.replace("-", "")) {
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

    if (instance.pinned) {
      groupKey = "Pinned";
    }

    grouped[groupKey].push(instance);
  }

  const sortDirection = groupBy.value.includes("-") ? 'dec' : 'asc';

  // Modify the order of the group keys based on the sort direction
  const groupKeys = Object.keys(grouped).sort((a, b) => {
    // Pinned at the top
    if (a === "Pinned") return -1;
    if (b === "Pinned") return 1;
    if (a === b) return 0;
    if (a > b) return sortDirection === 'asc' ? 1 : -1;

    return sortDirection === 'asc' ? -1 : 1;
  });

  const sorted: Record<string, SugaredInstanceJson[]> = {};
  for (const key of groupKeys) {
    sorted[key] = grouped[key];
  }

  return sorted;
})

function instanceJsonKey(sortKey: String): Partial<keyof SugaredInstanceJson> {
  switch (sortKey) {
    case "name": return "name";
    case "lastPlayed": return "lastPlayed";
    case "totalPlaytime": return "totalPlayTime";
    default: return "name";
  }
}

function onSortChange() {
  localStorage.setItem("library-data", JSON.stringify({collapsedGroups: collapsedGroups.value, sortBy: sortBy.value, groupBy: groupBy.value}));
}

function toggleCategorySelect(instances: SugaredInstanceJson[]) {
  const missingInstances = instances.filter(instance => !selectedInstances.value.some(e => e.uuid === instance.uuid));
  if (missingInstances.length > 0) {
    selectedInstances.value.push(...missingInstances);
  } else {
    selectedInstances.value = selectedInstances.value.filter(instance => !instances.some(e => e.uuid === instance.uuid));
  }
}

watch(sortBy, onSortChange)
watch(groupBy, onSortChange)
</script>

<template>
  <div class="mod-packs h-full">
    <div class="page-spacing" v-if="!instanceStore.loading && instanceStore.instances.length > 0">
      <header class="flex gap-4 mb-6 items-center">
        <Input :icon="faSearch" fill v-model="searchTerm" placeholder="Search" class="flex-1" />
        
        <selection2 v-if="Object.keys(groupedPacks).length > 1" :icon="faFolder" direction="right" :min-width="300" :options="groupByOptions" v-model="groupBy" aria-label="Sort categories" data-balloon-pos="down-right" />
        <selection2 :icon="faSort" direction="right" :min-width="300" :options="sortByOptions" v-model="sortBy" aria-label="Sort packs" data-balloon-pos="down-right" />
      </header>
      
      <div class="categories">
        <div class="category" v-for="(category, index) in groupedPacks" :key="`category-${index}`" :class="{'collapsed': collapsedGroups.includes(index)}">
          <template v-if="category.length">
            <header v-if="Object.keys(groupedPacks).length > 1">
              <h2>{{ index }}</h2>
              <span />
              <div class="flex items-center gap-2">
                <div class="py-1 px-2 rounded bg-white/10 hover:bg-white/20 cursor-pointer transition-colors" @click="toggleCategorySelect(category)">
                  <FontAwesomeIcon fixed-width :icon="faCheckSquare" />
                </div>
                <div class="collapse-icon py-1 px-2 rounded bg-white/10 hover:bg-white/20 cursor-pointer transition-colors" @click="collapseGroup(index)">
                  <FontAwesomeIcon fixed-width :icon="faChevronDown" />
                </div>
              </div>
            </header>
            <div class="pack-card-grid" v-if="!collapsedGroups.includes(index)">
              <template v-for="instance in category" :key="instance.uuid">
                <pack-card2
                  v-show="filteredInstance === null || filteredInstance.includes(instance.uuid)"
                  :instance="instance"
                  :checked="selectedInstances.find(e => e.uuid === instance.uuid) !== undefined"
                  :on-checked-change="(selected) => {
                    if (selected) {
                      selectedInstances.push(instance);
                    } else {
                      selectedInstances = selectedInstances.filter(e => e.uuid !== instance.uuid);
                    }
                  }"
                />
              </template>
            </div>
          </template>
        </div>
      </div>
    </div>
    
    <loader v-else-if="instanceStore.loading" />
    
    <div class="no-packs" v-else>
      <div class="message px-8 pt-20">
        <h1 class="text-5xl font-bold mb-1">Library Empty</h1>
        <p class="mb-8">
          It looks like your library is empty, you can search for new modpacks to install or pick from our recommendations below.
        </p>
        <div class="flex gap-6 mb-8">
          <UiButton type="success" :icon="faPlus" @click="globalStore.updateCreateInstanceVisibility(true)">Create your own</UiButton>
          <router-link to="/browseModpacks">
            <UiButton :wider="true" type="info" :icon="faSearch">Browse FTB Modpacks</UiButton>
          </router-link>
          <router-link :to="{ name: RouterNames.ROOT_BROWSE_PACKS, query: { provider: 'curseforge' } }">
            <UiButton type="info" :icon="faSearch">Browse CurseForge Modpacks</UiButton>
          </router-link>
        </div>
        
        <div class="featured-packs pb-6">
          <template v-if="!loadingFeatured && modpackStore.featuredPackIds.length">
            <h2 class="text-xl font-bold mb-3">Recommended Modpacks</h2>
            <pack-preview v-for="packId in modpackStore.featuredPackIds" :key="packId" :packId="packId" provider="modpacksch" />
          </template>
          <loader v-else-if="loadingFeatured" />
        </div>
      </div>
    </div>
  </div>
  
  <InstanceSelectActions :instances="selectedInstances" @deselect-all="selectedInstances = []" @select-all="selectedInstances = instanceStore.instances" />
</template>

<style lang="scss" scoped>
.categories {
  .category {
    &:not(:last-child) {
      margin-bottom: 2rem;
    }
    
    &.collapsed {
      header {
        .collapse-icon {
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
      
      .collapse-icon {
        cursor: pointer;
        
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
    position: fixed;
    width: 100%;
    height: 100%;

    background-size: auto 100%;
    background: url('../assets/images/no-pack-bg.webp') no-repeat fixed;
    z-index: -1;
    opacity: 0.3;
  }
}
</style>
