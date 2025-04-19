<script lang="ts" setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
// @ts-ignore
import { RecycleScroller } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'

const timeout = ref<number | null>(null)

const messages = ref<({id: number, text: string})[]>(Array.from({ length: 1000 }, (_, i) => ({
  id: i,
  text: `Item ${i + 1}`
})));

onMounted(() => {
  timeout.value = setInterval(() => {
    // Push another item to the array every 500ms
    messages.value.push({
      id: messages.value.length,
      text: `Item ${messages.value.length + 1}`
    });
  }, 500) as unknown as number;
})

onUnmounted(() => {
  if (timeout.value) {
    clearTimeout(timeout.value);
  }
  timeout.value = null;
})

const processedMessages = computed(() => {
  const tmp = [...messages.value]
  return tmp;
})
</script>

<template>
  Scroller

  <RecycleScroller
    :items="processedMessages"
    :item-size="20"
    key-field="id"
    direction="vertical"
    class="h-[300px]"
    v-slot="{ item }"
  >
    <div>
      {{ item.text }} - {{ processedMessages[processedMessages.length - 1] }}
    </div>
  </RecycleScroller>
  
  <p>Last item in list {{ processedMessages[processedMessages.length - 1] }}</p>
</template>