<template>
  <div class="popover-wrapper">
    <slot></slot>
    <span :style="{ left: `${leftShift}%` }">{{ text }}</span>
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
}
</script>

<style scoped lang="scss">
.popover-wrapper {
  position: relative;
  &:hover {
    span {
      left: 120% !important;
      opacity: 1 !important;
      visibility: visible;
    }
  }

  // This is needed because the normal way of doing this keeps it open when you navigate, super annoying
  > span {
    display: block !important;
    position: absolute;
    left: 110%;
    top: 50%;
    transform: translateY(-50%);
    z-index: 30;
    font-size: var(--balloon-font-size);
    background: black;
    white-space: nowrap;
    padding: 0.5em 1em;
    border-radius: 2px;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans',
      'Helvetica Neue', sans-serif;
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
  }
}
</style>
