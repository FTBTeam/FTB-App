<script lang="ts" setup>
import {ProgressBar, UiButton} from '@/components/ui';
import {RouterNames} from '@/router';
import {alertController} from '@/core/controllers/alertController';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {safeNavigate} from '@/utils';
import {createLogger} from '@/core/logger';
import platform from '@platform';
import {computed, onMounted, onUnmounted, ref, watch} from 'vue';
import {InstanceMessageData, InstanceRunningData, useRunningInstancesStore} from '@/store/runningInstancesStore.ts';
import {useRouter} from 'vue-router';
import {useInstanceStore} from '@/store/instancesStore.ts';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {
  faArrowLeft,
  faCircleNotch,
  faEllipsisVertical,
  faFolderOpen,
  faSkullCrossbones,
} from '@fortawesome/free-solid-svg-icons';
// @ts-ignore (Literally no types :tada:)
import {RecycleScroller} from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import {artworkFileOrElse} from '@/utils/helpers/packHelpers.ts';
import { AppContextController } from '@/core/context/contextController';
import {ContextMenus} from "@/core/context/contextMenus.ts";

export interface Bar {
  title: string;
  steps: number;
  step: number;
  message: string;
}

const logger = createLogger("LaunchingPage.vue");

const router = useRouter();
const instanceStore = useInstanceStore();
const runningInstanceStore = useRunningInstancesStore();

const enabledLogTypes = ref(["Info", "Warn", "Error"]);
const logTypes = ref(["Info", "Warn", "Error", "Debug", "Trace"]);
const loading = ref(false);

const logsKey = ref(enabledLogTypes.value.join("|"));

const instanceFolders = ref<string[]>([]);
const hasCrashed = ref(false);
const disableFollow = ref(false);

const emptyStep = {
  desc: '',
  step: 0,
  totalSteps: 0,
  progress: 0,
  progressHuman: '',
} as InstanceRunningData['startup']['step'];

const currentStep = ref<InstanceRunningData['startup']['step']>(emptyStep);

const finishedLoading = ref(false);
const preInitMessages = ref<Set<string>>(new Set());
const messages = ref<InstanceMessageData[]>([]);
const launchProgress = ref<Bar[] | null | undefined>(null);
const scrollIntervalRef = ref<any>(null);

const instance = computed(() => instanceStore.instances.find(e => e.uuid === router.currentRoute.value.params.uuid) ?? null);
const runningInstance = computed(() => runningInstanceStore.instances.find(e => e.uuid === router.currentRoute.value.params.uuid) ?? null);

onMounted(async () => {
  loading.value = false;
  hasCrashed.value = false;
  currentStep.value = emptyStep;
  finishedLoading.value = false;
  preInitMessages.value = new Set();
  messages.value = [];
  launchProgress.value = null;

  if (localStorage.getItem("enabledLogTypes")) {
    enabledLogTypes.value = localStorage.getItem("enabledLogTypes")!.split(",");
    logsKey.value = enabledLogTypes.value.join("|");
  }

  logger.debug("Websockets ready, loading instance")

  if (instance.value == null) {
    alertController.error('Instance not found')
    logger.debug("Instance not found, redirecting to library", router.currentRoute.value.params.uuid)
    await safeNavigate(RouterNames.ROOT_LIBRARY);
    return;
  }

  // Sync from any previous data
  if (runningInstance.value) {
    syncDataFromRunningData()
  }

  sendMessage("getInstanceFolders", {
    uuid: instance.value?.uuid ?? ""
  })
    .then((e) => (instanceFolders.value = e.folders))
    .catch(e => logger.error("Failed to get instance folders", e))

  scrollIntervalRef.value = setInterval(scrollToBottom, 500);
});

onUnmounted(() => {
  // Stop listening to events!
  clearInterval(scrollIntervalRef.value)
})

watch(() => runningInstance, (value) => {
  if (!value) {
    return;
  }
  
  syncDataFromRunningData()
})

function syncDataFromRunningData() {
  if (!runningInstance.value) {
    return;
  }
  
  const data = runningInstance.value;
  hasCrashed.value = data.status.crashed;
  finishedLoading.value = data.status.finishedLoading;
  launchProgress.value = data.startup.bars;
  messages.value = data.messages;
  preInitMessages.value = data.preInitMessages;
  currentStep.value = data.startup.step;
}

function cancelLoading() {
  logger.debug("Attempting to kill instance")
  sendMessage("instance.kill", {
    uuid: instance.value?.uuid ?? ""
  }).catch((error) => {
    logger.error("Failed to kill instance", error)
  });
}

function userInteractedWithLogs(event: any) {
  const location = event.target.scrollTop + event.target.clientHeight;

  // Give a 10px buffer
  disableFollow.value = location < event.target.scrollHeight - 5;
}

function scrollToBottom() {
  if (disableFollow.value) {
    return;
  }

  const elm = document.getElementById('log-container');
  if (elm) {
    elm.scrollTop = elm.scrollHeight;
  }
}

