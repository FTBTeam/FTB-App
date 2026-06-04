<script setup lang="ts" generic="T extends {key: string, value: string}">
import {UiSelectProps} from "@/components/ui/select/UiSelect.ts";
import UiSelect from "@/components/ui/select/UiSelect.vue";

const {
  label,
  disabled = false,
  fill = false,
  icon,
  placeholder = '',
  options,
  placement,
  minWidth = 180
} = defineProps<UiSelectProps<T>>();

const value = defineModel<string>({
  default: null
});
</script>

<template>
  <UiSelect :options="options" :label="label" :disabled="disabled" :fill="fill" :icon="icon" :placeholder="placeholder" :placement="placement" :minWidth="minWidth" v-model="value">
    <!-- Send give props from root to child. -->
    <template v-for="(_, name) in $slots" #[name]="slotProps">
      <slot :name="name" v-bind="slotProps ?? {}" />
    </template>
  </UiSelect>
</template>

<style scoped>

</style>