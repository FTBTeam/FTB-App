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
            <div class="loading-area" v-if="currentModpack !== null">
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
                  <font-awesome-icon spin icon="circle-notch" class="mr-4" />
                </div>
                {{ progressMessage }}
              </div>
            </div>
          </template>
          <div v-else class="flex mt-4">
            <ftb-button
              @click="openFolder"
              class="transition ease-in-out duration-200 text-sm py-2 px-4 mr-4"
              color="primary"
            >
              <font-awesome-icon icon="folder-open" class="mr-2" />
              Open instance folder
            </ftb-button>

            <ftb-button
              @click="cancelLoading"
              class="transition ease-in-out duration-200 text-sm py-2 px-4 mr-4 bg-red-600 hover:bg-red-700"
            >
              <font-awesome-icon icon="skull-crossbones" class="mr-2" />
              Kill instance
            </ftb-button>
          </div>
        </template>
        <template v-else>
          <p>Looks like the instance has crashed during startup or whilst running...</p>
          <div class="flex mt-4">
<!--            <ftb-button-->
<!--              @click="launch"-->
<!--              color="primary"-->
<!--              class="transition ease-in-out duration-200 text-sm py-2 px-4 mr-4"-->
<!--            >-->
<!--              <font-awesome-icon icon="arrow-rotate-right" class="mr-2" />-->
<!--              Retry launch-->
<!--            </ftb-button>-->
            <ftb-button
              @click="openFolder"
              class="transition ease-in-out duration-200 text-sm py-2 px-4 mr-4"
              color="info"
            >
              <font-awesome-icon icon="folder-open" class="mr-2" />
              Open instance folder
            </ftb-button>
            <ftb-button
              @click="leavePage"
              class="transition ease-in-out duration-200 text-sm py-2 px-4 mr-4 bg-red-600 hover:bg-red-700"
            >
              <font-awesome-icon icon="arrow-left" class="mr-2" />
              Exit
            </ftb-button>
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
          <font-awesome-icon :icon="['fas', darkMode ? 'sun' : 'moon']" />
        </div>
        <ftb-button
          @click="showInstance"
          class="transition ease-in-out duration-200 ml-4 py-1 px-4 text-xs border-blue-600 border border-solid hover:bg-blue-600 hover:text-white"
          aria-label="Sometimes an instance can get stuck hidden in the background... You can use this to show the instance if it's not showing up after you think it's finished loading."
          data-balloon-pos="up-right"
          data-balloon-length="xlarge"
          v-if="!instance?.preventMetaModInjection"
        >
          <font-awesome-icon icon="eye" class="" />
        </ftb-button>
        <ftb-button
          @click="cancelLoading"
          class="transition ease-in-out duration-200 ml-4 py-1 px-4 text-xs border-red-600 border border-solid hover:bg-red-600 hover:text-white"
        >
          <font-awesome-icon icon="skull-crossbones" class="mr-2" />
          Kill instance
        </ftb-button>
        <ftb-button
          @click="showOptions = true"
          class="transition ease-in-out duration-200 ml-4 py-1 px-4 text-xs border-orange-600 border border-solid hover:bg-orange-600 hover:text-white"
        >
          <font-awesome-icon icon="ellipsis-vertical" />
        </ftb-button>
      </div>
    </div>
    
    <recycle-scroller
      id="log-container"
      :items="logMessages"
      key-field="i"
      :item-size="20" 
      class="select-text log-contents flex-1"
      list-class="log-contents-fixer"
      :class="{ 'dark-mode': darkMode }" v-slot="{ item }"
      @scroll.native="userInteractedWithLogs"
    >
      <div class="log-item" :class="messageTypes[item.t] + (!darkMode ? '-600': '-400')" :key="item.i">{{item.v}}</div>
    </recycle-scroller>
    
    <modal :open="showOptions" title="Instance options" :sub-title="instanceName" @closed="showOptions = false">
      <div class="action-categories">
        <div class="category" v-for="category in instanceActions">
          <div class="title">{{category.title}}</div>
          <div class="actions">
            <template v-for="action in category.actions">
              <ftb-button
                class="transition ease-in-out duration-200 button"
                :class="{[action.color ?? '']: action.color, 'looks-like-button': action.looksLikeButton}"
                v-if="!action.condition || (instance && action.condition({instance, instanceFolders}))"
                @click="runAction(action)"
              >
                <font-awesome-icon :icon="action.icon" class="mr-2" />
                {{ action.title }}
              </ftb-button>
            </template>
          </div>
        </div>
      </div>
    </modal>
  </div>
</template>

