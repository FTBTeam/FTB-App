<script lang="ts" setup>
import {megabyteSize, prettyByteFormat} from '@/utils';
import { computed, onMounted, ref } from 'vue';
import { InputNumber, UiToggle } from '@/components/ui';
import { useAppSettings } from '@/store/appSettingsStore.ts';

const appSettingsStore = useAppSettings();

const {
  min = 0,
} = defineProps<{
  min?: number;
}>()

const value = defineModel<number>()

const step = 16;
const allowDangerous = ref(false)
const hidden = ref(true)

onMounted(() => {
  const maxRam = Math.min(1024 * 10, (appSettingsStore.systemHardware?.totalMemory ?? 0));
  if ((value?.value ?? 0) > maxRam) {
    allowDangerous.value = true;
  }
})

function resetMax() {
  if ((value?.value ?? 0) > maxRam.value) {
    value.value = maxRam.value;
  }
}

const maxRam = computed(() => {
  return allowDangerous.value ? (appSettingsStore.systemHardware?.totalMemory ?? 0) : Math.min(1024 * 10, appSettingsStore.systemHardware?.totalMemory ?? 0);
})

const valueAsByteReadable = computed(() => {
  return prettyByteFormat(Math.floor(parseInt((value.value ?? 0).toString()) * megabyteSize));
});

const valueAsPercentage = computed(() => {
  const v = parseInt(value.value?.toString() ?? "0");
  if (isNaN(v)) return 0;
  if (v == 0) return 6;

  return Math.min(Math.max(10, v / (maxRam.value ?? 0) * 100), 86);
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
         v-model="value"
         @focus="hidden = false"
         @blur="hidden = true"
         @mouseup="hidden = true"
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
       <InputNumber v-model="value" :min="0" :max="appSettingsStore.systemHardware?.totalMemory ?? 0" />
     </div>
   </div>
   
   <UiToggle v-if="maxRam <= (appSettingsStore.systemHardware?.totalMemory ?? 0)" :align-right="true" label="Allow full ram allocation" v-model="allowDangerous" @input="resetMax" desc="It is recommended that in most cases that you stay below 10GB of RAM for a Minecraft instance. " />
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