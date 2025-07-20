<script setup lang="ts">
  import {SugaredInstanceJson} from "@/core/types/javaApi";
  import Modal from "../../ui/modal/Modal.vue";
  import {UiButton, UiToggle} from "@/components/ui";
  import CategorySelector from "@/components/groups/modpack/create/CategorySelector.vue";
  import {ref, watch} from "vue";

  const {
    instances,
    open,
  } = defineProps<{
    instances: SugaredInstanceJson[],
    open: boolean,
  }>();

  const emit = defineEmits<{
    (e: 'close'): void;
  }>();
  
  const instanceCategoryMap = ref<Record<string, string>>({});
  const useSingleCategory = ref(true);
  const selectedCategory = ref("Default");
  
  watch(() => instances, (newInstances) => {
    instanceCategoryMap.value = {};
    newInstances.forEach(instance => {
      instanceCategoryMap.value[instance.uuid] = instance.category || "";
    });
  }, { immediate: true });
</script>

<template>
  <Modal :open="open" @closed="emit('close')" title="Move Instances" :subTitle="`Options for moving ${instances.length} instances`">
    <UiToggle v-model="useSingleCategory" label="Use single category for all instances" />
    
    <div v-if="useSingleCategory" class="mt-4">
      <CategorySelector v-model="selectedCategory" label="Select category for all instances" />
    </div>
    
    <div class="mt-4" v-else>
      <div v-for="instance in instances" :key="instance.uuid" class="flex gap-4 items-center mb-4 pt-4 border-t border-white/20">
        <p class="font-bold mb-2 flex-1">{{ instance.name }}</p>
        <CategorySelector class="w-1/3" v-model="instanceCategoryMap[instance.uuid]" />
      </div>
    </div>

    <template #footer>
      <div class="flex gap-4 justify-end">
        <UiButton @click="emit('close')">Cancel</UiButton>
        <UiButton type="success" @click="emit('close')">Move</UiButton>
      </div>
    </template>
  </Modal>
</template>