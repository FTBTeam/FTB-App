<script lang="ts" setup>
import {SettingsState} from '@/modules/settings/types';
import {megabyteSize, prettyByteFormat} from '@/utils';
import { computed, onMounted, ref } from 'vue';
import { FTBInput, UiToggle } from '@/components/ui';

// TODO: [port] fixme
// @State('settings') public settingsState!: SettingsState;
const settingsState: SettingsState = null;

const {
  min = 0,
} = defineProps<{
  min: number;
}>()

const value = defineModel<number>()
const emit = defineEmits<{
  (e: 'change', value: number): void;
}>()

const step = 16;
const allowDangerous = ref(false)
const hidden = ref(true)

onMounted(() => {
  const maxRam = Math.min(1024 * 10, settingsState.hardware.totalMemory);
  if (value.value > maxRam) {
    allowDangerous.value = true;
  }
})

function updateValue(newValue: number) {
  if (isNaN(newValue)) {
    value.value = min;
    return;
  }
  
  if (newValue < min) {
    value.value = min;
    return;
  }
  
  if (newValue > settingsState.hardware.totalMemory) {
    value.value = settingsState.hardware.totalMemory;
    return;
  }
  
  value.value = newValue;
}

function resetMax() {
  if (value.value > maxRam) {
    value.value = maxRam;
  }
}

const maxRam = computed(() => {
  return allowDangerous.value ? settingsState.hardware.totalMemory : Math.min(1024 * 10, settingsState.hardware.totalMemory);
})

const valueAsByteReadable = computed(() => {
  return prettyByteFormat(Math.floor(parseInt(value.value.toString()) * megabyteSize));
});

const valueAsPercentage = computed(() => {
  const v = parseInt(value.value?.toString() ?? "0");
  if (isNaN(v)) return 0;
  if (v == 0) return 6;

  return Math.min(Math.max(10, v / maxRam.value * 100), 86);
});
</script>

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
         @input="v => value = v.target.value"
         @change="v => value = v.target.value"
         @focus="hidden = false"
         @blur="hidden = true"
         @mouseup="() => {
           hidden = true;
           value = parseInt(value.toString()) 
         }"
         @mousedown="hidden = false"
         :min="min"
         :max="maxRam"
         :step="step"
         style="background-color: #8ab839"
         list="ram-markers"
       />

       <datalist id="ram-markers">
         <option v-for="i in Math.ceil(maxRam / 1024) + 1" :value="(i - 1) * 1024" />
       </datalist>
     </div>
     
     <div class="value-input gap-2 flex items-center">
       <FTBInput @blur="change(parseInt(value.toString()))" type="number" style="width: 120px" :value="value.toString()" @input="v => updateValue(parseInt(v))" @change="change(parseInt(value.toString()));" />
     </div>
   </div>
   
   <UiToggle v-if="maxRam <= settingsState.hardware.totalMemory" :align-right="true" label="Allow full ram allocation" v-model="allowDangerous" @input="resetMax" desc="It is recommended that in most cases that you stay below 10GB of RAM for a Minecraft instance. " />
 </div>
</template>

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