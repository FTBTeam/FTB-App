<script lang="ts" setup>
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { IconDefinition } from '@fortawesome/free-brands-svg-icons';

const {
  type = 'button',
  icon
} = defineProps<{
  type?: 'button' | 'submit',
  icon?: IconDefinition
}>()
</script>

<template>
  <button :type="type">
    <FontAwesomeIcon v-if="icon" :icon="icon" />
    <slot></slot>
  </button>
</template>