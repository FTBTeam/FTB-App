<template>
  <div class="pack-loading" :class="{ 'dark-mode': darkMode }">
    <header class="flex">
      <img :src="artSquare" class="art rounded-2xl shadow mr-8" width="135" alt="" />

      <div class="body flex-1">
        <h3 class="text-xl font-bold mb-2">
          {{ !finishedLoading ? (preLaunch ? 'Initializing' : 'Starting') : 'Running' }} {{ instanceName }}
        </h3>
        <p v-if="finishedLoading">
          {{ instanceName }} should be running! Enjoy <font-awesome-icon class="ml-2" icon="thumbs-up" />
        </p>
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
          @click="cancelLoading"
          class="transition ease-in-out duration-200 ml-4 py-1 px-4 text-xs border-red-600 border border-solid hover:bg-red-600 hover:text-white"
        >
          <font-awesome-icon icon="skull-crossbones" class="mr-2" />
          Kill instance
        </ftb-button>
      </div>
    </div>

    <div class="log-contents text-sm" :class="{ 'dark-mode': darkMode, wrap: wrapText }">
      <div class="log-item" v-for="i in messages.length" :key="i">
        {{ messages[messages.length - i] }}
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
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import { ModpackState } from '@/modules/modpacks/types';
import { Action, State } from 'vuex-class';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import MessageModal from '@/components/organisms/modals/MessageModal.vue';
import FTBModal from '@/components/atoms/FTBModal.vue';
import ServerCard from '@/components/organisms/ServerCard.vue';
import InstallModal from '@/components/organisms/modals/InstallModal.vue';
import platform from '@/utils/interface/electron-overwolf';
import ProgressBar from '@/components/atoms/ProgressBar.vue';
import { preLaunchChecksValid } from '@/utils/auth/authentication';
import { SettingsState } from '@/modules/settings/types';
import { AuthState } from '@/modules/auth/types';
import { MsgBox } from '@/components/organisms/packs/PackCard.vue';
import eventBus from '@/utils/event-bus';
import { RouterNames } from '@/router';

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
    ServerCard,
    ProgressBar,
  },
})
export default class LaunchingPage extends Vue {
  @State('modpacks') public modpacks!: ModpackState;
  @Action('fetchModpack', { namespace: 'modpacks' }) public fetchModpack!: any;
  @Action('sendMessage') public sendMessage!: any;
  @Action('showAlert') public showAlert: any;
  @State('settings') public settingsState!: SettingsState;
  @State('auth') public auth!: AuthState;

  loading = false;
  preLaunch = true;
  platform = platform;

  darkMode = true;
  wrapText = true;

  currentStep = {
    stepDesc: '',
    step: 0,
    totalSteps: 0,
    stepProgress: 0,
    stepProgressHuman: '',
  };

  finishedLoading = false;
  preInitMessages: Set<string> = new Set();

  messages: string[] = [];
  launchProgress: Bar[] | null | undefined = null;

  private showMsgBox = false;
  private msgBox: MsgBox = {
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
        content: 'Instance not found',
        type: 'error',
      });

      await this.$router.push(RouterNames.ROOT_LIBRARY);
      return;
    }

    eventBus.$on('ws.message', (data: any) => {
      if (data.type === 'launchInstance.logs') {
        this.handleLogMessages(data);
      }

      if (data.type === 'launchInstance.status') {
        this.handleInstanceLaunch(data);
      }

      if (data.type === 'clientLaunchData') {
        this.handleClientLaunch(data);
      }

      if (data.type === 'launchInstance.stopped') {
        this.$router.push({ name: RouterNames.ROOT_LIBRARY });
      }
    });

    await this.fetchModpack(this.instance?.id);
    if (this.modpacks.packsCache[this.instance?.id] !== undefined) {
      this.loading = false;
    }

    await this.launch();
  }

  destroyed() {
    // Stop listening to events!
    eventBus.$off('ws.message');
  }

  handleLogMessages(data: any) {
    for (const e of data.messages) {
      this.messages.push(e);
    }

    if (this.messages.length > 500) {
      // Remove the first 400 items of the array so there isn't a huge jump in the UI
      this.messages.splice(0, 400);
      this.messages.push(`[FTB APP][INFO] Cleaning up last 500 messages...`);
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
      this.messages.push('[FTB APP][INFO] ' + data.stepDesc);
    }
  }

  handleClientLaunch(data: any) {
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
    if (!(await preLaunchChecksValid(this.instance?.uuid))) {
      this.showAlert({
        title: 'Error!',
        message: 'Unable to update, validate or find your profile, please sign in again.',
        type: 'danger',
      });

      await this.$router.push(RouterNames.ROOT_LIBRARY);

      return;
    }

    const loadInApp =
      this.settingsState.settings.loadInApp === true ||
      this.settingsState.settings.loadInApp === 'true' ||
      this.auth.token?.activePlan == null;

    const disableChat = this.settingsState.settings.enableChat;
    this.preLaunch = true;

    this.sendMessage({
      payload: {
        type: 'launchInstance',
        uuid: this.instance?.uuid,
        loadInApp,
        extraArgs: disableChat ? '-Dmt.disablechat=true' : '',
      },
      callback: (data: any) => {
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
    if (this.modpacks === null) {
      return null;
    }

    return this.modpacks.installedPacks.filter((pack) => pack.uuid === this.$route.query.uuid)[0];
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
    return this.instance?.name ?? 'Unknown';
  }

  get currentModpack() {
    if (this.instance == null) {
      return null;
    }
    const id: number = this.instance.id;
    if (this.modpacks.packsCache[id] === undefined) {
      return null;
    }
    return this.modpacks.packsCache[id];
  }

  get art() {
    if (!this.currentModpack?.art) {
      return 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png';
    }

    const arts = this.currentModpack.art.filter((art) => art.type === 'splash');
    return arts.length > 0 ? arts[0].url : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png';
  }

  get artSquare() {
    if (!this.currentModpack?.art) {
      return 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png';
    }

    const arts = this.currentModpack.art.filter((art) => art.type === 'square');
    return arts.length > 0 ? arts[0].url : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png';
  }
}
</script>

<style lang="scss">
.pack-loading {
  display: flex;
  flex-direction: column;
  position: relative;
  height: 100%;
  max-height: 100%;
  z-index: 1;
  transition: 0.25s ease-in-out background-color;
  background-color: white;

  *::-webkit-scrollbar-corner {
    background-color: white;
    transition: 0.25s ease-in-out background-color;
  }

  &.dark-mode {
    background-color: black;

    *::-webkit-scrollbar-corner {
      background-color: black;
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
    background-color: black;

    header {
      padding: 1rem 1rem 0 1rem;
    }
  }

  > .buttons {
    background-color: black;
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
    background-color: white;
    color: #24292e;
    &.dark-mode {
      background-color: black;
      color: white;
    }
  }
}

.update-bar {
  font-weight: 700;
  margin-bottom: 1rem;
}
</style>
