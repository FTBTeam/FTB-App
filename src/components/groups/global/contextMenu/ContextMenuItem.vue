<script lang="ts" setup>
import { isSeparator, MenuItem, MenuOptions } from '@/core/context/contextMenu';
import NestedContextMenuItem from '@/components/groups/global/contextMenu/ContextMenuItem.vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faChevronRight } from '@fortawesome/free-solid-svg-icons';

const {
  options,
  overflowFix,
  openToLeft = false,
  depth = 0
} = defineProps<{
  options: MenuOptions;
  overflowFix: boolean;
  openToLeft: boolean;
  depth?: number;
}>()

const emit = defineEmits<{
  (e: 'select', option: MenuItem): void;
}>()

function onItemClicked(option: MenuItem) {
  // A parent without an action only opens its children
  if (!option.action) {
    return;
  }

  emit('select', option);
}
</script>

<template>
  <div class="items">
    <template
         v-for="(option, key) in options"
         :key="key"
    >
      <div v-if="isSeparator(option)" class="item separator" />
      <div class="item"
           v-else-if="!option.hidden"
           :class="{[option.color ?? 'normal']: true, 'has-children': option.children}"
           @click.stop="onItemClicked(option)">
        <div class="main">
          <FontAwesomeIcon :fixed-width="true" v-if="option.icon" :icon="option.icon"/>
          {{ option.title }}
        </div>
  
        <FontAwesomeIcon class="chevron" v-if="option.children" :icon="faChevronRight"/>
  
        <div class="child rounded-lg backdrop-blur-lg" :class="{'overflow-fix': overflowFix, 'open-to-left': openToLeft}" v-if="option.children" @click.stop.prevent>
          <nested-context-menu-item :depth="depth + 1" :options="option.children" :overflow-fix="overflowFix" @click.stop.prevent
                                    :open-to-left="openToLeft" @select="(o) => emit('select', o)"/>
        </div>
      </div>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.item {
  position: relative;
  white-space: nowrap;
  display: flex;
  justify-content: space-between;
  align-items: center;

  cursor: pointer;
  padding: .4rem 1.2rem .4rem .6rem;
  border-radius: var(--border-radius-inner);
  transition: background-color .25s ease-in-out, color .25s ease-in-out;
  font-weight: 500;

  &:not(:last-child) {
    margin-bottom: .1rem;
  }

  &.has-children {
    padding: .4rem .6rem;

    .chevron {
      margin-right: 0;
    }
  }

  &:hover {
    background-color: rgb(255 255 255 / 15%);
  }

  &.danger {
    color: #f63c3c;
  }

  &.danger:hover {
    background-color: rgb(255 101 101 / 63%);
    color: rgba(255 255 255 / 80%);
  }

  &.warning {
    color: #f68a3c;
  }

  &.warning:hover {
    background-color: rgb(255 140 74 / 85%);
    color: rgba(255 255 255 / 80%);
  }

  svg {
    margin-right: .7rem;
  }

  &:hover {
    > .child {
      opacity: 1;
      visibility: visible;
    }
  }
  
  &.separator {
    background: none;
    cursor: default;
    border-bottom: 1px solid rgba(white, .1);
    border-radius: 0;
    margin-bottom: .5rem;
    padding-top: 0;
  }

  .child {
    padding: .4rem;
    position: absolute;
    left: calc(100% + .4rem);
    top: -.44rem;
    background-color: #0a0a0a;
    color: rgba(white, .8);
    border-radius: var(--border-radius);
    box-shadow: 0 3px 20px rgb(0 0 0 / 30%);
    min-width: 140px;
    opacity: 0;
    visibility: hidden;

    transition: opacity .25s ease-in-out, visibility .25s ease-in-out;
    
    &.open-to-left {
      left: unset;
      right: calc(100% + .4rem);
    }

    &.overflow-fix {
      bottom: -.44rem;
      top: unset;
    }
  }
}
</style>