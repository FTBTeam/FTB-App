<script lang="ts" setup>
import { useInstanceStore } from '@/store/instancesStore.ts';
import {ref, computed} from 'vue';
import UiSelect from "@/components/ui/select/UiSelect.vue";
import {faCheck, faPlus, faTimes} from "@fortawesome/free-solid-svg-icons";
import UiButton from "@/components/ui/UiButton.vue";
import {Input} from "@/components/ui";
import {alertController} from "@/core/controllers/alertController.ts";
import {defaultInstanceCategory} from "@/core/constants.ts";
import {toggleBeforeAndAfter} from "@/utils/helpers/asyncHelpers.ts";
import {sendMessage} from "@/core/websockets/websocketsApi.ts";

const instanceStore = useInstanceStore();

const {
  label = undefined,
} = defineProps<{
  label?: string;
}>()

const value = defineModel<string>()

const extraCategory = ref("");
const showCreate = ref(false);
const adding = ref(false);

const _options = computed(() => {
  const categories: any[] = (instanceStore.instanceCategories ?? []).map(e => ({ value: e.name, key: e.uuid }));

  // Always sort the Default option to the top and the add new at the bottom, then the rest alphabetically
  categories.sort((a, b) => {
    if (a.key === defaultInstanceCategory) return -1;
    if (b.key === defaultInstanceCategory) return 1;
    return a.value.localeCompare(b.value);
  });

  return categories;
});

async function confirm() {
  if (extraCategory.value === "") {
    showCreate.value = false;
  }
  
  const result = await toggleBeforeAndAfter(async () => {
    return sendMessage('instanceCategories', {
      action: 'CREATE',
      categoryName: extraCategory.value
    })
  }, (state) => adding.value = state);
  
  const categoryUuid = result.category?.uuid;
  if (!categoryUuid) {
    alertController.error("Failed to create category");
    return;
  }
  
  await instanceStore.reloadCategories();
  
  value.value = categoryUuid;
  extraCategory.value = "";
  showCreate.value = false;
}

const isValid = computed(() => {
  // Ensure the input isn't empty
  if (extraCategory.value === '') {
    return false;
  }
  
  // Check if the category already exists
  return instanceStore.instanceCategories.findIndex(e => e.name.toLocaleLowerCase() === extraCategory.value.toLocaleLowerCase()) === -1;
});
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
    <Input :disabled="adding" placeholder="Category name" label="New Category Name" fill v-model="extraCategory">
      <template #suffix>
        <UiButton :disabled="adding" size="small" type="danger" :icon="faTimes" @click="() => {
          extraCategory = '';
          showCreate = false;
        }" />
        <UiButton :disabled="!isValid || adding" size="small" type="primary" :icon="faCheck" @click="confirm" />
      </template>
    </Input>
  </div>
</template>