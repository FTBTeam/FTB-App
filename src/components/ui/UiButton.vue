<script lang="ts" setup>
import { faCircleNotch, IconDefinition } from '@fortawesome/free-solid-svg-icons';
import { computed } from 'vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';

const {
  type = 'normal',
  size = 'normal',
  disabled = false,
  working = false,
  icon,
  innerClass = '',
  fullWidth = false,
  wider = false,
  ariaLabel = '',
  ariaLabelPos = 'down'
} = defineProps<{
  type?: ElementColorType;
  size?: ElementStandardSizing;
  innerClass?: string;
  disabled?: boolean;
  working?: boolean;
  icon?: IconDefinition;
  fullWidth?: boolean;
  wider?: boolean;
  ariaLabel?: string;
  ariaLabelPos?: ElementAriaDirection;
}>()

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
}>()

function click(event: MouseEvent) {
  if (disabled || working) {
    return;
  }

  emit('click', event);
}

const colorFromType = computed(() => colorFromElementColorType(type, disabled, working));
</script>

<script lang="ts">
export type ElementColorType = 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'info' | 'normal';
export type ElementAriaDirection = "up" | "down" | "left" | "right" | "up-left" | "up-right" | "down-left" | "down-right";
export type ElementStandardSizing = 'small' | 'normal' | 'large';

export function colorFromElementColorType(type: ElementColorType, disabled: boolean = false, working = false) {
  switch (type) {
    case 'primary': return 'bg-indigo-600' + (!disabled && !working ? ' hover:bg-indigo-500' : '');
    case 'secondary': return 'bg-green-600' + (!disabled && !working ? ' hover:bg-green-500' : '');
    case 'success': return 'bg-primary' + (!disabled && !working ? ' hover:bg-light-primary' : '');
    case 'danger': return 'bg-danger' + (!disabled && !working ? ' hover:bg-light-danger' : '');
    case 'warning': return 'bg-warning' + (!disabled && !working ? ' hover:bg-light-warning' : '');
    case 'info': return 'bg-info' + (!disabled && !working ? ' hover:bg-light-info' : '');
    case 'normal': return 'bg-gray-600' + (!disabled && !working ? ' hover:bg-gray-500' : '');
  }
}
</script>

<template>
  <div class="ui-button-holder" :aria-label="ariaLabel ? ariaLabel : undefined" :data-balloon-pos="ariaLabel && ariaLabelPos ? ariaLabelPos : undefined" :class="{'disabled': working || disabled}">
    <div 
      :class="[`ui-button ${colorFromType}`, {fullWidth, wider, 'disabled': working || disabled}, [size], [innerClass]]" 
      @click="click"
    >
      <span :class="{'opacity-0': working}">
        <FontAwesomeIcon :fixedWidth="true" v-if="icon" :icon="icon" :class="{'mr-2': $slots['default']}" />
        <slot />
      </span>
      <transition name="fade">
        <span v-if="working" class="absolute inset-0 flex items-center justify-center">
          <FontAwesomeIcon :fixedWidth="true" :icon="faCircleNotch" spin />
        </span>
      </transition>
    </div>
  </div>
</template>

<style scoped lang="scss">
.ui-button-holder {
  &.disabled {
    cursor: not-allowed;
  }
}

.ui-button {
  border-radius: 3px;
  position: relative;
  padding: 0.75em .8em;
  display: inline-block;
  font-size: 14px;
  text-align: center;
  font-weight: bold;
  transition: background-color .25s ease-in-out, opacity .25s ease-in-out;
  cursor: pointer;

  &.wider {
    padding: 0.75em 2em;
  }
  
  span {
    cursor: pointer;
    transition: opacity .25s ease-in-out;
  }
  
  &.fullWidth {
    display: block;
    width: 100%;
  }
  
  &.small {
    padding: 0.6em .6em;
    font-size: 0.875em;
    &.wider {
      padding: 0.4em 1.8em;
    }
  }
  
  &.large {
    font-size: 1.1275em;
    padding: 0.8em 1.1em; 
    
    &.wider {
      padding: 0.8em 2.4em;
    }
  }
  
  &.disabled {
    opacity: .6;
    cursor: not-allowed;
  }
}
</style>