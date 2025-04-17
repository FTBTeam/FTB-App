<script setup lang="ts">
const {
  min,
  max,
  placeholder = '',
  label,
  disabled = false
} = defineProps<{
  label?: string;
  min?: number;
  max?: number;
  placeholder?: string;
  disabled?: boolean;
}>();

const value = defineModel<number>({
  default: 0
});

const emit = defineEmits<{
  (e: 'blur'): void;
}>();

function increment(e: MouseEvent, decrement: boolean) {
  e.preventDefault();
  e.stopPropagation();
  
  const jump = e.shiftKey ? 10 : 1;
  const minValue = min ?? Number.MIN_SAFE_INTEGER;
  const maxValue = max ?? Number.MAX_SAFE_INTEGER;
  
  if (decrement) {
    value.value = Math.max(minValue, value.value - jump);
  } else {
    value.value = Math.min(maxValue, value.value + jump);
  }
}
</script>

<template>
  <div v-if="label">{{ label }}</div>
  <div class="flex items-center bg-black border border-white/40 rounded w-auto">
    <input type="number" @blur="emit('blur')" v-model="value" :min :max :disabled :placeholder class="appearance-none py-1 px-3 outline-none w-full">
    
    <div class="flex items-center">
      <button @click="(e) => increment(e, false)">+</button>
      <button @click="(e) => increment(e, true)">-</button>
    </div>
  </div>
</template>