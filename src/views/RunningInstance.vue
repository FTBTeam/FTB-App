<script lang="ts" setup>
import { FTBButton, ProgressBar, Modal } from '@/components/ui';
import Router, {RouterNames} from '@/router';
import {SugaredInstanceJson} from '@/core/types/javaApi';
import {resolveArtwork} from '@/utils/helpers/packHelpers';
import {alertController} from '@/core/controllers/alertController';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {safeNavigate} from '@/utils';
import {createLogger} from '@/core/logger';
import { RecycleScroller } from 'vue-virtual-scroller'
import platform from '@platform';
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import { InstanceMessageData, InstanceRunningData, useRunningInstancesStore } from '@/store/runningInstancesStore.ts';
import { useRouter } from 'vue-router';
import { useModpackStore } from '@/store/modpackStore.ts';
import { useInstanceStore } from '@/store/instancesStore.ts';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import {
  faArrowLeft,
  faCircleNotch,
  faEllipsisVertical,
  faEye,
  faFolderOpen,
  faSkullCrossbones,
} from '@fortawesome/free-solid-svg-icons';

type InstanceActionCategory = {
  title: string;
  actions: InstanceAction[];
}

type InstanceAction = {
  title: string;
  icon: string;
  action: (instance: SugaredInstanceJson, router: typeof Router) => void;
  condition?: (context: ConditionContext) => boolean;
  color?: string;
  looksLikeButton?: boolean;
}

type ConditionContext = {
  instance: SugaredInstanceJson;
  instanceFolders: string[];
}

function folderExists(path: string, folders: string[]) {
  if (path === '' || !folders.length) {
    return false;
  }

  return folders.findIndex((e) => e === path) !== -1;
}

function openFolderAction(name: string, path: string): InstanceAction {
  return {
    title: `${name}`,
    icon: "folder-open",
    condition: ({instanceFolders}) => folderExists(path, instanceFolders),
    action: (instance) => {
      gobbleError(async () => {
        await platform.io.openFinder(platform.io.pathJoin(instance.path, path))
      })
    }
  } 
}

const instanceActions: InstanceActionCategory[] = [
  {
    title: "Folders",
    actions: [
      {
        title: "Instance folder",
        icon: "folder-open",
        action: (instance) => {
          gobbleError(() => {
            platform.io.openFinder(`${instance.path}`)
          })
        }
      },
      openFolderAction("Logs", "logs"),
      openFolderAction("Crash Reports", "crash-reports"),
      openFolderAction("Backups", "backups"),
      openFolderAction("Worlds", "saves"),
      openFolderAction("Configs", "config"),
      openFolderAction("Mods", "mods"),
      openFolderAction("Resource packs", "resourcepacks"),
      openFolderAction("Shaders", "shaderpacks"),
      openFolderAction("Scripts", "scripts"),
      openFolderAction("KubeJS", "kubejs"),
    ]
  },
  {
    title: "Actions",
    actions: [
      {
        title: "Kill instance",
        icon: "skull-crossbones",
        color: "danger",
        action: (instance) => {
          gobbleError(() => {
            sendMessage("instance.kill", {
              uuid: instance.uuid
            })
          })
        },
        looksLikeButton: true
      }
    ]
  }
] 

export interface Bar {
  title: string;
  steps: number;
  step: number;
  message: string;
}

const logger = createLogger("LaunchingPage.vue");

const router = useRouter();
const modpackStore = useModpackStore();
const instanceStore = useInstanceStore();
const runningInstanceStore = useRunningInstancesStore();

const enabledLogTypes = ref(["Info", "Warn", "Error"]);
const logTypes = ref(["Info", "Warn", "Error", "Debug", "Trace"]);
const loading = ref(false);

const instanceFolders = ref<string[]>([]);
const hasCrashed = ref(false);
const darkMode = ref(true);
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
const showOptions = ref(false);
const scrollIntervalRef = ref<any>(null);

