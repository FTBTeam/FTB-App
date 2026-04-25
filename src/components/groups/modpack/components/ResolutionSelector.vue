<script setup lang="ts">
import {InputNumber, UiToggle} from "@/components/ui";
import {computed, onMounted, ref} from "vue";
import {UiSelectOption} from "@/components/ui/select/UiSelect.ts";
import {computeAspectRatio} from "@/utils";
import {faTv} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import platform from '@platform';
import UiSelectSingle from "@/components/ui/select/UiSelectSingle.vue";

export type ResolutionValue = {
  fullScreen: boolean;
  width: number;
  height: number;
}

const model = defineModel<ResolutionValue>({default: {
  fullScreen: false,
  width: 1080,
  height: 720
}});

const screenDimension = ref({
  width: 1080,
  height: 720
})

onMounted(() => {
  platform.utils.getScreenSize().then(dim => screenDimension.value = dim);
})

const emit = defineEmits<{
  (e: 'update', value: ResolutionValue): void;
}>();

function selectResolution(id: string) {
  if (id === null) return;

  const [ width, height ] = id.split('|');

  model.value.width = parseInt(width);
  model.value.height = parseInt(height);

  emit('update', model.value);
}

const commonResolutions = [
  [854, 480], [1280, 720], [1366, 768], [1600, 900],
  [1920, 1080], [2560, 1440], [3840, 2160]
].reverse();

const resolutionList = computed(() => {
  const resList: UiSelectOption<{aspect: string}>[] = [];

  const {width, height} = screenDimension.value || { width: 1080, height: 720 };
  
  const filteredResolutions = commonResolutions
    .filter(([w, h]) => w <= width && h <= height)
    .map(([width, height]) => ({ width, height }));

  for (const res of filteredResolutions) {
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
  <div>
    <h2 class="text-lg mb-4 font-bold">
      <FontAwesomeIcon :icon="faTv" class="mr-2" />
      Resolution / Display Settings
    </h2>

    <div class="">
      <div class="flex items-center mb-4 gap-4" :class="{'cursor-not-allowed opacity-50 pointer-events-none': model.fullScreen}">
        <InputNumber input-class="w-[100px]" input label="Width" v-model="model.width" @blur="emit('update', model)" @update:model-value="emit('update', model)" />
        <InputNumber input-class="w-[100px]" label="Height" v-model="model.height" @blur="emit('update', model)" @update:model-value="emit('update', model)" />

        <UiSelectSingle placement="bottom-end" class="ml-auto" label="Presets" placeholder="Select a preset" :options="resolutionList" v-model="resolutionId" @update:model-value="selectResolution">
          <template #option="{ option, clazz, isItem }">
            <div :class="clazz" class="flex justify-between items-center">
              <span :class="{'inline-block mr-6 text-white': !isItem}">{{ option.value }}</span>
              <span class="text-muted">{{ option.aspect }}</span>
            </div>
          </template>
        </UiSelectSingle>
      </div>

      <ui-toggle
        label="Fullscreen"
        v-model="model.fullScreen"
        @update:model-value="emit('update', model)"
        class="mb-4"
        :align-right="true"
      />
    </div>
  </div>
</template>
