<template>
  <div class="pack-loading" :class="{ 'dark-mode': darkMode }">
    <header class="flex">
      <img v-if="artLogo" :src="artLogo" class="art rounded-2xl shadow mr-8" width="135" alt="" />

      <div class="body flex-1">
        <h3 class="text-xl font-bold mb-2">
          {{ launchStatus.replace('%s', instanceName) }}
        </h3>
        <p v-if="finishedLoading && !hasCrashed">
          {{ instanceName }} should be running! Enjoy <font-awesome-icon class="ml-2" icon="thumbs-up" />
        </p>
        <template v-if="!hasCrashed">
          <template v-if="preLaunch">
            <div
              class="progress-container"
              :aria-label="`Getting everything ready for ${instanceName}`"
              data-balloon-pos="up"
            >
              <ProgressBar class="mt-6 mb-4" :progress="currentStep.stepProgress" />
            </div>
            <div class="mb-2 text-sm flex items-center">
              <div
                class="progress-spinner"
                aria-label="If this takes more than 5 minutes, kill the instance and try again."
                data-balloon-pos="down-left"
              >
                <font-awesome-icon spin icon="circle-notch" class="mr-4" />
              </div>

              {{ currentStep.stepDesc ? currentStep.stepDesc : 'Initializing...' }}
            </div>
            <p class="mb-2 text-sm" v-if="currentStep.stepProgressHuman !== undefined">
              {{ currentStep.stepProgressHuman }}
            </p>
          </template>
          <template v-else-if="!finishedLoading">
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
            <ftb-button
              @click="launch"
              color="primary"
              class="transition ease-in-out duration-200 text-sm py-2 px-4 mr-4"
            >
              <font-awesome-icon icon="arrow-rotate-right" class="mr-2" />
              Retry launch
            </ftb-button>
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

    <div class="logs flex justify-between items-center" :class="{ 'dark-mode': darkMode }">
      <h3 class="font-bold text-lg">Log</h3>
      <div class="buttons flex items-center">
        <!--        <ftb-button-->
        <!--          class="transition ease-in-out duration-200 text-xs border border-solid px-2 py-1 mr-4 hover:bg-green-600 hover:text-white hover:border-green-600"-->
        <!--          :class="{ 'border-black': !darkMode, 'border-white': darkMode }"-->
        <!--        >-->
        <!--          <font-awesome-icon icon="upload" class="mr-2" />-->
        <!--          Upload logs-->
        <!--        </ftb-button>-->
        <div
          class="color cursor-pointer ml-4"
          :aria-label="wrapText ? 'Unwrap text' : 'Wrap text'"
          data-balloon-pos="down"
        >
          <font-awesome-icon @click="wrapText = !wrapText" :icon="!wrapText ? 'left-right' : 'right-long'" />
        </div>
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

    <div class="log-contents text-sm select-text" :class="{ 'dark-mode': darkMode, wrap: wrapText }">
      <div v-for="i in messages.length" :key="i" class="log-item" :class="messageTypes[messages[messages.length - i].t + (!darkMode ? '-LIGHT': '')]">
        {{ messages[messages.length - i].m }}
      </div>
    </div>

    <FTBModal :visible="showMsgBox" @dismiss-modal="showMsgBox = false" :dismissable="true">
      <message-modal
        :title="msgBox.title"
        :content="msgBox.content"
        :ok-action="msgBox.okAction"
        :cancel-action="msgBox.cancelAction"
        :type="msgBox.type"
      />
    </FTBModal>
    
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
import { Component, Vue } from 'vue-property-decorator';
import {ModPack} from '@/modules/modpacks/types';
import { Action, Getter, State } from 'vuex-class';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import MessageModal from '@/components/organisms/modals/MessageModal.vue';
import FTBModal from '@/components/atoms/FTBModal.vue';
import InstallModal from '@/components/organisms/modals/InstallModal.vue';
import platform from '@/utils/interface/electron-overwolf';
import ProgressBar from '@/components/atoms/ProgressBar.vue';
import { validateAuthenticationOrSignIn } from '@/utils/auth/authentication';
import { SettingsState } from '@/modules/settings/types';
import { AuthState } from '@/modules/auth/types';
import { emitter } from '@/utils/event-bus';
import { RouterNames } from '@/router';
import {wsTimeoutWrapper, wsTimeoutWrapperTyped, yeetError} from '@/utils';
import Router from 'vue-router';
import {ns} from '@/core/state/appState';
import {SugaredInstanceJson} from '@/core/@types/javaApi';
import {resolveArtwork, typeIdToProvider} from '@/utils/helpers/packHelpers';
import {GetModpack} from '@/core/state/modpacks/modpacksState';
import {App} from '@/types';

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
      wsTimeoutWrapper({
        type: 'instanceBrowse',
        uuid: instance.uuid,
        folder: path,
      });
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
          wsTimeoutWrapper({
            type: 'instanceBrowse',
            uuid: instance.uuid,
          });
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
          wsTimeoutWrapper({
            type: 'instance.kill',
            uuid: instance.uuid,
          });
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
    'ftb-toggle': FTBToggle,
    InstallModal,
    FTBModal,
    'message-modal': MessageModal,
    ProgressBar,
  },
})
export default class LaunchingPage extends Vue {
  @Getter('instances', ns("v2/instances")) instances!: SugaredInstanceJson[];
  @Getter('getInstance', ns("v2/instances")) getInstance!: (uuid: string) => SugaredInstanceJson | undefined;
  @Action("getModpack", ns("v2/modpacks")) getModpack!: GetModpack;
  
