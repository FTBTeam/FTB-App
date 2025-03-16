<script lang="ts" setup>
import {InstanceRunningData, LaunchingStatus} from '@/core/state/misc/runningState';
import {InstanceJson, SugaredInstanceJson} from '@/core/types/javaApi';
import { Loader, UiButton, Modal } from '@/components/ui';

// TODO: [port] fixme
// @Getter("instances", ns("v2/instances")) public instances!: (SugaredInstanceJson | InstanceJson)[];
// @Getter("launchingStatus", ns("v2/running")) public launchingStatus!: LaunchingStatus | null;
// @Getter("preInitProgress", ns("v2/running")) public preInitMessages!: (uuid: string) => InstanceRunningData["preInitProgress"] | null | undefined;
// @Action("clearLaunchingStatus", ns("v2/running")) public clearStatus!: () => void;

function instances(): (SugaredInstanceJson | InstanceJson)[] {}
function launchingStatus(): LaunchingStatus | null {}
function preInitMessages(uuid: string): InstanceRunningData["preInitProgress"] | null | undefined {}
function clearStatus() {}

async function clearLaunchingStatus() {
  console.log("Cleaning launching status");
  // TODO: [port] fixme
  // await store.dispatch('v2/running/stopped', this.instance?.uuid);
  clearStatus();
}

function title() {
  if (instance) {
    return `Launching ${instance.name}`;
  }

  return "Launching Instance";
}

function subtitle() {
  if (!launchingStatus) {
    return "Launching...";
  }

  if (launchingStatus.error) {
    return "Error launching instance";
  }

  return launchingStatus.step;
}

function instance() {
  if (!launchingStatus) return null;
  return instances.find(i => i.uuid === launchingStatus?.uuid);
}

function latestPreInitProgress() {
  if (!instance) return null;
  const preInit = preInitMessages(instance.uuid);
  if (!preInit) return null;

  return preInit;
}

function numberToFixed(num: number) {
  return num.toFixed(2);
}
</script>

<template>
  <Modal :permanent="!launchingStatus?.error" :open="launchingStatus !== null" :title="title" :sub-title="subtitle" @closed="() => clearLaunchingStatus()">
    <template v-if="launchingStatus">
      <div v-if="!launchingStatus.error">
        <loader :title="launchingStatus?.starting ? 'Starting...' : 'Logging in...'" />
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
        
        <code class="whitespace-pre overflow-auto block bg-black p-2 rounded mb-6" style="max-height: 300px" v-if="launchingStatus.error">
          {{launchingStatus.error}}
        </code>
        
        <ui-button :full-width="true" @click="() => clearLaunchingStatus()">Close</ui-button>
      </div>
    </template>
    <span v-else>This shouldn't be possible</span>
  </Modal>
</template>