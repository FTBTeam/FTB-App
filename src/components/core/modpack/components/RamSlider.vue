<template>
 <div class="ram-slider">
   <div class="label">
     RAM Allocation
   </div>
   <div class="slider flex items-center gap-4  mb-1">
     <div class="slider-input relative flex flex-1 items-center">
       <div class="tooltip font-mono" :class="{'hide': hidden}" :style="`left: ${valueAsPercentage}%`">{{valueAsByteReadable}}</div>
       
       <input
         type="range"
         class="w-full"
         :value="value"
         @input="v => input(v.target.value)"
         @change="v => input(v.target.value)"
         @focus="hidden = false"
         @blur="hidden = true"
         @mouseup="() => {
           hidden = true;
           change(parseInt(value.toString()));
         }"
         @mousedown="hidden = false"
         :min="min"
         :max="maxRam"
         :step="step"
         style="background-color: #8ab839"
         list="ram-markers"
       />

       <datalist id="ram-markers">
         <option v-for="i in Math.ceil(this.maxRam / 1024) + 1" :value="(i - 1) * 1024" />
       </datalist>
     </div>
     
     <div class="value-input gap-2 flex items-center">
       <ftb-input type="number" style="width: 120px" :value="value.toString()" @input="v => updateValue(parseInt(v))" @change="change(parseInt(value.toString()));" />
     </div>
   </div>
   
   <ui-toggle v-if="maxRam <= this.settingsState.hardware.totalMemory" :align-right="true" label="Allow full ram allocation" v-model="allowDangerous" @input="resetMax" desc="It is recommended that in most cases that you stay below 10GB of RAM for a Minecraft instance. " />
 </div>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue} from 'vue-property-decorator';
import {SettingsState} from '@/modules/settings/types';
import {State} from 'vuex-class';
import UiButton from '@/components/core/ui/UiButton.vue';
import {prettyByteFormat, megabyteSize} from '@/utils';
import UiToggle from '@/components/core/ui/UiToggle.vue';

@Component({
  components: {UiToggle, UiButton}
})
export default class RamSlider extends Vue {
  @State('settings') public settingsState!: SettingsState;
  
  @Prop({default: 0}) min!: number;
  @Prop({default: 0}) value!: number | string; // stored in megabytes.
  @Emit() input(value: number) {}
  @Emit() change(value: number) {}
  
  mounted() {
    const maxRam = Math.min(1024 * 10, this.settingsState.hardware.totalMemory);
    if (this.value > maxRam) {
      this.allowDangerous = true;
    }
  }
  
  step = 16;
  allowDangerous = false;
  hidden = true;
  
  updateValue(value: number) {
    if (isNaN(value)) {
      this.input(this.min);
      return;
    }
    
    if (value < this.min) {
      this.input(this.min);
      return;
    }
    
    if (value > this.settingsState.hardware.totalMemory) {
      this.input(this.settingsState.hardware.totalMemory);
      return;
    }
    
    this.input(value);
  }
  
  resetMax() {
    if (this.value > this.maxRam) {
      this.input(this.maxRam);
    }
  }
  
  get maxRam() {
    return this.allowDangerous ? this.settingsState.hardware.totalMemory : Math.min(1024 * 10, this.settingsState.hardware.totalMemory);
  }
  
  get valueAsByteReadable() {
    return prettyByteFormat(Math.floor(parseInt(this.value.toString()) * megabyteSize));
  }
  
  get valueAsPercentage() {
    const v = parseInt(this.value?.toString() ?? "0");
    if (isNaN(v)) return 0;
    if (v == 0) return 6;
    
    return Math.min(Math.max(10, v / this.maxRam * 100), 86);
  }
}
</script>

<style lang="scss" scoped>
.tooltip {
  position: absolute;
  top: -2rem;
  left: 0;
  transform: translateX(-50%);
  padding: .2rem .5rem;
  background-color: black;
  border-radius: 5px;
  white-space: nowrap;
  transition: .2s opacity ease-in-out, .2s visibility ease-in-out;
  opacity: 0;
  visibility: hidden;
  
  &:not(.hide) {
    opacity: 1;
    visibility: visible;
  }
}
</style>