function leavePage() {
  if (instance.value) {
    router.push({ name: RouterNames.ROOT_LOCAL_PACK, params: { uuid: instance.value?.uuid ?? "" } });
  } else {
    router.push({ name: RouterNames.ROOT_LIBRARY });
  }
}

function openFolder() {
  gobbleError(async () => {
    await platform.io.openFinder(`${instance.value?.path ?? ""}`)
  })
}

const bars = computed(() => {
  if (launchProgress.value === null) {
    return [];
  }

  return launchProgress.value?.filter((b) => b.steps !== 1).slice(0, 5);
});

const progressMessage = computed(() => {
  return launchProgress.value?.map((e) => e.message).join(' // ') ?? 'Loading...';
});

const instanceName = computed(() => {
  if (!instance.value) {
    return "Unknown";
  }

  if (instance.value.name.endsWith(instance.value.version)) {
    return instance.value.name;
  }

  return `${instance.value.name} (${instance.value.version})`;
})

const messageTypes = {
  "W": "text-orange-400",
  "I": "text-blue-400",
  "E": "text-red-400",
  "D": "text-purple-400",
  "T": "text-green-400",
}

const launchStatus = computed(() => {
  if (hasCrashed.value) {
    return '%s has crashed! 🔥';
  }

  if (!finishedLoading.value) {
    return 'Starting %s';
  }

  return 'Running %s';
});

const logMessages = computed(() => {
  if (enabledLogTypes.value.length === 0) {
    return [{
      i: 0,
      t: "I",
      v: "No logs to display"
    }];
  }

  // Don't filter. 
  if (enabledLogTypes.value.length === logTypes.value.length) {
    return [...messages.value];
  }

  return [...messages.value].filter((e) => includeLog(e.t));
});

function includeLog(type: string) {
  switch (type) {
    case "I": return enabledLogTypes.value.includes("Info");
    case "W": return enabledLogTypes.value.includes("Warn");
    case "E": return enabledLogTypes.value.includes("Error");
    case "D": return enabledLogTypes.value.includes("Debug");
    case "T": return enabledLogTypes.value.includes("Trace");
    default: return enabledLogTypes.value.includes("Info");
  }
}

function toggleEnabledLog(type: string) {
  if (enabledLogTypes.value.includes(type)) {
    enabledLogTypes.value = enabledLogTypes.value.filter((e) => e !== type);
  } else {
    enabledLogTypes.value.push(type);
  }

  logsKey.value = enabledLogTypes.value.join("|");
  localStorage.setItem("enabledLogTypes", enabledLogTypes.value.join(","));
}

function showContextMenu(event: MouseEvent) {
  event.preventDefault()
  event.stopPropagation();
  
  AppContextController.openMenu(ContextMenus.RUNNING_INSTANCE_OPTIONS_MENU, event, () => ({ instance: instance.value }));
}
</script>

