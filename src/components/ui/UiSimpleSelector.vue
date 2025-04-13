<script lang="ts" setup>
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faChevronDown } from '@fortawesome/free-solid-svg-icons';

const {
  options,
  label,
  placeholder = "Select option",
} = defineProps<{
  label?: string;
  placeholder?: string;
  options: ({
    key: string;
    label: string;
  })[]
}>()

const value = defineModel<string>();
const elmId = "simple-select-" + Math.random().toString(36).substring(2, 9);

</script>

<template>
  <div>
    <label v-if="label" :for="elmId" class="text-xs text-white/80 mb-2 block uppercase font-bold">
      {{ label }}
    </label>
    
    <div :id="elmId" class="bg-black border-white/40 rounded border px-3 py-1 relative">
      <select v-model="value" class="appearance-none w-full outline-0">
        <option value="" disabled selected>
          {{ placeholder }}
        </option>
        <option v-for="(item, index) in options" :value="item.key" :key="index">
          {{ item.label }}
        </option>
      </select>
      
      <FontAwesomeIcon 
        :icon="faChevronDown"
        class="absolute right-2 top-1/2 -translate-y-1/2 text-white/80"
        size="xs"
      />
    </div>
  </div>
</template>