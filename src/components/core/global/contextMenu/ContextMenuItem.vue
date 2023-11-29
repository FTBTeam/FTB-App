<template>
  <div class="item" :class="{[option.color ?? 'normal']: true, 'has-children': option.children}" v-if="!option.predicate || option.predicate(context)" @click="() => $emit('clicked', option)">
    <div class="main">
      <font-awesome-icon :fixed-width="true" v-if="option.icon" :icon="option.icon" />
      {{ option.title }}
    </div>

    <font-awesome-icon class="chevron" v-if="option.children" icon="chevron-right" />

    <div class="child" :class="{'overflow-fix': overflowFix, 'open-to-left': openToLeft}" v-if="option.children">
      <div class="items">
        <template v-for="(childOption, index) in option.children">
          <div :key="index" class="separator" v-if="childOption['separator']"></div>
          <ContextMenuItem :context="context" :overflow-fix="overflowFix" v-else :option="childOption" @clicked="$emit('clicked', childOption)" />
        </template>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import {MenuItem} from '@/core/context/menus';

@Component({
  components: {
    ContextMenuItem
  }
})
export default class ContextMenuItem extends Vue {
  @Prop() option!: MenuItem<any>;
  @Prop() overflowFix!: boolean;
  @Prop() context!: any;
  @Prop({default: false}) openToLeft!: boolean;
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