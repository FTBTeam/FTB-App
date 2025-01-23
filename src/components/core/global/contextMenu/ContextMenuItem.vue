<template>
  <div class="items">
    <div class="item"
         v-for="(option, key) in options"
         :key="key"
         :class="{[option.color ?? 'normal']: true, 'has-children': option.children, 'separator': 'separator' in option, 'working': working[option.title]}"
         @click.stop="onItemClicked(option)"
         v-if="!option.predicate || option.predicate(context())">
      <div class="main">
        <font-awesome-icon :fixed-width="true" icon="spinner" spin v-if="working[option.title]" />
        <font-awesome-icon :fixed-width="true" v-else-if="option.icon" :icon="option.icon"/>
        {{ option.title }}
      </div>

      <font-awesome-icon class="chevron" v-if="option.children" icon="chevron-right"/>

      <div class="child" :class="{'overflow-fix': overflowFix, 'open-to-left': openToLeft}" v-if="option.children">
        <nested-context-menu-item :context="context" :options="option.children" :overflow-fix="overflowFix"
                                  :open-to-left="openToLeft" @clicked="onItemClicked"/>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import {MenuItemOrSeparator, MenuOptions} from '@/core/context/menus';

@Component({
  name: 'ContextMenuItem',
  components: {
    NestedContextMenuItem: () => import('@/components/core/global/contextMenu/ContextMenuItem.vue')
  }
})
export default class ContextMenuItem extends Vue {
  @Prop() context!: () => any;
  @Prop() options!: MenuOptions<any>;
  @Prop() overflowFix!: boolean;
  @Prop({default: false}) openToLeft!: boolean;
  
  working: {
    [key: string]: boolean
  } = {};
  
  async onItemClicked(option: MenuItemOrSeparator<any>) {
    if ("separator" in option) {
      return;
    }
    
    if (this.working[option.title]) {
      return;
    }
    
    this.working = {
      ...this.working,
      [option.title]: true
    }
    
    try {
      await option.action(this.context!())
      this.$emit('clicked', option);
    } catch (e) {
      console.error(e)
    } finally {
      this.working = {
        ...this.working,
        [option.title]: false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.item {
  position: relative;
  white-space: nowrap;
  display: flex;
  justify-content: space-between;
  align-items: center;

  cursor: pointer;
  padding: .4rem 1.2rem .4rem .6rem;
  border-radius: 3px;
  transition: background-color .25s ease-in-out, color .25s ease-in-out;

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
  
  &.working {
    animation: 1.2s ease-in-out infinite fade-in-out;
    
    @keyframes fade-in-out {
      0%, 100% {
        opacity: 1;
      }
      50% {
        opacity: .5;
      }
    }
  }
  
  &.separator {
    background: none;
    cursor: default;
    border-bottom: 1px solid rgba(white, .15);
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
    border-radius: 3px;
    box-shadow: 0 3px 20px rgb(0 0 0 / 30%);
    min-width: 180px;
    border: 1px solid rgba(white, .1);
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