<template>
  <div class="home page-spacing">
    <template v-if="recentInstances.length">
      <h2 class="text-lg font-bold text-white mb-4">Jump back in where you left off</h2>
      
      <div class="recently-played pack-card-grid mb-5">
        <PackCard2 class="pack-card" v-for="instance in recentInstances" :key="instance.uuid" :instance="instance" />
      </div>
    </template>
    
    <div class="featured-packs">
      <h2 class="text-lg font-bold text-white mb-4">Featured packs</h2>
      <PackPreview v-if="featuredPacksIds.length" v-for="packId in featuredPacksIds" :key="packId" :packId="packId" provider="modpacksch" />
      <Message type="warning" v-if="!featuredPacksIds.length && !loadingFeatured">
        <p>No featured packs available at the moment</p>
      </Message>
    </div>
  </div>
</template>

<script lang="ts" setup>
// import {ns} from '@/core/state/appState';
import PackPreview from '@/components/groups/modpack/PackPreview.vue';
import PackCard2 from '@/components/groups/modpack/PackCard2.vue';
import Message from '@/components/ui/Message.vue';
import { computed, onMounted, ref } from 'vue';
import { SugaredInstanceJson } from '@/core/types/javaApi';

// @Getter('instances', ns("v2/instances")) instances!: SugaredInstanceJson[];
//
// @Getter("featuredPacks", ns("v2/modpacks")) featuredPacksIds!: number[];
// @Action("getFeaturedPacks", ns("v2/modpacks")) getFeaturedPacks!: () => Promise<number[]>;

// TODO: State fix me
const instances = ref<SugaredInstanceJson[]>([]);
const featuredPacksIds = ref([]);
const getFeaturedPacks = async () => [];

const loadingFeatured = ref(false);

const loadFeaturedPacks = async () => {
  loadingFeatured.value = true;
  await getFeaturedPacks();
  loadingFeatured.value = false;
};

const recentInstances = computed(() => {
  return instances.value
    .sort((a, b) => {
      // If either lastPlayed is 0, put it at the end
      if (!a.lastPlayed) return 1;
      if (!b.lastPlayed) return -1;

      return b.lastPlayed - a.lastPlayed;
    })
    .slice(0, 6);
});

onMounted(() => {
  loadFeaturedPacks();
});
</script>

<style lang="scss" scoped>
.recently-played {
  .pack-card {
    @media (max-width: 1288px) {
      &:nth-child(5) {
        display: none;
      }
    }
    
    @media (max-width: 1458px) {
      &:nth-child(6) {
        display: none;
      }
    }
  }
}
</style>
