<script lang="ts" setup>
import ContextMenuItem from '@/components/groups/global/contextMenu/ContextMenuItem.vue';
import { useAttachDomEvent } from '@/composables';
import { nextTick, ref, useTemplateRef, watch } from 'vue';
import { closeContextMenu, MenuItem, openedContextMenu, runMenuItem } from '@/core/context/contextMenu';

const menuX = ref(0);
const menuY = ref(0);

const openToLeft = ref(false);
const overflowFix = ref(false);

const elmRef = useTemplateRef("elmRef");

useAttachDomEvent<MouseEvent>('click', handleOutOfClick)

watch(openedContextMenu, (opened) => {
  overflowFix.value = false;
  openToLeft.value = false;

  if (!opened) {
    menuX.value = 0;
    menuY.value = 0;
    return;
  }

  nextTick(() => placeMenu(opened.pos.x, opened.pos.y));
})

function handleOutOfClick(event: MouseEvent) {
  if (!openedContextMenu.value || (elmRef.value as any)?.contains(event.target)) {
    return;
  }

  closeContextMenu()
}

function onSelect(item: MenuItem) {
  runMenuItem(item);
}

function placeMenu(x: number, y: number) {
  overflowFix.value = false;
  openToLeft.value = false;
  const bodyBound = document.body.getBoundingClientRect();

  const elmBound = (elmRef.value as any)?.getBoundingClientRect();
  const elmHeight = elmBound?.height ?? 0;
  const elmWidth = calculateWidth();
  const realElmWidth = elmBound?.width ?? 0;

  menuX.value = x;
  menuY.value = y;

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
      <div class="context-menu rounded-lg" ref="elmRef" v-show="openedContextMenu" :style="{
        top: menuY + 'px',
        left: menuX + 'px'
      }">
        <ContextMenuItem v-if="openedContextMenu" :open-to-left="openToLeft" :overflow-fix="overflowFix" @select="onSelect" :options="openedContextMenu.items" />
      </div>
    </transition>
  </div>
</template>

<style lang="scss" scoped>
.context-menu {
  $titleBarHeight: 2.5rem;

  position: fixed;
  z-index: 10000;
  padding: .4rem;
  max-height: calc(100vh - $titleBarHeight);
  max-width: calc(100vw - $titleBarHeight);
  background-color: #0a0a0a;
  color: rgba(white, .8);
  box-shadow: 0 3px 20px rgb(0 0 0 / 30%);
  min-width: 140px;

  .items {
    .separator {
      height: 1px;
      background-color: rgba(255, 255, 255, .1);
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