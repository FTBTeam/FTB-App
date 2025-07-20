<script setup lang="ts">
import AbstractInput from '@/components/ui/form/AbstractInput.vue';
import { IconDefinition } from '@fortawesome/free-brands-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faMinus, faPlus } from '@fortawesome/free-solid-svg-icons';

const {
  icon,
  fill = false,
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
  icon?: IconDefinition;
  fill?: boolean;
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
  <AbstractInput v-slot="{ blur, focus, class: clazz }" :label="label" :disabled="disabled" :fill="fill" :icon="icon">
    <button class="border border-white/30 transition-colors duration-300 border-r-transparent rounded-l-lg p-2 hover:bg-white/10" :class="{ 'opacity-50': disabled, 'hover:border-white/60 cursor-pointer': !disabled }" @click="(e) => !disabled && increment(e, true)">
      <FontAwesomeIcon :icon="faMinus" />
    </button>
    <input type="number" @blur="() => {
      emit('blur');
      blur();
    }" @focus="focus" v-model="value" :min :max :disabled :placeholder :class="clazz + ' appearance-none !rounded-none'" />
    <button class="border border-white/30 transition-colors duration-300 border-l-transparent rounded-r-lg p-2 hover:bg-white/10" :class="{ 'opacity-50': disabled, 'hover:border-white/60 cursor-pointer': !disabled }" @click="(e) => !disabled && increment(e, false)">
      <FontAwesomeIcon :icon="faPlus" />
    </button>
  </AbstractInput>
</template>

<style scoped>
/** Remove the input arrows */
input[type=number]::-webkit-inner-spin-button,
input[type=number]::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
</style>