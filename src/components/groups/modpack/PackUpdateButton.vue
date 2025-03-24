<script lang="ts" setup>
import {InstanceJson, SugaredInstanceJson} from '@/core/types/javaApi';
import {packUpdateAvailable} from '@/utils/helpers/packHelpers';
import UpdateConfirmModal from '@/components/modals/UpdateConfirmModal.vue';
import { FTBButton } from '@/components/ui';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { ref, watch } from 'vue';
import { ModPack, Versions } from '@/core/types/appTypes.ts';

const {
  instance,
  localInstance,
} = defineProps<{
  instance: ModPack;
  localInstance: InstanceJson | SugaredInstanceJson;
}>()

const showConfirm = ref(false);
const latestVersion = ref<Versions | null>(null);

function updateAvailablePacks() {
  if (!instance || !localInstance) {
    return;
  }

  latestVersion.value = packUpdateAvailable(localInstance, instance) ?? null;
}

watch(() => instance, () => updateAvailablePacks)
watch(() => localInstance, () => updateAvailablePacks)
</script>

<template>
  <div class="update" v-if="instance && localInstance && latestVersion">
<!--    TODO: Replace with ui-button -->
    <FTBButton color="warning" class="update-btn px-4 py-1" @click="showConfirm = true">
      <span class="hidden sm:inline-block">Update available</span>
      <FontAwesomeIcon icon="cloud-download-alt" class="sm:ml-2" />
    </FTBButton>

    <UpdateConfirmModal v-if="localInstance && instance" :local-instance="localInstance" :latest-version="latestVersion" :open="showConfirm" @close="showConfirm = false" />
  </div>
</template>

<style lang="scss" scoped>
.update-btn {
  position: relative;

  box-shadow: 0 0 0 0 rgba(#ff801e, 1);
  animation: pulse 1.8s ease-in-out infinite;

  @keyframes pulse {
    0% {
      box-shadow: 0 0 0 0 rgba(#ff801e, 0.7);
    }

    70% {
      box-shadow: 0 0 0 10px rgba(#ff801e, 0);
    }

    100% {
      box-shadow: 0 0 0 0 rgba(#ff801e, 0);
    }
  }
}

.changes {
  border-top: 2px solid rgb(white, 0.05);
  padding-top: 1rem;
}
</style>
