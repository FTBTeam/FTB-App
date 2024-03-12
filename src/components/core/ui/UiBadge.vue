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
import UiButton, {ElementAriaDirection, ElementColorType} from '@/components/core/ui/UiButton.vue';


@Component
export default class UiBadge extends Vue {
  @Prop({default: 'normal'}) type!: ElementColorType;
  @Prop() icon!: string;
  @Prop({ default: "" }) ariaLabel!: string;
  @Prop({ default: "down" as ElementAriaDirection }) ariaLabelPos!: ElementAriaDirection;
  
  @Prop({ default: false }) hoverEffect!: boolean;
  
  get colorFromType() {
    return UiButton.colorFromElementColorType(this.type, !this.hoverEffect, false);
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