  @Getter("getApiPack", ns("v2/modpacks")) getApiPack!: (id: number) => ModPack | undefined;
  
  @Action('sendMessage') public sendMessage!: any;
  @Action('showAlert') public showAlert: any;
  @State('settings') public settingsState!: SettingsState;
  @State('auth') public auth!: AuthState;

  loading = false;
  preLaunch = true;
  platform = platform;

  instanceActions = instanceActions
  instanceFolders: string[] = [];
  
  hasCrashed = false;

  darkMode = true;
  wrapText = true;

  currentStep = {
    stepDesc: '',
    step: 0,
    totalSteps: 0,
    stepProgress: 0,
    stepProgressHuman: '',
  };
  emptyCurrentStep = { ...this.currentStep };

  finishedLoading = false;
  preInitMessages: Set<string> = new Set();

  messages: { t: string; m: string }[] = [];
  launchProgress: Bar[] | null | undefined = null;

  showOptions = false;
  
  private showMsgBox = false;
  private msgBox: App.MsgBox = {
    title: '',
    content: '',
    type: '',
    okAction: Function,
    cancelAction: Function,
  };

  public cancelLoading() {
    this.sendMessage({
      payload: {
        type: 'instance.kill',
        uuid: this.instance?.uuid,
      },
    });
  }

  public restoreLoading() {
    this.sendMessage({
      payload: {
        type: 'messageClient',
        uuid: this.instance?.uuid,
        message: 'show',
      },
    });
  }

  public async mounted() {
    if (this.instance == null) {
      this.showAlert({
        title: 'Error',
        message: 'Instance not found',
        type: 'danger',
      });

      await this.$router.push(RouterNames.ROOT_LIBRARY);
      return;
    }

    emitter.on('ws.message', this.onLaunchProgressUpdate);
    await this.getModpack({
      id: this.instance.id,
      provider: typeIdToProvider(this.instance.packType)
    });
    await this.launch();

    wsTimeoutWrapperTyped<any, { folders: string[] }>({ type: 'getInstanceFolders', uuid: this.instance.uuid })
      .then((e) => (this.instanceFolders = e.folders))
      .catch(console.log);
  }

