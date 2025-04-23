<script lang="ts" setup>
import { useRouter } from 'vue-router';
import { debounce, safeNavigate } from '@/utils';
import { SearchResultPack } from '@/core/types/modpacks/packSearch';
import { toggleBeforeAndAfter } from '@/utils/helpers/asyncHelpers';
import Loader from '@/components/ui/Loader.vue';
import PackPreview from '@/components/groups/modpack/PackPreview.vue';
import { modpackApi } from '@/core/pack-api/modpackApi';
import { Pagination, Message, Input } from '@/components/ui';
import { createLogger } from '@/core/logger';
import { RouterNames } from '@/router';
import { onMounted, ref, watch } from 'vue';
import { packBlacklist } from '@/store/modpackStore.ts';
import { PackProviders } from '@/core/types/appTypes.ts';
import { faInfo, faSearch, faWarning } from '@fortawesome/free-solid-svg-icons';

const logger = createLogger("BrowseModpacks.vue")
const router = useRouter();

const searchValue = ref('');
const currentTab = ref<PackProviders>('modpacksch');
const searchResults = ref<SearchResultPack[]>([]);
const loading = ref(false);
const error = ref('');
const loadingInitialPacks = ref(false);
const ourPackIds = ref<number[]>([]);
const currentPage = ref(1);
const visiblePacks = ref<number[]>([]);

onMounted(() => {
  if (router.currentRoute.value.query.provider) {
    currentTab.value = router.currentRoute.value.query.provider as PackProviders;
  }
  
  if (router.currentRoute.value.query.search) {
    searchValue.value = router.currentRoute.value.query.search as string;
    searchPacks();
    return;
  }
  
  try {
    toggleBeforeAndAfter(async () => {
      const data = await Promise.all([
        await modpackApi.modpacks.getModpacks(),
        await modpackApi.modpacks.getPrivatePacks()
      ])

      const allPackIds = new Set(data.flatMap(e => e?.packs ?? []));
      ourPackIds.value = [...allPackIds].sort((a, b) => b - a);

      // remove the modloader packs
      ourPackIds.value = ourPackIds.value.filter(e => !packBlacklist.includes(e));

      visiblePacks.value = ourPackIds.value.slice(0, 10);
    }, (state) => loadingInitialPacks.value = state);
  } catch (error) {
    logger.error("Failed to load packs", error);
  }
})

function scrollToTop() {
  // Smooth scroll to top
  document.querySelector('.app-content')?.scrollTo({
    top: 0,
    behavior: 'smooth'
  });
}

watch(currentPage, () => visiblePacks.value = ourPackIds.value.slice((currentPage.value - 1) * 10, ((currentPage.value - 1) * 10 + 10)))
watch(() => router.currentRoute.value.query.search, (newValue, oldValue) => {
  if (newValue !== oldValue) {
    searchValue.value = newValue as string;
    searchPacks();
  }
})

const debouncedSearch = debounce(() => {
  searchPacks();
}, 500);

async function changeTab(tab: PackProviders) {
  // Update the route
  await safeNavigate(RouterNames.ROOT_BROWSE_PACKS, { provider: tab })

  currentTab.value = tab;
  searchResults.value = [];
  await searchPacks();
}

watch(searchValue, () => {
  loading.value = true;
  searchResults.value = [];

  if (searchValue.value === '') {
    loading.value = false;
    error.value = '';
    return;
  }

  error.value = '';
  debouncedSearch.run();
});

async function searchPacks() {
  if (searchValue.value.length < 2) {
    return;
  }

  try {
    const results = await toggleBeforeAndAfter(() => modpackApi.search.search(searchValue.value, currentTab.value), v => loading.value = v);
    searchResults.value = results?.packs ?? [];
  } catch (resError: any) {
    logger.error("Failed to search packs", resError);
    error.value = 'Failed to search packs';
  }
}
</script>

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
      <Input
        v-model="searchValue"
        :icon="faSearch"
        fill
        :placeholder="`Search ${currentTab === 'modpacksch' ? 'FTB Modpacks' : 'Curseforge Modpacks'}`"
      />
    </div>
    
    <Message :icon="faWarning" type="danger" class="my-6" v-if="error">
      {{ error }}
    </Message>

    <Message :icon="faInfo" type="info" class="my-6" v-if="!error && searchResults.length === 0 && searchValue.length > 0 && !loading">
      No results found for '{{ searchValue }}'
    </Message>

    <loader class="mt-20"  v-if="(!error && searchValue !== '' && loading && !loadingInitialPacks)" />
    
    <Message v-if="searchValue === '' && !loading && !loadingInitialPacks && !ourPackIds.length">
      <p>No packs available</p>
    </Message>

    <div class="result-cards pb-2" v-if="!error && searchResults.length > 0">
      <pack-preview v-for="(pack, index) in searchResults" :partial-pack="pack" :key="index" :provider="currentTab" />
    </div>
    
    <div class="latest-packs" v-if="searchValue === '' && !loading">
      <div class="result-cards pb-2">
        <pack-preview v-for="(packId, index) in visiblePacks" :pack-id="packId" :key="`${currentPage}:${index}`" />
      </div>

      <div class="flex justify-center pb-8">
        <Pagination v-if="ourPackIds.length" v-model="currentPage" :total="ourPackIds.length" :per-page="10" @input="scrollToTop" />
      </div>
      <loader class="mt-20" v-if="loadingInitialPacks" />
    </div>
  </div>
</template>

<style scoped lang="scss">
@import 'tailwindcss/theme' theme(reference);

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
    border: 1px solid rgba(white, .3);

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
        background-color: var(--color-green-600);
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
