<script lang="ts" setup>
import {computed} from "vue";

const {
  title,
  value
} = defineProps<{
  title: string;
  value?: number | string;
}>();

const isNumber = computed(() => typeof value === 'number');
</script>

<template>
  <div>
    <div class="text-white/80">{{ title }}</div>
    <div class="font-bold text-lg">
      <template v-if="!$slots.default && value !== undefined">
        {{ isNumber ? value.toLocaleString() : value }}
      </template>
      <slot v-else />
    </div>
  </div>
</template>