  onLaunchProgressUpdate(data: any) {
    if (data.type === 'launchInstance.logs') {
      this.handleLogMessages(data);
    }

    if (data.type === 'launchInstance.status') {
      this.handleInstanceLaunch(data);
    }

    if (data.type === 'clientLaunchData') {
      this.handleClientLaunch(data);
    }

    if (
      data.type === 'launchInstance.stopped' ||
      (data.type === 'launchInstance.reply' && (data.status === 'abort' || data.status === 'error'))
    ) {
      // Lets assume we've crashed
      if (data.status === 'errored' || data.status === 'error') {
        this.showAlert({
          title: 'Instance failure',
          message:
            data.status === 'error'
              ? 'Unable to start pack... please see the instance logs...'
              : 'The instance has crashed or has been externally closed.',
          type: 'danger',
        });

        this.hasCrashed = true;
        return; // block the redirection
      }

      this.leavePage();
    }
  }

  leavePage() {
    if (this.instance) {
      this.$router.push({ name: RouterNames.ROOT_LOCAL_PACK, query: { uuid: this.instance?.uuid } });
    } else {
      this.$router.push({ name: RouterNames.ROOT_LIBRARY });
    }
  }

  async showInstance() {
    await yeetError(async () => {
      await wsTimeoutWrapper({
        type: 'messageClient',
        uuid: this.instance?.uuid,
        message: 'show',
      });
    })
  }

  destroyed() {
    // Stop listening to events!
    emitter.off('ws.message', this.onLaunchProgressUpdate);
  }
  
  async lazyLogChecker(messages: string[]) {
    if (this.finishedLoading) {
      return;
    }
    
    // Make an educated guess that when this shows, we're likely good to assume it's loaded
    for (const message of messages) {
      if (message.includes("Created:") && message.includes("minecraft:textures/atlas") && message.includes("TextureAtlas")) {
        this.finishedLoading = true;
      }
    }
  }

  handleLogMessages(data: any) {
    yeetError(() => this.lazyLogChecker(data.messages));
    for (const e of data.messages) {
      this.messages.push(this.formatMessage(e));
    }

    if (this.messages.length > 500) {
      // Remove the first 400 items of the array so there isn't a huge jump in the UI
      this.messages.splice(0, 400);
      this.messages.push({t: "INFO", m: `[FTB APP][INFO] Cleaning up last 500 messages...`});
    }
  }

  handleInstanceLaunch(data: any) {
    this.currentStep.stepDesc = data.stepDesc;
    this.currentStep.step = data.step;
    this.currentStep.totalSteps = data.totalSteps;
    this.currentStep.stepProgress = data.stepProgress;
    this.currentStep.stepProgressHuman = data.stepProgressHuman;

    if (!this.preInitMessages.has(data.stepDesc)) {
      this.preInitMessages.add(data.stepDesc);
      this.messages.push({t: "INFO", m: '[FTB APP][INFO] ' + data.stepDesc});
    }
  }

  handleClientLaunch(data: any) {
    console.log("Launch data", data);
    if (data.messageType === 'message') {
      this.launchProgress = data.message === 'init' ? [] : undefined;
    } else if (data.messageType === 'progress') {
      if (data.clientData.bars) {
        this.launchProgress = data.clientData.bars;
      }
    } else if (data.messageType === 'clientDisconnect' || (data.messageType === 'message' && data.message === 'done')) {
      this.finishedLoading = true;
    }
  }

