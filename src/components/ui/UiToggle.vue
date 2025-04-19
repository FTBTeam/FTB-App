<script lang="ts" setup>

const { disabled = false, label = undefined, desc = undefined, alignRight = false } = defineProps<{
  disabled?: boolean;
  label?: string;
  desc?: string;
  alignRight?: boolean;
}>();

const value = defineModel<boolean>()
const hasText = label || desc;

function toggle() {
  if (!disabled) {
    value.value = !value.value;
  }
}
</script>

<template>
  <div class="ui-toggle" @click="toggle" :class="{disabled, 'text-based': hasText, 'right': alignRight, 'no-desc': !desc}">
    {{value ? 'On' : 'Off'}}
    <div class="toggle" :class="{active: value}">
      <div class="inner" />
    </div>
    
    <div class="main" v-if="hasText">
      <div class="label" v-if="label">{{label}}</div>
      <div class="desc mt-1 text-muted" v-if="desc">{{desc}}</div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.ui-toggle {
  --size: 18px;
  --spacing: .25rem;
  cursor: pointer;
  position: relative;
  z-index: 1;
  height: calc(var(--size) + (var(--spacing) * 2));
  
  &.text-based {
    height: unset;
    display: flex;
    gap: 1.4rem;
    align-items: center;
    
    .toggle {
    }
    
    &.right {
      flex-direction: row-reverse;
    }
  }
  
  &.disabled {
    cursor: not-allowed;
    
    .main {
      opacity: .4;
      
      .desc {
        color: white;
      }
    }
  }
  
  &.no-desc {
    .main .label {
      font-weight: normal !important;
    }
    
    &.text-based {
      align-items: center;
    }
  }
  
  .toggle {
    --transition-timing: cubic-bezier(.9,0,.07,.99);
    position: relative;
    display: inline-block;
    background: rgba(white, .1);
    padding: var(--spacing);
    border-radius: var(--size);
    width: calc((var(--size) + var(--spacing)) * 2);
    transition: background .2s var(--transition-timing);
    
    &::before {
      content: '';
      background: var(--color-success-button);
      opacity: 0;
      border-radius: var(--size);
      position: absolute;
      inset: 0;
      z-index: -1;
      transition: transform .2s var(--transition-timing), opacity .2s var(--transition-timing);
    }
    
    &.active {
      background: var(--color-success-button);
      
      &::before {
        transform: scaleY(1.3) scaleX(1.2);
        opacity: .3;
      }
      
      .inner {
        margin-left: var(--size);
        
        &::after {
          background: rgba(black, .6);
        }
      }
    }
    
    .inner {
      position: relative;
      display: block;
      background: white;
      width: var(--size);
      height: var(--size);
      border-radius: var(--size);
      transition: margin-left .2s var(--transition-timing);
      
      &::after {
        --size: 4px;
        width: var(--size);
        height: var(--size);
        border-radius: var(--size);
        background: rgba(black, .3);
        content: '';
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        transition: background .2s var(--transition-timing);
      }
    }
  }
  
  .main {
    flex: 1;
    
    .label {
      font-weight: bold;
    }
    
    
    .desc {
      font-size: .875em;
    }
  }
}
</style>