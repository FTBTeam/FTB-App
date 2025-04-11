<script setup lang="ts">
import { RouterView } from 'vue-router';
import { onBeforeMount, ref } from 'vue';
import { services } from '@/bootstrap.ts';

const loading = ref(true);

onBeforeMount(async () => {
  await waitForSocket();
  
  loading.value = false;
})

async function waitForSocket() {
  // Wait for websocket
  await new Promise((res) => {
    function check() {
      if (services().websocket.isAlive()) {
        res(null);
      } else {
        setTimeout(check, 500); // Every 500ms
      }
    }

    check();
  });
}
</script>

<template>
  <div v-if="loading">Loading</div>
  <RouterView v-else />
</template>