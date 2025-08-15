<template>
  <div class="home page-spacing">
    <template v-if="recentInstances.length">
      <h2 class="text-2xl font-bold text-white mb-4">Jump back in where you left off</h2>
      
      <div class="recently-played pack-card-grid mb-5">
        <PackCard2 class="pack-card" v-for="instance in recentInstances" :key="instance.uuid" :instance="instance" />
      </div>
    </template>
    
    <div class="featured-packs">
      <h2 class="text-2xl font-bold text-white mb-4">Featured packs</h2>
      <PackPreview v-if="modpackStore.featuredPackIds.length" v-for="packId in modpackStore.featuredPackIds" :key="packId" :packId="packId" provider="modpacksch" />
      <Message type="warning" v-if="!modpackStore.featuredPackIds.length && !loadingFeatured">
        <p>No featured packs available at the moment</p>
      </Message>
    </div>
  </div>
</template>

<script lang="ts" setup>
import PackPreview from '@/components/groups/modpack/PackPreview.vue';
import PackCard2 from '@/components/groups/modpack/PackCard2.vue';
import { Message } from '@/components/ui';
import { computed, onMounted, ref } from 'vue';
import { useInstanceStore } from '@/store/instancesStore.ts';
import { useModpackStore } from '@/store/modpackStore.ts';
import { toggleBeforeAndAfter } from '@/utils/helpers/asyncHelpers.ts';

const instanceStore = useInstanceStore();
const modpackStore = useModpackStore();

const loadingFeatured = ref(false);

const loadFeaturedPacks = async () => {
  await toggleBeforeAndAfter(async () => {
    await modpackStore.getFeaturedPacks();
  }, v => loadingFeatured.value = v);
};

const recentInstances = computed(() => {
  return instanceStore.instances
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
