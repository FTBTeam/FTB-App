<template>
  <div class="select-box" :class="{disabled}">
    <div class="label" v-if="label && !icon">{{ label }}</div>
    <div class="selection" tabindex="0" @focus="open = true" @focusout="open = false" :class="{ open }" ref="selection">
      <div class="main" v-if="!icon">
        <div class="item with-empty" v-if="!selected">
          <div class="badge empty">_</div>
          {{ placeholder }}
        </div>
        <div class="item" v-else>
          <div class="badge" :style="{ backgroundColor: selected.badge.color }" v-if="selected.badge">
            {{ selected.badge.text }}
          </div>
          <div v-else class="badge empty">_</div>
          <div class="text">{{ selected.label }}</div>
          <div class="meta" v-if="selected.meta">{{ selected.meta }}</div>
        </div>

        <font-awesome-icon class="arrow" icon="chevron-down" />
      </div>
      
      <div class="main-icon" v-else>
        <font-awesome-icon :fixed-width="true" :icon="icon" />
      </div>

      <div class="dropdown" :class="{'open-up': openUp, [direction]: true}" :style="{width: (minWidth === 0 ? undefined : minWidth + 'px')}">
        <div class="item list-item" :class="{'no-badge': !option.badge}" v-for="(option, index) in options" :key="index" @click="() => select(option)">
          <div v-if="!badgeEnd && option.badge" class="badge" :style="{ backgroundColor: option.badge.color }">
            <font-awesome-icon v-if="option.badge.icon" :icon="option.badge.icon" class="mr-1" />
            {{ option.badge.text }}
          </div>
          <div class="text">{{ option.label }}</div>
          <div class="meta" v-if="option.meta">{{ option.meta }}</div>
          <!-- Eww -->
          <div v-if="badgeEnd && option.badge" class="badge end" :style="{ backgroundColor: option.badge.color }">
            {{ option.badge.text }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue} from 'vue-property-decorator';
import {consoleBadButNoLogger} from '@/utils';

export type SelectionOptions = SelectionOption[];
export type SelectionOption = {
  label: string;
  value: any;
  badge?: {
    color?: string;
    text: string;
    icon?: string | string[];
  };
  meta?: any;
};

// TODO: (M#01) Migrate everything to this one.
@Component
export default class Selection2 extends Vue {
  @Prop() label!: string;
  @Prop({default: null}) icon!: string | string[] | null;
  @Prop({default: 'left'}) direction!: 'left' | 'right';
  @Prop({default: 0}) minWidth!: number;
  
  @Prop({ default: 'Select option' }) placeholder!: string;
  @Prop({ default: () => [] }) options!: SelectionOptions;
  
  @Prop({ default: false }) badgeEnd!: boolean;
  @Prop({default: false}) allowDeselect!: boolean;
  
  @Prop({default: false}) openUp!: boolean;
  @Prop({default: false}) disabled!: boolean;
  
  @Prop({default: null}) value!: any;
  @Emit() change(value: any) {
    return value;
  }
  
  open = false;
  
  mounted() {
    if (this.options.length) {
      // Check for duplicate keys and warn
      const keys = this.options.map(o => o.value);
      const uniqueKeys = new Set(keys);
      if (keys.length !== uniqueKeys.size) {
        consoleBadButNoLogger("W", "Duplicate keys detected in selection options for: " + this.label ?? "unknown");
      }
    }
  }
  
  select(option: SelectionOption) {
    if (option.value === this.value) {
      if (this.allowDeselect) {
        this.$emit('input', null)
        this.$emit('change', null)
      }
      this.open = false;
      // Forcefully deselect the dropdown
      (this.$refs['selection'] as any).blur();
      return;
    }

    this.$emit('input', option.value)
    this.$emit('change', option.value)
    this.open = false;
    // Forcefully deselect the dropdown
    (this.$refs['selection'] as any).blur();
  }
  
  get selected() {
    return this.options.find(o => o.value === this.value) ?? null;
  }
}
</script>

<style scoped lang="scss">
.select-box {
  &.disabled {
    opacity: 0.5;
    pointer-events: none;
  }
  
  .label {
    margin-bottom: 0.5rem;
  }

  .selection {
    background: #252525;
    border-radius: 5px;
    padding: 0.55rem;
    cursor: pointer;
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

      &.right {
        right: 0;
        left: unset;
      }

      &.open-up {
        top: auto;
        bottom: 120%;
      }
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

      &.no-badge {
        padding: 0.1rem 0;
      }

      .badge {
        padding: 0.1rem 0.5rem;
        border-radius: 3px;
        font-size: 0.875rem;
        margin-right: 1rem;

        &.end {
          margin-right: 0;
          margin-left: 1rem;
        }

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
        font-weight: bold;
      }

      .meta {
        margin-left: 1rem;
        opacity: .8;
      }
    }
  }
}
</style>
