<script lang="ts" setup>
import Selection2, {SelectionOptions} from '@/components/ui/Selection2.vue';
import { UiButton, Modal, Input } from '@/components/ui';
import {alertController} from '@/core/controllers/alertController';
import {stringListContainsIgnoreCase} from '@/utils/helpers/arrayHelpers';
import { useInstanceStore } from '@/store/instancesStore.ts';
import { ref, computed } from 'vue';
import { faPlus } from '@fortawesome/free-solid-svg-icons';

const instanceStore = useInstanceStore();

const {
  label = undefined,
  openDown = false,
} = defineProps<{
  label?: string;
  openDown?: boolean;
}>()

const value = defineModel<string>()

const extraCategory = ref("");
const showCreate = ref(false);

function valueChanged(newValue: string | null) {
  if (newValue === "_") {
    showCreate.value = true;
    return;
  }
}

function addCategory() {
  if (extraCategory.value === "") return;
  if (stringListContainsIgnoreCase(instanceStore.instanceCategories, extraCategory.value)) {
    alertController.warning("A category with that name already exists.")
    return;
  }

  showCreate.value = false;
  value.value = extraCategory.value;
}

const _options = computed(() => {
  const categories: SelectionOptions = (instanceStore.instanceCategories ?? []).map(e => ({ value: e, label: e }));
  if (extraCategory.value !== "") {
    categories.push({ value: extraCategory.value, label: extraCategory.value });
    categories.push({
      value: "_", label: `Edit ${extraCategory.value}`, badge: {
        color: "#b46f2a",
        text: "+ Edit"
      }
    });
  } else {
    categories.push({
      value: "_", label: "Add new", badge: {
        color: "#2ab46e",
        text: "+ New"
      }
    });
  }

  // Always sort the Default option to the top and the add new at the bottom, then the rest alphabetically
  categories.sort((a, b) => {
    if (a.value === "Default") return -1;
    if (b.value === "Default") return 1;
    if (a.value === "_") return 1;
    if (b.value === "_") return -1;
    return a.value.localeCompare(b.value);
  });

  return categories;
});
</script>

<template>
  <div>
    <Selection2 :open-up="!openDown" :label="label ?? 'Category'" :options="_options"
                :value="value" @updated="v => valueChanged(v)" class="flex-1"/>
    
    <Modal :open="showCreate" title="New category" @closed="showCreate = false">
      <Input fill v-model="extraCategory" placeholder="FTB Packs" label="Category name" />
      <template #footer>
        <div class="flex justify-end">
          <UiButton @click="addCategory" type="success" :icon="faPlus">Create</UiButton>
        </div>
      </template>
    </Modal>
  </div>
</template>