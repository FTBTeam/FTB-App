<script setup lang="ts">
import {UiSelectOption} from "@/components/ui/select/UiSelect.ts";
import {getColorForReleaseType} from "@/utils";
import {toTitleCase} from "@/utils/helpers/stringHelpers.ts";
import UiSelect from "@/components/ui/select/UiSelect.vue";
import {UiBadge} from "@/components/ui";
import {Dayjs} from "dayjs";

export type VersionSelectorOption = UiSelectOption<{
  date: Dayjs;
  releaseType: string;
}>

const value = defineModel<string>();

const {
  options
} = defineProps<{
  options: VersionSelectorOption[]
}>();
</script>

<template>
  <UiSelect :options="options" v-model="value" class="mb-4 mt-6" :min-width="320">
    <template #option="{ option, clazz }">
      <div :class="clazz" class="flex items-center gap-2">
        <UiBadge :style="{ backgroundColor: getColorForReleaseType(option.releaseType) }">{{ toTitleCase(option.releaseType) }}</UiBadge>
        <span class="ml-2">{{ option.value }}</span>
        <span class="text-white/80 ml-auto">{{ option.date.fromNow() }}</span>
      </div>
    </template>
  </UiSelect>
</template>


