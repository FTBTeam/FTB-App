<script setup lang="ts">
  import {SugaredInstanceJson} from "@/core/types/javaApi";
  import Modal from "@/components/ui/modal/Modal.vue";
  import {ref, watch} from "vue";
  import {Input} from "@/components/ui";
  import CategorySelector from "@/components/groups/modpack/create/CategorySelector.vue";
  import {UiButton} from "@/components/ui";
  
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
  
  const instanceNameMap = ref<Record<string, string>>({});
  const instanceCategoryMap = ref<Record<string, string>>({});
  
  watch(() => instances, (newInstances) => {
    instanceNameMap.value = {};
    newInstances.forEach(instance => {
      instanceNameMap.value[instance.uuid] = instance.name + " (copy)";
      instanceCategoryMap.value[instance.uuid] = instance.category || "";
    });
  }, { immediate: true });
</script>

<template>
  <Modal :open="open" @closed="emit('close')" title="Duplicate Instances">
    {{ instances.length }} instances will be duplicated, by default they will be named with " (copy)" suffix and placed in the same category as the original instances.
    
    <div class="mt-4">
      <div v-for="(instance, index) in instances" :key="instance.uuid" class="mb-4 pt-4 border-t border-white/20" :class="{'border-t-0': index === 0}">
        <p class="font-bold mb-2">{{ instance.name }}</p>
        <div class="flex items-center gap-2">
          <Input class="w-1/2" fill v-model="instanceNameMap[instance.uuid]" label="New name" :placeholder="`New name for ${instance.name}`" />
          <CategorySelector class="w-1/2" v-model="instanceCategoryMap[instance.uuid]" />
        </div>
      </div>
    </div>
    
    <template #footer>
      <div class="flex gap-4 justify-end">
        <UiButton @click="emit('close')">Cancel</UiButton>
        <UiButton type="success" @click="emit('close')">Duplicate</UiButton>
      </div>
    </template>
  </Modal>
</template>