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
          <div v-if="!badgeEnd && option.badge" class="badge" :style="{ backgroundColor: option.badge.color }">
            {{ option.badge.text }}
          </div>
          <div class="text">{{ option.text }}</div>
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
  @Prop({ default: false }) badgeEnd!: boolean;

  @Prop({default: false}) allowDeselect!: boolean;
  
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
      if (this.allowDeselect) {
        this.selected = null;
        this.$emit('selected', null);
      }
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

<style lang="scss">

</style>
