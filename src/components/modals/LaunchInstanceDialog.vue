<script lang="ts" setup>
import { Loader, UiButton, Modal } from '@/components/ui';
import { InstanceRunningData, useRunningInstancesStore } from '@/store/runningInstancesStore.ts';
import { useInstanceStore } from '@/store/instancesStore.ts';
import { computed } from 'vue';

const runningInstancesStore = useRunningInstancesStore();
const instancesStore = useInstanceStore();

const instance = computed(() => {
  if (!runningInstancesStore.launchingStatus) return null;
  return instancesStore.instances.find(i => i.uuid === runningInstancesStore.launchingStatus?.uuid);
})

async function clearLaunchingStatus() {
  console.log("Cleaning launching status");
  
  if (!instance.value) return;
  
  runningInstancesStore.stopped(instance.value?.uuid)
  runningInstancesStore.clearLaunchingStatus();
}

function title() {
  if (instance) {
    return `Launching ${instance.value?.name}`;
  }

  return "Launching Instance";
}

function subtitle() {
  if (!runningInstancesStore.launchingStatus) {
    return "Launching...";
  }

  if (runningInstancesStore.launchingStatus.error) {
    return "Error launching instance";
  }

  return runningInstancesStore.launchingStatus.step;
}

const latestPreInitProgress = computed<InstanceRunningData["preInitProgress"] | null>(() => {
  if (!instance) return null;
  const preInit = runningInstancesStore.instances.find(e => e.uuid === instance.value?.uuid)?.preInitProgress
  if (!preInit) return null;

  return preInit;
})

function numberToFixed(num: number) {
  return num.toFixed(2);
}
</script>

<template>
  <Modal :permanent="!runningInstancesStore.launchingStatus?.error" :open="runningInstancesStore.launchingStatus !== null" :title="title()" :sub-title="subtitle()" @closed="() => clearLaunchingStatus()">
    <template v-if="runningInstancesStore.launchingStatus">
      <div v-if="!runningInstancesStore.launchingStatus.error">
        <loader :title="runningInstancesStore.launchingStatus?.starting ? 'Starting...' : 'Logging in...'" />
        <div v-if="latestPreInitProgress" class="mt-6 pt-6 border-t border-white border-opacity-25">
          <p class="text-muted font-bold mb-4">Running pre-startup tasks...</p>
          
          <div class="flex gap-8 items-center">
            <div>
              <span class="font-bold text-3xl font-mono">{{ numberToFixed(latestPreInitProgress.progress) }}</span>
              <span class="text-lg ml-2">%</span>
            </div>
            
            <div>
              <div><b>{{ latestPreInitProgress.stepName }}</b></div>
              <span class="block mt-1 opacity-75 text-sm italic font-mono">
                ({{ latestPreInitProgress.step }}/{{ latestPreInitProgress.steps }})
                <span v-if="latestPreInitProgress.stepExtra"> - {{ latestPreInitProgress.stepExtra }}</span>
              </span>
            </div>
          </div>
        </div>
      </div>
      <div v-else>
        <p class="bold text-lg text-red-500 font-bold mb-3">Instance failed to launch!</p>
        
        <code class="whitespace-pre overflow-auto block bg-black p-2 rounded mb-6" style="max-height: 300px" v-if="runningInstancesStore.launchingStatus.error">
          {{runningInstancesStore.launchingStatus.error}}
        </code>
        
        <ui-button :full-width="true" @click="() => clearLaunchingStatus()">Close</ui-button>
      </div>
    </template>
    <span v-else>This shouldn't be possible</span>
  </Modal>
</template>