<template>
  <div ref="mountPoint" :style="{
    width: width,
    height: height,
    background: 'transparent'
  }" class="w-full h-full flex items-center justify-center"></div>
</template>

<script lang="ts" setup>
// This is needed as it's a very weird component that vue3 doesn't like
import {nextTick, onMounted, useTemplateRef} from 'vue';
import appPlatform from '@platform';

const props = defineProps<{
  enableHighImpact?: boolean;
  width?: string;
  height?: string;
}>();

const emit = defineEmits<{
  (e: 'highImpactAdStateChanged', state: 'loaded' | 'removed'): void;
}>();

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
    // Find the `data-is-main-ad` element
    const fancyAd = document.querySelector("owadview[data-is-main-ad='true']");
    if (fancyAd) {
      fancyAd.addEventListener("high-impact-ad-loaded", () => {
        emit('highImpactAdStateChanged', 'loaded');
      });
      
      fancyAd.addEventListener("high-impact-ad-removed", () => {
        emit('highImpactAdStateChanged', 'removed');
      });
    }
  })
})
</script>