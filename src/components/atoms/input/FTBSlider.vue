<template>
  <div class="flex flex-col ftb-slider" :class="{ dark }">
    <div class="w-full">
      <label class="block uppercase tracking-wide text-white-700 text-xs font-bold mb-4">
        {{ label }}
        <span
          class="normal-case"
          v-if="description"
          data-balloon-length="medium"
          :aria-label="description"
          data-balloon-pos="up"
          ><font-awesome-icon icon="info-circle"
        /></span>
      </label>
      <div class="slider-area">
        <div class="values">
          <span>{{ formatValue(minValue, true) }}</span>
          <span class="middle">{{ formatValue(currentValue) }}</span>
          <span>{{ formatValue(visualMax ? visualMax : maxValue, true) }}</span>
        </div>
        <input
          type="range"
          :value="value"
          :min="minValue"
          :max="maxValue"
          @change="$emit('input', $event.target.value)"
          @input="$emit('input', $event.target.value)"
          @blur="$emit('blur')"
          @mouseup="$emit('blur')"
          class="slider"
          :class="cssClass"
          :step="step === undefined ? 1 : step"
          :style="rawStyle"
        />
      </div>
    </div>
    <small class="text-muted mt-4" v-if="small">{{ small }}</small>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';

@Component
export default class FTBSlider extends Vue {
  @Prop({ default: '' }) label!: string;
  @Prop({ default: 100 }) maxValue!: number;
  @Prop({ default: 0 }) minValue!: number;
  @Prop({ default: 0 }) currentValue!: number;
  @Prop({ default: '' }) unit!: string;
  @Prop({ default: '' }) description!: string;
  @Prop({ default: '' }) cssClass!: string;
  @Prop({ default: '' }) rawStyle!: string;
  @Prop({ default: '' }) small!: string;

  @Prop({ default: '' }) dark!: boolean;
  @Prop() visualMax!: number;
  @Prop() maxValueLabel!: string;
  @Prop() step!: number;
  @Prop({ default: 0 }) value!: number;

  formatValue(value: number, dontShowThreads = false) {
    if (this.unit === 's') {
      if (value > 3600) {
        return value > 86400 ? (value / 86400).toFixed(2) + ' Days' : (value / 3600).toFixed(2) + ' Hours';
      }

      return value + ' Seconds';
    }

    if (value == 0) {
      return 'Unlimited';
    }

    if (value > 1024) {
      return (
        (value / 1024).toFixed(2) +
        (this.unit === ' threads' && !dontShowThreads ? ' threads' : this.unit === 'MB' ? 'GB' : this.unit)
      );
    }

    return value + (this.unit === 'threads' && dontShowThreads ? '' : ' ' + this.unit);
  }
}
</script>

<style lang="scss" scoped>
.ftb-slider {
  &.dark .slider-area {
    background-color: #252525;
  }

  .slider-area {
    background-color: var(--color-background);
    padding: 0.8rem 1.3rem 1rem 1.3rem;
    border-radius: 5px;
    position: relative;
    z-index: 1;

    .values {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.9rem;
      text-align: center;

      .middle {
        padding: 0.2rem 0.6rem;
        background-color: rgba(black, 0.4);
        border-radius: 4px;
      }

      span {
        display: block;
        position: relative;

        &:not(.middle)::before {
          position: absolute;
          content: '';
          top: 150%;
          left: 0;
          width: 2px;
          height: 20px;
          background-color: rgb(56, 56, 56);
          z-index: -1;
        }

        &:last-child::before {
          left: auto;
          right: 0;
        }
      }
    }
  }

  .slider {
    background-color: rgb(74, 74, 74);
    -webkit-appearance: none; /* Override default CSS styles */
    appearance: none;
    width: 100%; /* Full-width */

    border-radius: 15px;
    height: 8px; /* Specified height */
    outline: none; /* Remove outline */
    -webkit-transition: 0.2s; /* 0.2 seconds transition on hover */
    transition: opacity 0.2s;
  }

  /* The slider handle (use -webkit- (Chrome, Opera, Safari, Edge) and -moz- (Firefox) to override default look) */
  .slider::-webkit-slider-thumb {
    -webkit-appearance: none; /* Override default look */
    border-radius: 50%;
    appearance: none;
    width: 20px; /* Set a specific slider handle width */
    height: 20px; /* Slider handle height */
    background: var(--color-primary-button);
    cursor: pointer; /* Cursor on hover */
  }

  .slider::-moz-range-thumb {
    width: 20px; /* Set a specific slider handle width */
    height: 20px; /* Slider handle height */
    @apply bg-primary;
    cursor: pointer; /* Cursor on hover */
    border-radius: 50%;
  }

  .memory {
    /*background: linear-gradient(to right, #8e0c25 25%, #a55805 30%, #a55805 50%, #005540 55%);*/
    height: 10px;
    opacity: 1;
  }
}
</style>
