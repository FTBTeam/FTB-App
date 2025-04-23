<script lang="ts" setup>
import { IconDefinition } from '@fortawesome/free-brands-svg-icons';
import AbstractInput from '@/components/ui/form/AbstractInput.vue';

const {
  placeholder = '',
  icon,
  label,
  disabled = false,
  fill = false,
  spellcheck = false,
  rows,
  cols,
} = defineProps<{
  icon?: IconDefinition;
  label?: string;
  placeholder?: string;
  disabled?: boolean;
  fill?: boolean;
  spellcheck?: boolean;
  rows?: number;
  cols?: number;
}>();

const emit = defineEmits<{
  (e: 'blur'): void;
}>();

const value = defineModel<string>({
  default: ''
});
</script>

<template>
  <AbstractInput
    :fill="fill"
    :placeholder="placeholder"
    :icon="icon"
    :label="label"
    :disabled="disabled"
    v-slot="{ class: clazz, focus, blur }"
  >
    <textarea @blur="() => {
      emit('blur');
      blur();
    }" @focus="focus"  :placeholder="placeholder" :disabled="disabled" :class="clazz" v-model="value" :rows="rows" :cols="cols" :spellcheck="true" />
  </AbstractInput>
</template>