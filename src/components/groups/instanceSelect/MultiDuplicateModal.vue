<script setup lang="ts">
import {SugaredInstanceJson} from "@/core/types/javaApi";
import Modal from "@/components/ui/modal/Modal.vue";
import {onMounted, ref, watch} from "vue";
import {Input} from "@/components/ui";
import CategorySelector from "@/components/groups/modpack/create/CategorySelector.vue";
import {UiButton} from "@/components/ui";
import {InstanceController} from "@/core/controllers/InstanceController.ts";
import {alertController} from "@/core/controllers/alertController.ts";
import {defaultInstanceCategory} from "@/core/constants.ts";

const {
  instances,
  open,
} = defineProps<{
  instances: SugaredInstanceJson[],
  open: boolean,
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'deselect'): void;
}>();

const instanceNameMap = ref<Record<string, string>>({});
const instanceCategoryMap = ref<Record<string, string>>({});

onMounted(() => {
  updateNameMapping(instances);
})

async function duplicate() {
  for (const instance of instances) {
    // Resolve the new name and category for each instance
    const newName = instanceNameMap.value[instance.uuid] || (instance.name + " (copy)");
    const newCategory = instanceCategoryMap.value[instance.uuid] || instance.categoryId || defaultInstanceCategory;

    const controller = InstanceController.from(instance);
    try {
      await controller.duplicateInstance(newName, newCategory);
    } catch (e) {
      alertController.error(`Failed to duplicate instance ${instance.name}: ${e.message}`);
      console.error(e);
    } finally {
      emit('deselect');
      emit('close');
    }
  }
}

watch(() => instances, (newInstances) => {
  updateNameMapping(newInstances);
}, {immediate: true});

watch(() => open, (newOpen) => {
  if (newOpen) {
    updateNameMapping(instances);
  }
});

function updateNameMapping(newInstances: SugaredInstanceJson[]) {
  instanceNameMap.value = {};
  newInstances.forEach(instance => {
    instanceNameMap.value[instance.uuid] = instance.name + " (copy)";
    instanceCategoryMap.value[instance.uuid] = instance.categoryId || defaultInstanceCategory;
  });
}
</script>

<template>
  <Modal :open="open" @closed="emit('close')" title="Duplicate Instances">
    {{ instances.length }} instances will be duplicated, by default they will be named with " (copy)" suffix and placed
    in the same category as the original instances.

    <div class="mt-4">
      <div v-for="(instance, index) in instances" :key="instance.uuid" class="mb-4 pt-4 border-t border-white/20"
           :class="{'border-t-0': index === 0}">
        <p class="font-bold mb-2">{{ instance.name }}</p>
        <div class="flex items-center gap-2">
          <Input fill v-model="instanceNameMap[instance.uuid]" label="New name"
                 :placeholder="`New name for ${instance.name}`"/>
          <CategorySelector class="w-2/3" v-model="instanceCategoryMap[instance.uuid]"/>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="flex gap-4 justify-end">
        <UiButton @click="emit('close')">Cancel</UiButton>
        <UiButton type="success" @click="duplicate">Duplicate</UiButton>
      </div>
    </template>
  </Modal>
</template>