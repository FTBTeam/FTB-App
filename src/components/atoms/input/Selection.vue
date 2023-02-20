<template>
  <div class="select-box">
    <div class="label" v-if="label">{{ label }}</div>
    <div class="selection" tabindex="0" @focus="open = true" @focusout="open = false" :class="{ open }" ref="selection">
      <div class="main">
        <div class="item with-empty" v-if="!selected">
          <div class="badge empty">_</div>
          {{ placeholder }}
        </div>
        <div class="item" v-else>
          <div class="badge" :style="{ backgroundColor: selected.badge.color }" v-if="selected.badge">
            {{ selected.badge.text }}
          </div>
          <div v-else class="badge empty">_</div>
          <div class="text">{{ selected.text }}</div>
          <div class="meta" v-if="selected.meta">{{ selected.meta }}</div>
        </div>

        <font-awesome-icon class="arrow" icon="chevron-down" />
      </div>

      <div class="dropdown">
        <div class="item list-item" v-for="(option, index) in options" :key="index" @click="() => select(option)">
          <div class="badge" :style="{ backgroundColor: option.badge.color }" v-if="option.badge">
            {{ option.badge.text }}
          </div>
          <div class="text">{{ option.text }}</div>
          <div class="meta" v-if="option.meta">{{ option.meta }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

export type Option = {
  value: any;
  text: string;
  badge?: {
    color: string;
    text: string;
  };
  meta?: any;
};

@Component
export default class Selection extends Vue {
  @Prop() label!: string;
  @Prop({ default: 'Select option' }) placeholder!: string;
  @Prop({ default: [] }) options!: Option[];

  @Prop({ default: null }) inheritedSelection!: Option | null;

  open = false;
  selected: Option | null = null;

  mounted() {
    // Auto select the first option if there is only one (paul)
    if (this.options.length === 1) {
      this.select(this.options[0]);
    } else if (this.inheritedSelection != null) {
      const option = this.options.find(e => e === this.inheritedSelection);
      if (option) {
        this.select(option);
      }
    }
  }

  select(option: Option) {
    if (option.value === this.selected?.value) {
      this.selected = null;
      this.$emit('selected', null);
      return;
    }

    this.selected = option;
    this.$emit('selected', option.value);
    this.open = false;
    // Forcefully deselect the dropdown
    (this.$refs['selection'] as any).blur();
  }
}
</script>

<style lang="scss" scoped>
.select-box {
  .label {
    margin-bottom: 0.5rem;
  }
}

.selection {
  background: #252525;
  border-radius: 5px;
  padding: 0.55rem 1rem;
  cursor: pointer;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol';
  position: relative;
  border: 1px solid #1b1b1b;

  .main {
    padding-right: 2rem;
  }

  .arrow {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    right: 1rem;
    transition: transform 0.25s ease-in-out;
  }

  &.open {
    .dropdown {
      top: 120%;
      opacity: 1;
      visibility: visible;
      pointer-events: auto;
    }

    .arrow {
      transform: translateY(-50%) rotateZ(180deg);
    }
  }

  .dropdown {
    // prevent weirdness
    pointer-events: none;
    cursor: default;
    position: absolute;
    top: 150%;
    left: 0;
    background: var(--color-background);
    width: 100%;
    border-radius: 5px;
    box-shadow: 0 5px 0.5rem rgba(black, 0.2);
    border: 1px solid lighten(black, 10);
    z-index: 1000;
    padding: 1rem;
    max-height: 240px;
    overflow-y: auto;

    visibility: hidden;
    opacity: 0;

    transition: visibility 0.25s ease-in-out, opacity 0.25s ease-in-out, top 0.25s ease-in-out;
  }

  .item {
    display: flex;
    align-items: center;
    cursor: pointer;
    
    &.list-item:not(:last-child) {
      margin-bottom: 1rem;
    }

    &.list-item {
      position: relative;

      &::after {
        position: absolute;
        content: '';
        top: -20%;
        left: -1.5%;
        width: 103%;
        height: 140%;
        background-color: var(--color-navbar);
        z-index: -1;
        border-radius: 5px;
        visibility: hidden;
        opacity: 0;
        transition: visibility 0.25s ease-in-out, opacity 0.25s ease-in-out;
      }

      &:hover::after {
        visibility: visible;
        opacity: 1;
      }
    }

    .badge {
      padding: 0.2rem 0.5rem;
      border-radius: 5px;
      font-size: 0.875rem;
      margin-right: 1rem;

      &.empty {
        width: 0;
        padding: 0.2rem 0;
        margin-right: 0;
        background: transparent !important;
        visibility: hidden;
      }
    }

    .text {
      flex: 1;
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .meta {
      margin-left: 1rem;
    }
  }
}
</style>
