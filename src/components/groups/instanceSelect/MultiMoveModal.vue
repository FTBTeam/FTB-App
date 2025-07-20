<script setup lang="ts">
  import {SugaredInstanceJson} from "@/core/types/javaApi";
  import Modal from "../../ui/modal/Modal.vue";
  import {UiButton, UiToggle} from "@/components/ui";
  import CategorySelector from "@/components/groups/modpack/create/CategorySelector.vue";
  import {ref, watch} from "vue";
  import {InstanceController} from "@/core/controllers/InstanceController.ts";
  import {alertController} from "@/core/controllers/alertController.ts";

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
  
  const instanceCategoryMap = ref<Record<string, string>>({});
  const useSingleCategory = ref(true);
  const selectedCategory = ref("Default");
  
  watch([() => instances, () => open], (values) => {
    if (values[0]) {
      updateNameMapping(values[0]);
    }
  }, { immediate: true });
  
  function updateNameMapping(instances: SugaredInstanceJson[]) {
    instanceCategoryMap.value = {};
    instances.forEach(instance => {
      instanceCategoryMap.value[instance.uuid] = instance.category || "";
    });
  }
  
  async function moveInstances() {
    for (const instance of instances) {
      const newCategory = useSingleCategory.value ? selectedCategory.value : instanceCategoryMap.value[instance.uuid];
      if (!newCategory) {
        continue; // Skip if no category is selected
      }
      
      const controller = InstanceController.from(instance);
      try {
        const res = await controller.updateInstance({
          category: newCategory
        });
        
        if (res) {
          alertController.success(`Instance ${instance.name} moved successfully to category ${newCategory}`);
        }
      } catch (e) {
        console.error(e);
        alertController.error(`Failed to move instance ${instance.name}`);
      } finally {
        emit('deselect')
        emit('close');
      }
    }
  }
</script>

<template>
  <Modal :open="open" @closed="emit('close')" title="Move Instances" :subTitle="`Options for moving ${instances.length} instances`">
    <UiToggle v-model="useSingleCategory" label="Use single category for all instances" />
    
    <div v-if="useSingleCategory" class="mt-4">
      <CategorySelector v-model="selectedCategory" label="Select category for all instances" />
    </div>
    
    <div class="mt-4" v-else>
      <div v-for="instance in instances" :key="instance.uuid" class="flex gap-2 flex-col mb-4 pt-4 border-t border-white/20">
        <p class="font-bold mb-2 flex-1">{{ instance.name }}</p>
        <CategorySelector v-model="instanceCategoryMap[instance.uuid]" />
      </div>
    </div>

    <template #footer>
      <div class="flex gap-4 justify-end">
        <UiButton @click="emit('close')">Cancel</UiButton>
        <UiButton type="success" @click="moveInstances">Move</UiButton>
      </div>
    </template>
  </Modal>
</template>