const instance = computed(() => instanceStore.instances.find(e => e.uuid === router.currentRoute.value.params.uuid) ?? null);

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
  }

  logger.debug("Mounted Launch page, waiting for websockets...");
  // TODO: fix me
  // await waitForWebsocketsAndData("Launch page", this.websockets.socket, () => {
  //   // This should get resolved quickly but it's possible it wont
  //   return this.instance != null;
  // })

  logger.debug("Websockets ready, loading instance")

  if (instance.value == null) {
    alertController.error('Instance not found')
    logger.debug("Instance not found, redirecting to library", router.currentRoute.value.params.uuid)
    await safeNavigate(RouterNames.ROOT_LIBRARY);
    return;
  }

  // Sync from any previous data
  const runningData = runningInstanceStore.instances.find(e => e.uuid === instance.value?.uuid)
  if (runningData) {
    syncDataFromRunningData(runningData)
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

watch(() => router.currentRoute.value.params.uuid, async (newValue) => {
  console.log("Route changed", newValue)
  messages.value = [];
  nextTick(() => {
    const runningData = runningInstanceStore.instances.find(e => e.uuid === instance.value?.uuid)
    if (runningData) {
      console.log("Sycning data")
      syncDataFromRunningData(runningData)
    } else {
      console.log("Instance not found, redirecting to library")
    }
  })
})

watch(() => runningInstanceStore.instances, async (newData) => {
  const runningData = newData.find((e) => e.uuid === instance.value?.uuid);
  if (!runningData) {
    await safeNavigate(RouterNames.ROOT_LIBRARY);
    return;
  }

  syncDataFromRunningData(runningData);
}, { deep: true });

function syncDataFromRunningData(data: InstanceRunningData) {
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

async function showInstance() {
  await gobbleError(async () => {
    await sendMessage('messageClient', {
      uuid: instance.value?.uuid ?? "",
      message: 'show',
    })
  })
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
  "W": "text-orange",
  "I": "text-blue",
  "E": "text-red",
  "D": "text-purple",
  "T": "text-green",
}

function runAction(action: InstanceAction) {
  if (!instance.value) {
    return;
  }

  action.action(instance.value, router);
  showOptions.value = false;
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

const artLogo = computed(async () => {
  if (!instance.value) {
    return null;
  }

  return resolveArtwork(instance.value, "square", await modpackStore.getModpack(instance.value.id) ?? null)
})

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
    return messages.value;
  }

  return messages.value.filter((e) => includeLog(e.t));
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

  localStorage.setItem("enabledLogTypes", enabledLogTypes.value.join(","));
}
</script>

<template>
  <div class="pack-loading" :class="{ 'dark-mode': darkMode }">
    <header class="flex">
      <img v-if="artLogo" :src="artLogo" class="art rounded-2xl shadow mr-8" width="135" alt="" />

      <div class="body flex-1">
        <h3 class="text-xl font-bold mb-2">
          {{ launchStatus.replace('%s', instanceName) }}
        </h3>
        <p v-if="finishedLoading && !hasCrashed">
          <i class="italic">{{ instanceName }}</i> running
        </p>
        <template v-if="!hasCrashed">
          <template v-if="!finishedLoading">
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
          <div v-else class="flex mt-4">
            <FTBButton
              @click="openFolder"
              class="transition ease-in-out duration-200 text-sm py-2 px-4 mr-4"
              color="primary"
            >
              <FontAwesomeIcon :icon="faFolderOpen" class="mr-2" />
              Open instance folder
            </FTBButton>

            <FTBButton
              @click="cancelLoading"
              class="transition ease-in-out duration-200 text-sm py-2 px-4 mr-4 bg-red-600 hover:bg-red-700"
            >
              <FontAwesomeIcon :icon="faSkullCrossbones" class="mr-2" />
              Kill instance
            </FTBButton>
          </div>
        </template>
        <template v-else>
          <p>Looks like the instance has crashed during startup or whilst running...</p>
          <div class="flex mt-4">
<!--            <FTBButton-->
<!--              @click="launch"-->
<!--              color="primary"-->
<!--              class="transition ease-in-out duration-200 text-sm py-2 px-4 mr-4"-->
<!--            >-->
<!--              <FontAwesomeIcon icon="arrow-rotate-right" class="mr-2" />-->
<!--              Retry launch-->
<!--            </FTBButton>-->
            <FTBButton
              @click="openFolder"
              class="transition ease-in-out duration-200 text-sm py-2 px-4 mr-4"
              color="info"
            >
              <FontAwesomeIcon :icon="faFolderOpen" class="mr-2" />
              Open instance folder
            </FTBButton>
            <FTBButton
              @click="leavePage"
              class="transition ease-in-out duration-200 text-sm py-2 px-4 mr-4 bg-red-600 hover:bg-red-700"
            >
              <FontAwesomeIcon :icon="faArrowLeft" class="mr-2" />
              Exit
            </FTBButton>
          </div>
        </template>
      </div>
    </header>

    <div class="logs flex items-center" :class="{ 'dark-mode': darkMode }">
      <h3 class="font-bold text-lg mr-6">Log</h3>
      
      <div class="flex text-sm gap-2 flex-1">
        <div v-for="(type, index) in logTypes" :key="index" @click="() => toggleEnabledLog(type)" class="border rounded px-1 border-gray-700 text-white opacity-50 transition-colors cursor-pointer duration-200" :class="{'bg-gray-700 text-white  opacity-100': enabledLogTypes.includes(type)}">
          {{ type }}
        </div>
      </div>
      
      <div class="buttons flex items-center">
        <div
          class="color cursor-pointer ml-4"
          :aria-label="darkMode ? 'Light mode' : 'Dark mode'"
          data-balloon-pos="down"
          @click="darkMode = !darkMode"
        >
          <FontAwesomeIcon :icon="['fas', darkMode ? 'sun' : 'moon']" />
        </div>
        <FTBButton
          @click="showInstance"
          class="transition ease-in-out duration-200 ml-4 py-1 px-4 text-xs border-blue-600 border border-solid hover:bg-blue-600 hover:text-white"
          aria-label="Sometimes an instance can get stuck hidden in the background... You can use this to show the instance if it's not showing up after you think it's finished loading."
          data-balloon-pos="up-right"
          data-balloon-length="xlarge"
          v-if="!instance?.preventMetaModInjection"
        >
          <FontAwesomeIcon :icon="faEye" class="" />
        </FTBButton>
        <FTBButton
          @click="cancelLoading"
          class="transition ease-in-out duration-200 ml-4 py-1 px-4 text-xs border-red-600 border border-solid hover:bg-red-600 hover:text-white"
        >
          <FontAwesomeIcon :icon="faSkullCrossbones" class="mr-2" />
          Kill instance
        </FTBButton>
        <FTBButton
          @click="showOptions = true"
          class="transition ease-in-out duration-200 ml-4 py-1 px-4 text-xs border-orange-600 border border-solid hover:bg-orange-600 hover:text-white"
        >
          <FontAwesomeIcon :icon="faEllipsisVertical" />
        </FTBButton>
      </div>
    </div>
    
    <RecycleScroller
      id="log-container"
      :items="logMessages"
      key-field="i"
      :item-size="20" 
      class="select-text log-contents flex-1"
      list-class="log-contents-fixer"
      :class="{ 'dark-mode': darkMode }" v-slot="{ item }"
      @scroll.native="userInteractedWithLogs"
    >
      <div class="log-item" :class="(messageTypes as any)[item.t] + (!darkMode ? '-600': '-400')" :key="item.i">{{item.v}}</div>
    </RecycleScroller>
    
    <Modal :open="showOptions" title="Instance options" :sub-title="instanceName" @closed="showOptions = false">
      <div class="action-categories">
        <div class="category" v-for="category in instanceActions">
          <div class="title">{{category.title}}</div>
          <div class="actions">
            <template v-for="action in category.actions">
              <FTBButton
                class="transition ease-in-out duration-200 button"
                :class="{[action.color ?? '']: action.color, 'looks-like-button': action.looksLikeButton}"
                v-if="!action.condition || (instance && action.condition({instance, instanceFolders}))"
                @click="runAction(action)"
              >
                <FontAwesomeIcon :icon="action.icon" class="mr-2" />
                {{ action.title }}
              </FTBButton>
            </template>
          </div>
        </div>
      </div>
    </Modal>
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
  background-color: #ececec;

  *::-webkit-scrollbar-corner {
    background-color: #ececec;
    transition: 0.25s ease-in-out background-color;
  }
  
  &.dark-mode {
    background-color: #1c1c1c;

    *::-webkit-scrollbar-corner {
      background-color: #1c1c1c;
    }
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
    font-size: 12px;
    
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
    background-color: #ececec;
    color: #24292e;
    &.dark-mode {
      background-color: #1c1c1c;
      color: white;
    }
  }
  
  &:not(.dark-mode) {
    .log-contents {
      font-weight: 600;
    }
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
