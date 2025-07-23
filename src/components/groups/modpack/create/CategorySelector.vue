<script lang="ts" setup>
import { useInstanceStore } from '@/store/instancesStore.ts';
import {ref, computed} from 'vue';
import UiSelect from "@/components/ui/select/UiSelect.vue";
import {faCheck, faPlus, faTimes} from "@fortawesome/free-solid-svg-icons";
import UiButton from "@/components/ui/UiButton.vue";
import {Input} from "@/components/ui";
import {alertController} from "@/core/controllers/alertController.ts";

const instanceStore = useInstanceStore();

const {
  label = undefined,
} = defineProps<{
  label?: string;
  openDown?: boolean;
}>()

const value = defineModel<string>()

const extraCategory = ref("");
const showCreate = ref(false);

const _options = computed(() => {
  const categories: any[] = (instanceStore.instanceCategories ?? []).map(e => ({ value: e, key: e }));
  
  if (categories.find(e => e.value === "Default") === undefined) {
    categories.push({ value: "Default", key: "Default" });
  }

  // Always sort the Default option to the top and the add new at the bottom, then the rest alphabetically
  categories.sort((a, b) => {
    if (a.value === "Default") return -1;
    if (b.value === "Default") return 1;
    if (a.value === "") return 1;
    if (b.value === "") return -1;
    return a.value.localeCompare(b.value);
  });

  return categories;
});

function confirm() {
  if (extraCategory.value === "") {
    showCreate.value = false;
  }
  
  const existingNames = _options.value.map(e => e.value.toLowerCase());
  if (existingNames.includes(extraCategory.value.toLowerCase())) {
    alertController.warning("Category already exists");
    return;
  }
  
  value.value = extraCategory.value;
  extraCategory.value = "";
  showCreate.value = false;
}
</script>

<template>
  <div class="" v-if="!showCreate">
    <UiSelect :label="label ?? 'Category'" 
              :options="_options"
              v-model="value"
              class="flex-1"
    >
      <template #suffix>
        <UiButton size="small" :icon="faPlus" @click="showCreate = true" />
      </template>
    </UiSelect>
  </div>
  <div class="" v-else>
    <Input placeholder="Category name" label="New Category Name" fill v-model="extraCategory">
      <template #suffix>
        <UiButton size="small" type="primary" :icon="faCheck" @click="confirm" />
      </template>
    </Input>
  </div>
</template>