<script lang="ts" setup>
import {faArrowRight, faCopy, faTimes, faTrash} from "@fortawesome/free-solid-svg-icons";
  import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
  import UiButton from "../../ui/UiButton.vue";
  import {useAds} from "@/composables/useAds.ts";
  import {SugaredInstanceJson} from "@/core/types/javaApi";
  import MultiDuplicateModal from "@/components/groups/instanceSelect/MultiDuplicateModal.vue";
  import MultiMoveModal from "@/components/groups/instanceSelect/MultiMoveModal.vue";
import {computed, ref} from "vue";
  import {dialogsController} from "@/core/controllers/dialogsController.ts";
  
  const ads = useAds();
  
  const {
    instances,
  } = defineProps<{
    instances: SugaredInstanceJson[]
  }>();
  
  const emit = defineEmits<{
    (e: 'deselect-all'): void;
  }>();
  
  const multiMoveModal = ref(false);
  const multiDuplicateModal = ref(false);

  async function deleteInstances() {
    if (!instances.length) {
      return;
    }
    
    if (!(await dialogsController.createConfirmationDialog("Are you sure?", `You are about to delete ${instances.length} instance${instances.length > 1 ? 's' : ''}. This action cannot be undone.`))) {
      return;
    }
    
    // TODO: Implement delete logic
    
    emit('deselect-all');
  }
  
  const containerWidth = computed(() => {
    const subtractWidth = 70 + (ads.adsEnabled.value ? 400 : 0);
    return `calc(100% - ${subtractWidth}px)`;
  });
</script>

<template>
  <transition name="transition-fade">
    <div
      v-if="instances.length > 0"
      class="fixed bottom-0 left-[70px] w-full z-10 px-4 pb-6 flex items-center justify-center"
      :style="{ width: containerWidth }"
    >
      <div class="bg-[#2a2a2a]/90 backdrop-blur border-1 border-white/15 shadow-lg rounded-lg py-3 px-5 flex items-center gap-6">
        <div class="text-white flex-1">
          <span class="px-2 py-1 bg-white/20 rounded mr-2 inline-block font-bold font-mono">{{ instances.length }}</span> selected
        </div>

        <div class="flex items-center gap-3 border-l border-r border-white/30 px-4">
          <UiButton size="small" :icon="faArrowRight" @click="multiMoveModal = true">
            Move
          </UiButton>

          <UiButton size="small" :icon="faCopy" @click="multiDuplicateModal = true">
            Duplicate
          </UiButton>

          <UiButton size="small" :icon="faTrash" type="danger" @click="deleteInstances">
            Delete
          </UiButton>
        </div>

        <div class="cursor-pointer p-1 rounded hover:bg-white/20" @click="emit('deselect-all')">
          <FontAwesomeIcon :icon="faTimes" fixed-width />
        </div>
      </div>
    </div>
  </transition>
  
  <MultiDuplicateModal @close="multiDuplicateModal = false" :open="multiDuplicateModal" :instances="instances" />
  <MultiMoveModal @close="multiMoveModal = false" :open="multiMoveModal" :instances="instances" />
</template>

<style lang="scss" scoped>

</style>