<script lang="ts" setup>
import {InstallRequest, InstallStatus} from '@/core/controllers/InstanceInstallController';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { services } from '@/bootstrap.ts';
import { faCircleNotch, faTimes } from '@fortawesome/free-solid-svg-icons';

const {
  item,
  isInstall,
  isNext = false
} = defineProps<{
  item: InstallRequest | InstallStatus;
  isInstall: boolean;
  isNext?: boolean;
}>()

const cancelling = ref(false);

async function cancelInstall(item: InstallRequest | InstallStatus) {
  if (cancelling.value) {
    return;
  }

  const uuid = isInstall ? (item as InstallStatus).request.uuid : (item as InstallRequest).uuid;

  cancelling.value = true;
  try {
    await services().instanceInstallController.cancelInstall(uuid, isInstall);
  } catch (e) {
    console.error(e);
  }
  cancelling.value = false;
}

const request = computed(() => {
  if (isInstall) {
    return (item as InstallStatus).request;
  }
  
  return item as InstallRequest;
})

const status = computed(() => {
  if (isInstall) {
    return item as InstallStatus
  }
  
  return null;
})
</script>

<template>
  <div class="install-query-row flex gap-4 items-center">
    <div class="name flex-1">
      {{request.name}}
    </div>
    <div class="status">
      <div class="install-status" v-if="status">
        {{status.stage}} - <span>{{ status.progress }}%</span> {{status.speed ? `(${(status.speed / 12500000).toFixed(2)} MB/s)` : ''}}
      </div>
      <div class="install-status" v-else>
        Queued <span class="inline-block" aria-label="Up next for download" data-balloon-pos="down-right" v-if="isNext">(Next)</span>
      </div>
    </div>
    <div class="actions">
      <div class="btn">
        <FontAwesomeIcon :icon="faTimes" @click="cancelInstall(item)" v-if="!cancelling" />
        <FontAwesomeIcon :icon="faCircleNotch" spin v-else />
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.install-query-row {
  padding: 0.45rem;
  border-bottom: 1px solid rgba(white, .1);
  font-size: 14px;
  font-weight: 500;
  background-color: rgba(white, .05);
  border-radius: 0.25rem;
  
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