<template>
  <div class="pack-loading">
    <header class="flex">
      <img v-if="instance" :src="artworkFileOrElse(instance)" class="art rounded-2xl shadow mr-8" width="135" alt="" />

      <div class="body flex-1">
        <h3 class="text-xl font-bold mb-2">
          {{ runningInstance ? launchStatus.replace('%s', instanceName) : (instanceName + " has stopped") }}
        </h3>
        <p v-if="runningInstance && finishedLoading && !hasCrashed">
          <i class="italic">{{ instanceName }}</i> running
        </p>
        <template v-if="!hasCrashed && !finishedLoading">
          <template v-if="!finishedLoading && runningInstance">
            <div class="loading-area" v-if="instance !== null">
              <div
                class="progress-container"
                :aria-label="`Starting ${instanceName}... this might take a few minutes`"
                data-balloon-pos="up"
              >
                <progress-bar class="mt-6 mb-4" :progress="bars && bars[0] ? bars[0].step / bars[0].steps : 0" />
              </div>
              <div class="mb-2 flex items-center text-sm">
                <div
                  class="progress-spinner"
                  aria-label="If this takes more than 5 minutes, kill the instance and try again."
                  data-balloon-pos="down-left"
                >
                  <FontAwesomeIcon spin :icon="faCircleNotch" class="mr-4" />
                </div>
                {{ progressMessage }}
              </div>
            </div>
          </template>
          <div v-else class="flex mt-4 gap-4">
            <UiButton
              @click="openFolder"
              type="info"
            >
              <FontAwesomeIcon :icon="faFolderOpen" class="mr-2" />
              Open instance folder
            </UiButton>

            <router-link :to="{ name: RouterNames.ROOT_LIBRARY }">
              <UiButton
                v-if="!runningInstance"
                type="info"
              >
                <FontAwesomeIcon :icon="faArrowLeft" class="mr-2" />
                Back to library
              </UiButton>
            </router-link>
            
            <UiButton
              v-if="runningInstance"
              @click="cancelLoading"
              type="danger"
            >
              <FontAwesomeIcon :icon="faSkullCrossbones" class="mr-2" />
              Kill instance
            </UiButton>
          </div>
        </template>
        <template v-else-if="hasCrashed">
          <p>Looks like the instance has crashed during startup or whilst running...</p>
          <div class="flex mt-4">
            <UiButton
              @click="openFolder"
              class="transition ease-in-out duration-200 text-sm py-2 px-4 mr-4"
              color="info"
            >
              <FontAwesomeIcon :icon="faFolderOpen" class="mr-2" />
              Open instance folder
            </UiButton>
            <UiButton
              @click="leavePage"
              class="transition ease-in-out duration-200 text-sm py-2 px-4 mr-4 bg-red-600 hover:bg-red-700"
            >
              <FontAwesomeIcon :icon="faArrowLeft" class="mr-2" />
              Exit
            </UiButton>
          </div>
        </template>
      </div>
    </header>

    <div class="logs flex items-center">
      <h3 class="font-bold text-lg mr-6">Log</h3>
      
      <div class="flex text-sm gap-2 flex-1">
        <div v-for="(type, index) in logTypes" :key="index" @click="() => toggleEnabledLog(type)" class="border rounded px-1 border-gray-700 text-white opacity-50 transition-colors cursor-pointer duration-200" :class="{'bg-gray-700 text-white  opacity-100': enabledLogTypes.includes(type)}">
          {{ type }}
        </div>
      </div>
      
      <div class="buttons flex gap-4 items-center">
        <UiButton
          type="danger"
          size="small"
          v-if="runningInstance"
          @click="cancelLoading"
        >
          <FontAwesomeIcon :icon="faSkullCrossbones" class="mr-2" />
          Kill instance
        </UiButton>
        <UiButton
          type="info"
          size="small"
          class="!px-4"
          @click="showContextMenu"
        >
          <FontAwesomeIcon :icon="faEllipsisVertical" />
        </UiButton>
      </div>
    </div>
    
    <RecycleScroller
      :key="logsKey"
      id="log-container"
      :items="logMessages"
      key-field="i"
      :item-size="20" 
      class="select-text log-contents flex-1"
      list-class="log-contents-fixer"
      v-slot="{ item }"
      @scroll.native="userInteractedWithLogs"
    >
      <div class="log-item" :class="(messageTypes as any)[item.t]" :key="item.i">{{item.v}}</div>
    </RecycleScroller>
  </div>
</template>

<style lang="scss" scoped>
.pack-loading {
  display: flex;
  flex-direction: column;
  position: relative;
  height: 100%;
  max-height: 100%;
  z-index: 1;
  transition: 0.25s ease-in-out background-color;
  background-color: #1c1c1c;

  *::-webkit-scrollbar-corner {
    background-color: #1c1c1c;
    transition: 0.25s ease-in-out background-color;
  }

  > header {
    padding: 2rem;
    background-color: #2a2a2a;
  }

  .background {
    position: absolute;
    height: 200px;
    width: 100%;
    top: 0;
    left: 0;
    z-index: -1;

    &::before,
    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      z-index: -1;
    }

    &::before {
      opacity: 0.5;
      background: black;
    }

    &::after {
      background: linear-gradient(0deg, rgba(black, 1) 25%, rgba(black, 0) 100%);
    }
  }

  .logs {
    padding: 1rem;
    background-color: #1c1c1c;

    header {
      padding: 1rem 1rem 0 1rem;
    }
  }

  > .buttons {
    background-color: #1c1c1c;
    justify-content: flex-end;
    padding: 1rem 1rem 0 1rem;
  }

  .log-contents {
    padding: 1rem;
    font-family: 'JetBrains Mono', 'Consolas', 'Courier New', Courier, monospace;
    
    overflow-x: auto;
    white-space: nowrap;

    &::-webkit-scrollbar-track {
      background: transparent;
      z-index: 10;
    }

    &::-webkit-scrollbar {
      width: 8px;
      height: 8px;
      border-radius: 150px;
      z-index: 10;
    }

    &.wrap {
      .log-item {
        text-indent: -25px;
        padding-left: 25px;
        white-space: normal;
      }
    }

    .log-item {
      white-space: pre;
    }
  }

  .logs,
  .log-contents {
    transition: 0.25s ease-in-out background-color, 0.25s ease-in-out color;
    background-color: #1c1c1c;
    color: white;
  }
}

.action-categories {
  .category {
    .title {
      font-weight: bold;
      margin-bottom: .5rem;
    }
    
    &:not(:last-child) {
      margin-bottom: 1rem;
    }
    
    .actions {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
      padding: .5rem 0;
      
      .button {
        width: calc(50% - .5rem);
      }
      
      .button {
        padding: .2rem 0;
        
        &.looks-like-button {
          padding: .4rem 1rem;
          background-color: pink;
          
          &.danger {
            background-color: var(--color-danger-button);
          }

          &.warning {
            background-color: var(--color-warning-button);
          }
        }
        
        &:not(.looks-like-button):hover {
          transform: translateX(5px);
          color: #2ca2ff;
        }
      }
    }
  }
}

.update-bar {
  font-weight: 700;
  margin-bottom: 1rem;
}
</style>
