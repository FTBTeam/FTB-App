<script lang="ts" setup>
import {InstallRequest, InstallStatus} from '@/core/controllers/InstanceInstallController';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faCircleNotch, faTimes } from '@fortawesome/free-solid-svg-icons';
import { computed, ref } from 'vue';
import { useAppStore } from '@/store/appStore.ts';

const {
  entry,
  isInstall,
  isNext = false
} = defineProps<{
  entry: InstallRequest | InstallStatus;
  isInstall: boolean;
  isNext?: boolean;
}>()

const appStore = useAppStore();

const cancelling = ref(false);

async function cancelInstall(item: InstallRequest | InstallStatus) {
  if (cancelling.value) {
    return;
  }

  const uuid = isInstall ? (item as InstallStatus).request.uuid : (item as InstallRequest).uuid;

  cancelling.value = true;
  try {
    await appStore.controllers.install.cancelInstall(uuid, isInstall);
  } catch (e) {
    console.error(e);
  }
  cancelling.value = false;
}

const request = computed(() => {
  if (isInstall) {
    return (entry as InstallStatus).request;
  }
  
  return entry as InstallRequest;
})

const status = computed(() => {
  if (isInstall) {
    return entry as InstallStatus
  }
  
  return null;
})
</script>

<template>
  <div class="install-query-row flex flex-col gap-4">
    <div class="flex items-center justify-between gap-4">
      <div class="name flex items-center gap-4">
        <div v-if="request.logo" class="relative">
          <img class="rounded-lg border border-white/20" width="60" height="60" :src="request.logo" />

          <div class="absolute inset-0 rounded bg-black/20 backdrop-blur-xs flex items-center justify-center" v-if="status">
            <span class="font-bold text-xl mr-0.5">{{ status.progress }}</span><span class="pt-1">%</span>
          </div>
        </div>
        
        <div class="flex-1">
          <b class="line-clamp-1">{{request.name}}</b>
          
          <div class="status">
            <div class="install-status text-white/80" v-if="status">
              {{status.stage}} {{status.speed ? `(${(status.speed / 12500000).toFixed(2)} MB/s)` : ''}}
            </div>
            <div class="install-status text-white/80" v-else>
              Queued <span class="inline-block" aria-label="Up next for download" data-balloon-pos="down-left" v-if="isNext">(Next)</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="actions mr-1">
        <div class="btn">
          <FontAwesomeIcon :icon="faTimes" @click="cancelInstall(entry)" v-if="!cancelling" />
          <FontAwesomeIcon :icon="faCircleNotch" spin v-else />
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.install-query-row {
  font-size: 14px;
  font-weight: 500;
  
  .name {
  }
  
  .status {
    span {
      color: white;
      font-weight: bold;
    }
  }
  
  .install-status {
    font-weight: 500;
  }
  
  .actions {
    .btn {
      cursor: pointer;
      background-color: rgba(white, .1);
      padding: 0 .35rem;
      border-radius: 3px;
      transition: background-color .2s ease-in-out;
      
      &:hover {
        background-color: var(--color-warning-button);
      }
    }
  }
}
</style>