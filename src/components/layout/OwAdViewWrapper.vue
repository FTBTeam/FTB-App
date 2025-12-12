<template>
  <div ref="mountPoint" :style="{
    minWidth: width,
    minHeight: height,
    background: 'transparent'
  }" class="w-full h-full flex items-center justify-center"></div>
</template>

<script lang="ts" setup>
// This is needed as it's a very weird component that vue3 doesn't like
import {nextTick, onMounted, useTemplateRef} from 'vue';
import appPlatform from '@platform';
import {createLogger} from "@/core/logger.ts";

const props = defineProps<{
  enableHighImpact?: boolean;
  width?: string;
  height?: string;
}>();

const emit = defineEmits<{
  (e: 'highImpactAdStateChanged', state: 'loaded' | 'removed'): void;
}>();

const logger = createLogger("OwAdViewWrapper.vue");
const mountPoint = useTemplateRef("mountPoint")

onMounted(() => {
  if (!appPlatform.isElectron) {
    return;
  }
  
  if (!mountPoint.value) {
    return;
  }
  
  if (props.enableHighImpact) {
    mountPoint.value.innerHTML = "<owadview data-is-main-ad='true' adstyle='high-impact-ad;'></owadview>"
  } else {
    mountPoint.value.innerHTML = "<owadview></owadview>"
  }
  
  nextTick(() => {    
    const fancyAd = mountPoint.value?.querySelector('owadview[data-is-main-ad="true"]');
    if (fancyAd) {
      logger.info("High impact ad view loaded");
      fancyAd.addEventListener("high-impact-ad-loaded", () => {
        logger.log("[AD] high-impact-ad-loaded");
        emit('highImpactAdStateChanged', 'loaded');
      });

      fancyAd.addEventListener("high-impact-ad-removed", () => {
        logger.log("[AD] high-impact-ad-removed");
        emit('highImpactAdStateChanged', 'removed');
      });
    }
  })
})
</script>