  public async launch(): Promise<void> {
    // Reset everything (supports relaunching)
    this.loading = false;
    this.preLaunch = true;
    this.hasCrashed = false;
    this.currentStep = this.emptyCurrentStep;
    this.finishedLoading = false;
    this.preInitMessages = new Set();
    this.messages = [];
    this.launchProgress = null;

    if (!this.$route.query.offline) {
      const refreshResponse = await validateAuthenticationOrSignIn(this.instance?.uuid);
      if (!refreshResponse.ok && !refreshResponse.networkError) {
        if (!this.instance) {
          await this.$router.push({ name: RouterNames.ROOT_LIBRARY });
          return;
        }

        await this.$router.push({ name: RouterNames.ROOT_LOCAL_PACK, query: { uuid: this.instance?.uuid } });
        return;
      } else if (refreshResponse.networkError) {
        await this.$router.push({
          name: RouterNames.ROOT_LOCAL_PACK,
          query: { uuid: this.instance?.uuid, presentOffline: 'true' },
        });
        return;
      }
    }

    const disableChat = this.settingsState.settings.enableChat;
    this.preLaunch = true;

    this.sendMessage({
      payload: {
        type: 'launchInstance',
        uuid: this.instance?.uuid,
        extraArgs: disableChat ? '-Dmt.disablechat=true' : '',
        offline: this.$route.query.offline,
        offlineUsername: this.$route.query.username ?? 'FTB Player',
      },
      callback: (data: any) => {
        // TODO: Replace with something much better!
        if (data.status === 'error') {
          this.preLaunch = false;
          // An instance is already running
          this.msgBox.type = 'okOnly';
          this.msgBox.title = 'An error occurred whilst launching';
          this.msgBox.okAction = () => (this.showMsgBox = false);
          this.msgBox.content = data.message;
          this.showMsgBox = true;
        } else if (data.status === 'success') {
          this.preLaunch = false;
        }
      },
    });
  }

  openFolder() {
    this.sendMessage({
      payload: { type: 'instanceBrowse', uuid: this.instance?.uuid },
      callback: (data: any) => {},
    });
  }

  get instance() {
    return this.getInstance(this.$route.query.uuid as string) ?? null;
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
    return (this.instance?.name ?? 'Unknown') + ' ' + (this.instance?.version ?? '');
  }

  get currentModpack() {
    return this.instance;
    // if (this.instance == null) {
    //   return null;
    // }
    // const id: number = this.instance.id;
    // if (this.modpacks.packsCache[id] === undefined) {
    //   return null;
    // }
    // return this.modpacks.packsCache[id];
  }

  messageTypes = {
    "WARN": "text-orange-200",
    "INFO": "text-blue-200",
    "ERROR": "text-red-200",
    "WARN-LIGHT": "text-orange-700",
    "INFO-LIGHT": "text-blue-700",
    "ERROR-LIGHT": "text-red-700",
  };
  
  lastType = "INFO";
  formatMessage(message: string) {
    let type = this.lastType ?? "INFO";
    const result = /^\[[^\/]+\/([^\]]+)]/.exec(message);
    if (result !== null && result[1]) {
      type = result[1];
      this.lastType = type;
    }
    
    return {
      t: type,
      m: message
    };
  }

  runAction(action: InstanceAction) {
    if (!this.instance) {
      return;
    }
    
    action.action(this.instance, this.$router);
    this.showOptions = false;
  }
  
  // get artSquare() {
  //   if (!this.currentModpack?.art) {
  //     return 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png';
  //   }
  //
  //   const arts = this.currentModpack.art.filter((art) => art.type === 'square');
  //   return arts.length > 0 ? arts[0].url : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png';
  // }
    
  get launchStatus() {
    if (this.hasCrashed) {
      return '%s has crashed! 🔥';
    }

    if (!this.finishedLoading) {
      return this.preLaunch ? 'Initializing %s' : 'Starting %s';
    }

    return 'Running %s';
  }
  
  get artLogo() {
    if (!this.instance) {
      return null;
    }
    
    return resolveArtwork(this.instance, "square", this.getApiPack(this.instance!.id) ?? null)
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
    flex: 1;
    display: flex;
    flex-direction: column-reverse;
    padding: 1rem 1rem 1rem 0;
    overflow: auto;
    font-family: 'Consolas', 'Courier New', Courier, monospace;
    margin: 0 0.1rem 0.5rem 1rem;

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
      white-space: nowrap;
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