<script lang="ts">
import {Component, Vue, Watch} from 'vue-property-decorator';
import {ModPack} from '@/modules/modpacks/types';
import {Getter, State} from 'vuex-class';
import platform from '@/utils/interface/electron-overwolf';
import ProgressBar from '@/components/atoms/ProgressBar.vue';
import {SettingsState} from '@/modules/settings/types';
import {RouterNames} from '@/router';
import Router from 'vue-router';
import {ns} from '@/core/state/appState';
import {SugaredInstanceJson} from '@/core/@types/javaApi';
import {resolveArtwork} from '@/utils/helpers/packHelpers';
import {alertController} from '@/core/controllers/alertController';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {safeNavigate, waitForWebsocketsAndData} from '@/utils';
import {createLogger} from '@/core/logger';
import {SocketState} from '@/modules/websocket/types';
import {InstanceMessageData, InstanceRunningData} from '@/core/state/misc/runningState';

type InstanceActionCategory = {
  title: string;
  actions: InstanceAction[];
}

type InstanceAction = {
  title: string;
  icon: string;
  action: (instance: SugaredInstanceJson, router: Router) => void;
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
        await platform.get.io.openFinder(platform.get.io.pathJoin(instance.path, path))
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
            platform.get.io.openFinder(`${instance.path}`)
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

@Component({
  name: 'LaunchingPage',
  components: {
    ProgressBar,
  },
})
export default class RunningInstance extends Vue {
  @Getter('getInstance', ns("v2/instances")) getInstance!: (uuid: string) => SugaredInstanceJson | undefined;
  @State('websocket') public websockets!: SocketState;
  
  @Getter("getApiPack", ns("v2/modpacks")) getApiPack!: (id: number) => ModPack | undefined;
  @State('settings') public settingsState!: SettingsState;
  @State('instances', ns("v2/running")) runningInstancesData!: InstanceRunningData[]
  
  private logger = createLogger("LaunchingPage.vue");
  
  enabledLogTypes = ["Info", "Warn", "Error"];
  logTypes = ["Info", "Warn", "Error", "Debug", "Trace"];
  
  loading = false;
  platform = platform;

  instanceActions = instanceActions
  instanceFolders: string[] = [];
  
  hasCrashed = false;

  darkMode = true;
  disableFollow = false;

  currentStep = {
    desc: '',
    step: 0,
    totalSteps: 0,
    progress: 0,
    progressHuman: '',
  } as InstanceRunningData['startup']['step'];
  emptyCurrentStep = { ...this.currentStep };

  finishedLoading = false;
  preInitMessages: Set<string> = new Set();

  messages: InstanceMessageData[] = [];
  launchProgress: Bar[] | null | undefined = null;

  showOptions = false;
  
  scrollIntervalRef: any = null;

  public cancelLoading() {
    this.logger.debug("Attempting to kill instance")
    sendMessage("instance.kill", {
      uuid: this.instance?.uuid ?? ""
    }).catch((error) => {
      this.logger.error("Failed to kill instance", error)
    });
  }

  public async mounted() {
    this.loading = false;
    this.hasCrashed = false;
    this.currentStep = this.emptyCurrentStep;
    this.finishedLoading = false;
    this.preInitMessages = new Set();
    this.messages = [];
    this.launchProgress = null;
    
    if (localStorage.getItem("enabledLogTypes")) {
      this.enabledLogTypes = localStorage.getItem("enabledLogTypes")!.split(",");
    }
    
    this.logger.debug("Mounted Launch page, waiting for websockets...");
    await waitForWebsocketsAndData("Launch page", this.websockets.socket, () => {
      // This should get resolved quickly but it's possible it wont
      return this.instance != null;
    })

    this.logger.debug("Websockets ready, loading instance")
    
    if (this.instance == null) {
      alertController.error('Instance not found')
      this.logger.debug("Instance not found, redirecting to library", this.$route.params.uuid)
      await safeNavigate(RouterNames.ROOT_LIBRARY);
      return;
    }
    
    // Sync from any previous data
    const runningData = this.runningInstancesData.find(e => e.uuid === this.instance?.uuid)
    if (runningData) {
      this.syncDataFromRunningData(runningData)
    }
    
    sendMessage("getInstanceFolders", {
      uuid: this.instance.uuid
    })
      .then((e) => (this.instanceFolders = e.folders))
      .catch(e => this.logger.error("Failed to get instance folders", e))
    
    this.scrollIntervalRef = setInterval(this.scrollToBottom, 500);
  }
  
  @Watch('$route.params.uuid')
  async onRouteUpdate() {
    console.log("Route update")
    this.messages = [];
    this.$nextTick().then(() => {
      const runningData = this.runningInstancesData.find(e => e.uuid === this.instance?.uuid)
      if (runningData) {
        console.log("Sycning data")
        this.syncDataFromRunningData(runningData)
      } else {
        console.log("Instance not found, redirecting to library")
      }
    })
  }
  
  @Watch('runningInstancesData', {deep: true})
  async onRunningInstanceUpdate(newData: InstanceRunningData[]) {
    const runningData = newData.find((e) => e.uuid === this.instance?.uuid);
    if (!runningData) {
      await safeNavigate(RouterNames.ROOT_LIBRARY)
      return;
    }
    
    this.syncDataFromRunningData(runningData);
  }
  
  private syncDataFromRunningData(data: InstanceRunningData) {
    this.hasCrashed = data.status.crashed;
    this.finishedLoading = data.status.finishedLoading;
    this.launchProgress = data.startup.bars;
    this.messages = data.messages;
    this.preInitMessages = data.preInitMessages;
    this.currentStep = data.startup.step;
  }
  
  userInteractedWithLogs(event: any) {
    const location = event.target.scrollTop + event.target.clientHeight;
    
    // Give a 10px buffer
    this.disableFollow = location < event.target.scrollHeight - 5;
  }
  
  scrollToBottom() {
    if (this.disableFollow) {
      return;
    }

    const elm = document.getElementById('log-container');
    if (elm) {
      elm.scrollTop = elm.scrollHeight;
    }
  }

  leavePage() {
    if (this.instance) {
      this.$router.push({ name: RouterNames.ROOT_LOCAL_PACK, params: { uuid: this.instance?.uuid ?? "" } });
    } else {
      this.$router.push({ name: RouterNames.ROOT_LIBRARY });
    }
  }

  async showInstance() {
    await gobbleError(async () => {
      await sendMessage('messageClient', {
        uuid: this.instance?.uuid ?? "",
        message: 'show',
      })
    })
  }

  destroyed() {
    // Stop listening to events!
    clearInterval(this.scrollIntervalRef)
  }
  
  openFolder() {
    gobbleError(async () => {
      await this.platform.get.io.openFinder(`${this.instance?.path ?? ""}`)
    })
  }

  get instance() {
    return this.getInstance(this.$route.params.uuid as string) ?? null;
  }

  get bars() {
    if (this.launchProgress === null) {
      return [];
    }

    return this.launchProgress?.filter((b) => b.steps !== 1).slice(0, 5);
  }

  get progressMessage() {
    return this.launchProgress?.map((e) => e.message).join(' // ') ?? 'Loading...';
  }

  get instanceName() {
    if (!this.instance) {
      return "Unknown";
    }
    
    if (this.instance.name.endsWith(this.instance.version)) {
      return this.instance.name;
    }
    
    return `${this.instance.name} (${this.instance.version})`;
  }

  get currentModpack() {
    return this.instance;
  }

  messageTypes = {
    "W": "text-orange",
    "I": "text-blue",
    "E": "text-red",
    "D": "text-purple",
    "T": "text-green",
  };

  runAction(action: InstanceAction) {
    if (!this.instance) {
      return;
    }
    
    action.action(this.instance, this.$router);
    this.showOptions = false;
  }
  
  get launchStatus() {
    if (this.hasCrashed) {
      return '%s has crashed! 🔥';
    }

    if (!this.finishedLoading) {
      return 'Starting %s';
    }

    return 'Running %s';
  }
  
  get artLogo() {
    if (!this.instance) {
      return null;
    }
    
    return resolveArtwork(this.instance, "square", this.getApiPack(this.instance!.id) ?? null)
  }
  
  get logMessages() {
    if (this.enabledLogTypes.length === 0) {
      return [{
        i: 0,
        t: "I",
        v: "No logs to display"
      }];
    }
    
    // Don't filter. 
    if (this.enabledLogTypes.length === this.logTypes.length) {
      return this.messages;
    }

    return this.messages.filter((e) => this.includeLog(e.t));
  }
  
  includeLog(type: string) {
    switch (type) {
      case "I": return this.enabledLogTypes.includes("Info");
      case "W": return this.enabledLogTypes.includes("Warn");
      case "E": return this.enabledLogTypes.includes("Error");
      case "D": return this.enabledLogTypes.includes("Debug");
      case "T": return this.enabledLogTypes.includes("Trace");
      default: return this.enabledLogTypes.includes("Info");
    }
  }
  
  toggleEnabledLog(type: string) {
    if (this.enabledLogTypes.includes(type)) {
      this.enabledLogTypes = this.enabledLogTypes.filter((e) => e !== type);
    } else {
      this.enabledLogTypes.push(type);
    }
    
    localStorage.setItem("enabledLogTypes", this.enabledLogTypes.join(","));
  }
}
</script>

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
    transition: 0.25s ease-in-out background-color, color 0.25s ease-in-out;
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
