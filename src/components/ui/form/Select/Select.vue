<script lang="ts" setup>
import { IconDefinition } from '@fortawesome/free-brands-svg-icons';
import AbstractInput from '@/components/ui/form/AbstractInput.vue';

const {
  placeholder = '',
  icon,
  label,
  disabled = false,
  fill = false
} = defineProps<{
  icon?: IconDefinition;
  label?: string;
  placeholder?: string;
  disabled?: boolean;
  fill?: boolean;
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
    v-slot="{ class: clazz }"
  >
    <select :placeholder="placeholder" :disabled="disabled" :class="clazz" v-model="value">
      <option value="" disabled selected>{{ placeholder }}</option>
      <slot />
    </select>
  </AbstractInput>
</template>