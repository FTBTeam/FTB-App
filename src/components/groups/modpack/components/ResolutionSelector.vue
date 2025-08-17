<script setup lang="ts">
import UiSelect from "@/components/ui/select/UiSelect.vue";
import {InputNumber, UiToggle} from "@/components/ui";
import {computed} from "vue";
import {UiSelectOption} from "@/components/ui/select/UiSelect.ts";
import {computeAspectRatio} from "@/utils";
import {useAppSettings} from "@/store/appSettingsStore.ts";
import {faTv} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";

const appSettingsStore = useAppSettings();

export type ResolutionValue = {
  fullScreen: boolean;
  width: number;
  height: number;
}

const model = defineModel<ResolutionValue>({default: {
  fullScreen: false,
  width: 1920,
  height: 1080
}});

const emit = defineEmits<{
  (e: 'update', value: ResolutionValue): void;
}>();

function selectResolution(id: string) {
  if (id === null) return;

  const selected = appSettingsStore.systemHardware?.supportedResolutions.find(e => `${e.width}|${e.height}` === id);
  if (!selected) {
    return;
  }

  model.value.width = selected.width;
  model.value.height = selected.height;

  emit('update', model.value);
}

const resolutionList = computed(() => {
  const resList: UiSelectOption<{aspect: string}>[] = [];

  const resolutions = appSettingsStore.systemHardware?.supportedResolutions ?? [];

  resolutions.sort((a, b) => {
    const { width: aWidth, height: aHeight } = a;
    const { width: bWidth, height: bHeight } = b;

    if (aWidth !== bWidth) {
      return bWidth - aWidth;
    }

    return bHeight - aHeight;
  });

  for (const res of resolutions) {
    resList.push({
      key: `${res.width}|${res.height}`,
      value: `${res.width} x ${res.height}`,
      aspect: computeAspectRatio(res.width, res.height)
    })
  }

  return resList;
});

const resolutionId = computed(() => {
  if (!model.value.width || !model.value.height) {
    return undefined;
  }
  
  const computedKey = `${model.value.width}|${model.value.height}`;
  const result = resolutionList.value.find(e => e.key === computedKey);
  
  return result?.key ?? undefined
})
</script>

<template>
  <h2 class="text-lg mb-4 font-bold">
    <FontAwesomeIcon :icon="faTv" class="mr-2" />
    Resolution / Display Settings
  </h2>
  
  <div class="">
    <div class="flex items-center mb-4 gap-4" :class="{'cursor-not-allowed opacity-50 pointer-events-none': model.fullScreen}">
      <InputNumber input-class="w-[100px]" input label="Width" v-model="model.width" @blur="emit('update', model)" @update:model-value="emit('update', model)" />
      <InputNumber input-class="w-[100px]" label="Height" v-model="model.height" @blur="emit('update', model)" @update:model-value="emit('update', model)" />

      <UiSelect placement="bottom-end" class="ml-auto" label="Presets" placeholder="Select a preset" :options="resolutionList" v-model="resolutionId" @update:model-value="selectResolution">
        <template #option="{ option, clazz, isItem }">
          <div :class="clazz" class="flex justify-between items-center">
            <span :class="{'inline-block mr-6 text-white': !isItem}">{{ option.value }}</span>
            <span class="text-muted">{{ option.aspect }}</span>
          </div>
        </template>
      </UiSelect>
    </div>

    <ui-toggle
      label="Fullscreen"
      v-model="model.fullScreen"
      @update:model-value="emit('update', model)"
      class="mb-4"
      :align-right="true"
    />
  </div>
</template>
