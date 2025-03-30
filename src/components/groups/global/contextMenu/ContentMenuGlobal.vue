<template>
  <div class="context-menu-container">
    <transition name="slide-up">
      <div class="context-menu" ref="elmRef" v-show="menu" :style="{
        top: menuY + 'px',
        left: menuX + 'px'
      }">
        <context-menu-item v-if="menu" :context="context" :open-to-left="openToLeft" :overflow-fix="overflowFix" @clicked="onOptionClick" :options="menu.options()" />
      </div>
    </transition>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {emitter} from '@/utils';
import {ContextMenu, MenuItem} from '@/core/context/menus/contextMenu';
import ContextMenuItem from '@/components/groups/global/contextMenu/ContextMenuItem.vue';

type ContextMenuEventContext = {
  context: () => any,
  menu: ContextMenu<any>,
  pointer: PointerEvent
}

@Component({
  components: {ContextMenuItem}
})
export default class ContextMenuGlobal extends Vue {
  menu: ContextMenu<any> | null = null;
  menuX = 0;
  menuY = 0;
  
  openToLeft = false;

  context: (() => any) | null = null;
  overflowFix = false;

  mounted() {
    emitter.on("context-menu-open", this.handleMenuOpen as any)
    window.addEventListener('click', this.handleOutOfClick)
  }

  destroyed() {
    emitter.off('context-menu-open', this.handleMenuOpen as any)
    window.removeEventListener('click', this.handleOutOfClick)
  }

  handleOutOfClick(event: MouseEvent) {
    if ((this.$refs.elmRef as any).contains(event.target)) {
      return;
    }

    this.handleMenuClose()
  }

  handleMenuOpen(context: ContextMenuEventContext) {
    this.menu = context.menu;
    this.context = context.context;

    this.$nextTick(() => {
      this.placeMenu(context.pointer);
    })
  }

  onOptionClick(option: MenuItem<any>) {
    this.handleMenuClose()
  }

  handleMenuClose() {
    this.menuX = 0;
    this.menuY = 0;
    this.menu = null;
    this.context = null;
    this.overflowFix = false;
    this.openToLeft = false;
  }

  placeMenu(pointer: PointerEvent) {
    this.overflowFix = false;
    this.openToLeft = false;
    const bodyBound = document.body.getBoundingClientRect();

    const elmBound = (this.$refs.elmRef as any)?.getBoundingClientRect();
    const elmHeight = elmBound?.height ?? 0;
    const elmWidth = this.calculateWidth();
    const realElmWidth = elmBound?.width ?? 0;
    
    this.menuX = pointer.clientX + 5;
    this.menuY = pointer.clientY + 5;

    if ((this.menuY + elmHeight) > (bodyBound.height - 20)) {
      this.menuY = this.menuY - elmHeight;
      this.overflowFix = true;
    }

    if ((this.menuX + elmWidth) > bodyBound.width) {
      this.menuX = this.menuX - realElmWidth;
      this.openToLeft = true;
    }

    if ((elmHeight + 50) > bodyBound.height) {
      this.menuY = 28.8;
    }
  }

  /**
   * This method is limited to a single child, this might need to be addressed if this ever changes
   */
  calculateWidth() {
    const containingElement = (this.$refs.elmRef as any);
    
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
}
</script>

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