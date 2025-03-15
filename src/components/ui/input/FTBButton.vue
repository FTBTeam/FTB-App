<script lang="ts" setup>

const {
  disabled = false,
  color,
  cssClass,
  noPadding,
  isRounded = true,
} = defineProps<{
  disabled: boolean;
  color: string;
  cssClass: string;
  noPadding: boolean;
  isRounded: boolean;
}>();

const emit = defineEmits<{
  (e: 'click'): void;
}>();

function handleClick() {
  if (disabled) {
    return;
  }
  emit('click');
}
</script>

<template>
  <div
    :class="
      `ftb-button ${!noPadding ? '' : 'p-2'} ${color ? 'bg-' + color : ''} ${color ? 'hover:bg-light-' + color : ''} ${
        disabled ? 'cursor-not-allowed disabled' : 'cursor-pointer'
      } ${isRounded ? 'rounded' : ''}`
    "
    @click="handleClick"
  >
    <p :class="`${cssClass} ${disabled ? 'cursor-not-allowed' : 'cursor-pointer'}`">
      <slot></slot>
    </p>
  </div>
</template>

<style lang="scss" scoped>
.ftb-button svg {
  cursor: pointer;
}
.disabled {
  filter: grayscale(0.8) brightness(0.5);
}
</style>
