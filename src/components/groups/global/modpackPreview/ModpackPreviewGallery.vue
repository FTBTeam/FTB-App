<script lang="ts" setup>
import {Art, ModPack} from "@/core/types/appTypes.ts";
import {computed} from "vue";
import {useGlobalStore} from "@/store/globalStore.ts";

const {
  modpack
} = defineProps<{
  modpack: ModPack
}>()

const globalStore = useGlobalStore();

const screenShots = computed(() => modpack.art.filter(e => e.type === 'screenshot' || e.type === 'media'))

function previewImage(screenshot: Art) {
  globalStore.openImagePreview({
    url: screenshot.url,
    name: screenshot.title,
    description: screenshot.description,
  })
}
</script>

<template>  
  <div class="grid grid-cols-2 gap-x-6 gap-y-8" v-if="screenShots.length">
    <div v-for="(screenshot, i) in screenShots" :key="i">
      <img :draggable="false" @click="previewImage(screenshot)" class="cursor-pointer aspect-video mb-2 rounded-lg border border-white/10" :src="screenshot.url" :alt="screenshot.type" />
      <p class="select-text text-lg mb-1 font-bold">{{screenshot.title}}</p>
      <p class="select-text text-sm text-white/80" v-if="screenshot.description">{{ screenshot.description }}</p>
    </div>
  </div>
  <div v-else>
    <p class="text-white/80 text-center">No screenshots available</p>
  </div>
</template>