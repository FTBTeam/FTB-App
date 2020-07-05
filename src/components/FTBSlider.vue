<template>
  <!-- <div class="flex flex-col my-2">
    <div class="flex flex-row justify-center my-4">
      <label class="block uppercase tracking-wide text-white-700 text-xs font-bold mb-2" for="grid-last-name">
        {{label}}
      </label>
      <p class="text-white mx-2">{{minValue}}{{unit}}</p>
      <div class="slide-container w-full">
          <input type="range" :value="value" :min="minValue" :max="maxValue" @input="$emit('input', $event.target.value)" v-on:blur="$emit('blur')" @mouseup="$emit('blur')" class="slider" :step="step === undefined ? 1 : step">
      </div>
      <p class="text-white mx-2">{{maxValueLabel === undefined ? maxValue : maxValueLabel}}{{unit}}</p>
      </div>
    </div> -->
    <div class="flex flex-col my-2">
    <div class="w-full px-1">
      <label class="block uppercase tracking-wide text-white-700 text-xs font-bold mb-2">
        {{label}}
        <span class="normal-case" v-if="description" data-balloon-length="medium" :aria-label="description" data-balloon-pos="up"><font-awesome-icon icon="info-circle"/></span>
      </label>
      <div class="flex flex-row justify-center">
        <p class="text-white mx-2" style="min-width: 6rem; width: auto">{{(unit == 's' ? ((currentValue > 3600) ? ((currentValue > 86400) ? (currentValue / 86400).toFixed(2) + 'd' : (currentValue / 3600).toFixed(2) + 'h' ) : currentValue + unit) : (currentValue == 0 ? 'Unlimited' : (currentValue > 1024 ? (currentValue / 1024).toFixed(2) : currentValue) + (unit === 'threads' ? ' threads' : (currentValue > 1024 ? 'GB' : unit))))}}</p>
        <!-- <input class="slider" id="grid-last-name" type="range" :value="value" :min="minValue" :max="maxValue" @input="$emit('input', $event.target.value)" v-on:blur="$emit('blur')" @mouseup="$emit('blur')" :step="step === undefined ? 1 : step"> -->
        <input type="range" :value="value" :min="minValue" :max="maxValue" @change="$emit('input', $event.target.value)" @input="$emit('input', $event.target.value)" v-on:blur="$emit('blur')" @mouseup="$emit('blur')" class="slider" :class="cssClass" :step="step === undefined ? 1 : step" :style="rawStyle">
        <!-- <p class="text-white mx-2">{{maxValueLabel === undefined ? maxValue : maxValueLabel}}{{unit}}</p> -->
      </div>
    </div>
    </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
@Component({
    props: [
        'label',
        'maxValue',
        'minValue',
        'currentValue',
        'value',
        'unit',
        'maxValueLabel',
        'step',
        'description',
        'cssClass',
        'rawStyle',
    ],
})
export default class FTBSlider extends Vue {
    @Prop({default: ''})
    public label!: string;
    @Prop({default: 100})
    public maxValue!: number;
    @Prop({default: 0})
    public minValue!: number;
    @Prop({default: 0})
    public currentValue!: number;
    @Prop({default: ''})
    public unit!: string;
    @Prop({default: ''})
    public description!: string;
    @Prop({default: ''})
    public cssClass!: string;
    @Prop({default: ''})
    public rawStyle!: string;
}
</script>

<style lang="scss">
.bg-input {
}
.slider {
  background-color: #252525;
  -webkit-appearance: none;  /* Override default CSS styles */
  appearance: none;
  width: 100%; /* Full-width */

  // border-radius: 5px;
  height: 15px; /* Specified height */
  outline: none; /* Remove outline */
  opacity: 0.7; /* Set transparency (for mouse-over effects on hover) */
  -webkit-transition: .2s; /* 0.2 seconds transition on hover */
  transition: opacity .2s;

}

/* Mouse-over effects */
.slider:hover {
  opacity: 1; /* Fully shown on mouse-over */
}

/* The slider handle (use -webkit- (Chrome, Opera, Safari, Edge) and -moz- (Firefox) to override default look) */
.slider::-webkit-slider-thumb {
  -webkit-appearance: none; /* Override default look */
border-radius: 50%;
  appearance: none;
  width: 25px; /* Set a specific slider handle width */
  height: 25px; /* Slider handle height */
  background: var(--color-primary-button);
  cursor: pointer; /* Cursor on hover */
}

.slider::-moz-range-thumb {
  width: 25px; /* Set a specific slider handle width */
  height: 25px; /* Slider handle height */
  @apply bg-primary;
  cursor: pointer; /* Cursor on hover */
    border-radius: 50%;

}

input[type='range'] {
    }

.memory{
  /*background: linear-gradient(to right, #8e0c25 25%, #a55805 30%, #a55805 50%, #005540 55%);*/
  height: 10px;
  opacity: 1;
}
</style>
