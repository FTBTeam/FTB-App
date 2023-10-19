<template>
  <div class="select-box">
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
        console.warn("Duplicate keys detected in selection options for: " + this.label ?? "unknown");
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
