<script lang="ts" setup>
import {
  colorFromElementColorType,
  ElementAriaDirection,
  ElementColorType,
} from '@/components/ui/UiButton.vue';

import { computed } from 'vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { IconDefinition } from '@fortawesome/free-brands-svg-icons';

const {
  type = 'secondary',
  icon,
  ariaLabel = '',
  ariaLabelPos = 'down',
  hoverEffect = false
} = defineProps<{
  type?: ElementColorType;
  icon?: IconDefinition;
  ariaLabel?: string;
  ariaLabelPos?: ElementAriaDirection;
  hoverEffect?: boolean
}>()

const colorFromType = computed(() => colorFromElementColorType(type, !hoverEffect, false));
</script>

<template>
  <div 
    class="ui-badge" 
    :class="colorFromType" 
    :aria-label="ariaLabel ? ariaLabel : undefined"
    :data-balloon-pos="ariaLabel && ariaLabelPos ? ariaLabelPos : undefined">
    
    <FontAwesomeIcon :fixed-width="true" v-if="icon" :icon="icon" />
    <slot />
  </div>
</template>

<style lang="scss" scoped>
.ui-badge {
  display: inline-flex;
  align-items: center;
  gap: .5rem;

  font-weight: normal; 
  font-size: 0.75rem;
  padding: .15rem .5rem;
  
  border-radius: 3px;  
}
</style>