<template>
  <div class="popover-wrapper">
    <slot></slot>
    <div v-if="$slots.inner || text" class="popover" :class="position" :style="{ left: `${leftShift}%` }">
      <template v-if="$slots.inner">
        <slot name="inner"></slot>
      </template>
      <template v-else>{{ text }}</template>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Prop } from 'vue-property-decorator';

@Component
export default class Popover extends Vue {
  @Prop({ default: '' }) text!: string;
  @Prop({ default: 110 }) leftShift!: string;

  @Prop({ default: 'middle' }) position!: string;
}
</script>

<style scoped lang="scss">
.popover-wrapper {
  position: relative;
  &:hover {
    .popover {
      left: 120% !important;
      opacity: 1 !important;
      visibility: visible;
    }
  }

  // This is needed because the normal way of doing this keeps it open when you navigate, super annoying
  > .popover {
    position: absolute;
    left: 110%;
    z-index: 30;
    font-size: var(--balloon-font-size);
    background: black;
    white-space: nowrap;
    padding: 0.5em 1em;
    border-radius: 2px;
    transition: 0.15s ease-in-out left, 0.15s ease-in-out visibility, 0.15s ease-in-out opacity;
    visibility: hidden;
    opacity: 0 !important;

    &::before {
      position: absolute;
      content: '';
      background-color: black;
      top: 50%;
      transform: translateY(-50%) rotateZ(45deg);
      left: -4px;
      border-radius: 2px;
      width: 8px;
      height: 8px;
    }

    &.middle {
      top: 50%;
      transform: translateY(-50%);
    }

    &.bottom {
      bottom: -3px;

      &::before {
        top: unset;
        transform: rotateZ(45deg);
        bottom: 8px;
      }
    }
  }
}
</style>
