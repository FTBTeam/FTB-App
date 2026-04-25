<script setup lang="ts">
import {computed} from "vue";
import {ReleaseChannelOptions} from "@/utils/commonOptions.ts";
import UiSelectSingle from "@/components/ui/select/UiSelectSingle.vue";

const model = defineModel<string>();

const { 
  includeUnset = false,
  disabled = false,
} = defineProps<{
  includeUnset?: boolean;
  disabled?: boolean;
}>();

const channelOptions = computed(() => ReleaseChannelOptions(includeUnset));
</script>

<template>
  <UiSelectSingle :options="channelOptions" v-model="model" placement="bottom-end" :disabled="disabled">
    <template #option="{ clazz, option }">
      <div class="flex justify-between gap-4" :class="clazz">
        <span>{{ option.value }}</span>
        <span>{{ option.icon }}</span>
      </div>
    </template>
  </UiSelectSingle>
</template>