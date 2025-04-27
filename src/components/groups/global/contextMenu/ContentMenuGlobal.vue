<script lang="ts" setup>
import {ContextMenu, MenuItem} from '@/core/context/menus/contextMenu';
import ContextMenuItem from '@/components/groups/global/contextMenu/ContextMenuItem.vue';
import { useAttachDomEvent } from '@/composables';
import { nextTick, onMounted, onUnmounted, ref, useTemplateRef } from 'vue';
import { useAppStore } from '@/store/appStore.ts';

type ContextMenuEventContext = {
  context: () => any,
  menu: ContextMenu<any>,
  pointer: PointerEvent
}

const appStore = useAppStore();

const menu = ref<ContextMenu<any> | null>(null);
const menuX = ref(0);
const menuY = ref(0);

const openToLeft = ref(false);

const context = ref<(() => any) | null>(null);
const overflowFix = ref(false);

const elmRef = useTemplateRef("elmRef");

useAttachDomEvent<MouseEvent>('click', handleOutOfClick)

onMounted(() => {
  appStore.emitter.on("action/context-menu-open", handleMenuOpen as any)
})

onUnmounted(() => {
  appStore.emitter.off('action/context-menu-open', handleMenuOpen as any)
})

function handleOutOfClick(event: MouseEvent) {
  if ((elmRef.value as any).contains(event.target)) {
    return;
  }
  
  handleMenuClose()
}

function handleMenuOpen(givenContext: ContextMenuEventContext) {
  menu.value = givenContext.menu;
  context.value = givenContext.context;

  nextTick(() => {
    placeMenu(givenContext.pointer);
  })
}

function onOptionClick(depth: number, option: MenuItem<any>) {
  handleMenuClose()
}

function handleMenuClose() {
  menuX.value = 0;
  menuY.value = 0;
  menu.value = null;
  context.value = null;
  overflowFix.value = false;
  openToLeft.value = false;
}

function placeMenu(pointer: PointerEvent) {
  overflowFix.value = false;
  openToLeft.value = false;
  const bodyBound = document.body.getBoundingClientRect();

  const elmBound = (elmRef.value as any)?.getBoundingClientRect();
  const elmHeight = elmBound?.height ?? 0;
  const elmWidth = calculateWidth();
  const realElmWidth = elmBound?.width ?? 0;

  menuX.value = pointer.clientX + 5;
  menuY.value = pointer.clientY + 5;

  if ((menuY.value + elmHeight) > (bodyBound.height - 20)) {
    menuY.value = menuY.value - elmHeight;
    overflowFix.value = true;
  }

  if ((menuX.value + elmWidth) > bodyBound.width) {
    menuX.value = menuX.value - realElmWidth;
    openToLeft.value = true;
  }

  if ((elmHeight + 50) > bodyBound.height) {
    menuY.value = 28.8;
  }
}

/**
 * This method is limited to a single child, this might need to be addressed if this ever changes
 */
function calculateWidth() {
  const containingElement = (elmRef.value as any);

  let minWidth = containingElement.getBoundingClientRect().width;
  const itemsWithChildren = containingElement.querySelectorAll('.item > .child');
  for (let i = 0; i < itemsWithChildren.length; i++) {
    const item = itemsWithChildren[i];
    const itemWidth = item.getBoundingClientRect().width;
    const newWidth = minWidth + itemWidth;
    minWidth = Math.max(newWidth, minWidth);
  }

  return minWidth
}
</script>

<template>
  <div class="context-menu-container">
    <transition name="slide-up">
      <div class="context-menu" ref="elmRef" v-show="menu" :style="{
        top: menuY + 'px',
        left: menuX + 'px'
      }">
        <ContextMenuItem v-if="menu && context" :context="context" :open-to-left="openToLeft" @close="handleMenuClose" :overflow-fix="overflowFix" @clicked="onOptionClick" :options="menu.options()" />
      </div>
    </transition>
  </div>
</template>

<style lang="scss" scoped>
.context-menu {
  $titleBarHeight: 28.8px;

  position: fixed;
  z-index: 10000;
  padding: .4rem;
  max-height: calc(100vh - $titleBarHeight);
  max-width: calc(100vw - $titleBarHeight);
  border: 1px solid rgba(white, .1);
  background-color: #0a0a0a;
  color: rgba(white, .8);
  box-shadow: 0 3px 20px rgb(0 0 0 / 30%);

  border-radius: 3px;
  min-width: 180px;

  .items {
    font-size: 0.875rem;

    .separator {
      height: 1px;
      background-color: rgba(255 255 255 / 20%);
      margin: .5rem 0;
    }
  }
}

.slide-up {
  &-enter-active, &-leave-active {
    transition: transform .25s ease-in-out, opacity .25s ease-in-out;
  }
  
  &-enter, &-leave-to {
    opacity: 0;
    transform: translateY(10px);
  }
}
</style>