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

const emit = defineEmits<{
  (e: 'blur', event: FocusEvent): void;
  (e: 'focus', event: FocusEvent): void;
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
    v-slot="{ blur, focus, class: clazz }"
  >
    <input :placeholder="placeholder" @blur="(e) => {
      blur()
      emit('blur', e)
    }" @focus="(e) => {
      focus()
      emit('focus', e)
    }" :disabled="disabled" :class="clazz" v-model="value"  />
  </AbstractInput>
</template>