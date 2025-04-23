<template>
  <div ref="mountPoint" class="w-full h-full"></div>
</template>

<script lang="ts" setup>
// This is needed as it's a very weird component that vue3 doesn't like
import { onMounted, useTemplateRef } from 'vue';
import appPlatform from '@platform';

const mountPoint = useTemplateRef("mountPoint")

onMounted(() => {
  if (!appPlatform.isElectron) {
    return;
  }
  
  if (!mountPoint.value) {
    return;
  }
  
  mountPoint.value.innerHTML = "<owadview></owadview>"
})
</script>