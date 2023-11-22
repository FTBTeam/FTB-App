<template>
  <div 
    class="ui-badge" 
    :class="colorFromType" 
    :aria-label="ariaLabel ? ariaLabel : undefined"
    :data-balloon-pos="ariaLabel && ariaLabelPos ? ariaLabelPos : undefined">
    
    <font-awesome-icon :fixed-width="true" v-if="icon" :icon="icon" />
    <slot />
  </div>
</template>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';

// TODO: (M#01) stop duplicating this
type BadgeType = 'primary' | 'secondary' | 'success' | 'danger' | 'warning' | 'info' | 'normal';
type AriaDirection = "up" | "down" | "left" | "right" | "up-left" | "up-right" | "down-left" | "down-right";

@Component
export default class UiBadge extends Vue {
  // TODO: (M#01) stop duplicating this
  @Prop({default: 'normal'}) type!: BadgeType;
  @Prop() icon!: string;
  @Prop({ default: "" }) ariaLabel!: string;
  @Prop({ default: "down" as AriaDirection }) ariaLabelPos!: AriaDirection;
  
  @Prop({ default: false }) hoverEffect!: boolean;

  // TODO: (M#01) stop duplicating this
  get colorFromType() {
    switch (this.type) {
      case 'primary': return `bg-indigo-600 ${this.hoverEffect ? 'hover:bg-indigo-500' : ''}`;
      case 'secondary': return `bg-green-600 ${this.hoverEffect ? 'hover:bg-green-500' : ''}`;
      case 'success': return `bg-primary ${this.hoverEffect ? 'hover:bg-light-primary' : ''}`;
      case 'danger': return `bg-danger ${this.hoverEffect ? 'hover:bg-light-danger' : ''}`;
      case 'warning': return `bg-warning ${this.hoverEffect ? 'hover:bg-light-warning' : ''}`;
      case 'info': return `bg-info ${this.hoverEffect ? 'hover:bg-light-info' : ''}`;
      case 'normal': return `bg-gray-600 ${this.hoverEffect ? 'hover:bg-gray-500' : ''}`;
    }
  }
}
</script>

<style lang="scss" scoped>
.ui-badge {
  display: inline-flex;
  align-items: center;
  gap: .5rem;

  font-weight: normal; 
  font-size: 0.75rem;
  padding: .15rem .5rem;
  
  border-radius: 3px;  
}
</style>