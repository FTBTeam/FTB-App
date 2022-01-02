<template>
  <div class="pack-loading">
    <header class="flex">
      <img :src="artSquare" class="art rounded-2xl shadow mr-8" width="135" alt="" />

      <div class="body flex-1">
        <h3 class="text-xl font-bold mb-2">{{ preLaunch ? 'Initializing' : 'Starting' }} {{ instance.name }}</h3>
        <template v-if="preLaunch">
          <div
            class="progress-container"
            :aria-label="`Getting everything ready for ${instance.name}`"
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
        <template v-else>
          <div class="loading-area" v-if="currentModpack !== null">
            <div
              class="progress-container"
              :aria-label="`Starting ${instance.name}... this might take a few minutes`"
              data-balloon-pos="up"
            >
              <progress-bar class="mt-6 mb-4" :progress="bars[0] ? bars[0].step / bars[0].steps : 0" />
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
      </div>
    </header>

    <div class="logs flex justify-between items-center" :class="{ 'dark-mode': darkMode }">
      <h3 class="font-bold text-lg">Log</h3>
      <div class="buttons flex items-center ">
        <ftb-button
          @click="cancelLoading"
          class="transition ease-in-out duration-200 py-1 px-4 text-xs mr-4 border-red-600 border border-solid hover:bg-red-600 hover:text-white"
        >
          <font-awesome-icon icon="skull-crossbones" class="mr-2" />
          Kill instance
        </ftb-button>
        <ftb-button
          class="transition ease-in-out duration-200 text-xs border border-solid px-2 py-1 mr-4 hover:bg-green-600 hover:text-white hover:border-green-600"
          :class="{ 'border-black': !darkMode, 'border-white': darkMode }"
        >
          <font-awesome-icon icon="upload" class="mr-2" />
          Upload logs
        </ftb-button>
        <div class="color cursor-pointer" @click="darkMode = !darkMode">
          <font-awesome-icon :icon="['fas', darkMode ? 'sun' : 'moon']" />
        </div>
      </div>
    </div>

    <div class="log-contents text-sm" :class="{ 'dark-mode': darkMode }">
      <div class="log-item" v-for="n in 100">[debug][{{ n }}]: AHHHH</div>
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
  @Action('registerLaunchProgressCallback') public registerLaunchProgressCallback: any;

  loading = false;
  preLaunch = true;
  platform = platform;

  darkMode = true;

  currentStep = {
    stepDesc: '',
    step: 0,
    totalSteps: 0,
    stepProgress: 0,
    stepProgressHuman: '',
  };

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
        type: 'messageClient',
        uuid: this.instance?.uuid,
        message: 'yeet',
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
      return null;
    }

    this.registerLaunchProgressCallback((data: any) => {
      this.currentStep.stepDesc = data.stepDesc;
      this.currentStep.step = data.step;
      this.currentStep.totalSteps = data.totalSteps;
      this.currentStep.stepProgress = data.stepProgress;
      this.currentStep.stepProgressHuman = data.stepProgressHuman;
    });

    await this.fetchModpack(this.instance?.id);
    if (this.modpacks.packsCache[this.instance?.id] !== undefined) {
      this.loading = false;
    }

    // await this.launch();
  }

  public async launch(): Promise<void> {
    if (!(await preLaunchChecksValid())) {
      this.showAlert({
        title: 'Error!',
        message: 'Unable to update, validate or find your profile, please sign in again.',
        type: 'danger',
      });

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
          this.msgBox.title = 'An error occured whilst launching';
          this.msgBox.okAction = () => (this.showMsgBox = false);
          this.msgBox.content = data.message;
          this.showMsgBox = true;
        } else if (data.status === 'success') {
          this.preLaunch = false;
        }
      },
    });
  }

  get instance() {
    if (this.modpacks === null) {
      return null;
    }

    return this.modpacks.installedPacks.filter(pack => pack.uuid === this.$route.query.uuid)[0];
  }

  get limitedTags() {
    if (this.currentModpack && this.currentModpack.tags) {
      return this.currentModpack.tags.slice(0, 5);
    } else {
      return [];
    }
  }

  get bars() {
    console.log(this.modpacks);
    if (this.modpacks === undefined || this.modpacks === null) {
      return [];
    }
    if (this.modpacks.launchProgress === null) {
      return [];
    }

    return this.modpacks.launchProgress?.filter(b => b.steps !== 1).slice(0, 5);
  }

  get progressMessage() {
    return this.modpacks.launchProgress?.map(e => e.message).join(' // ') ?? 'Loading...';
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

    const arts = this.currentModpack.art.filter(art => art.type === 'splash');
    return arts.length > 0 ? arts[0].url : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png';
  }

  get artSquare() {
    if (!this.currentModpack?.art) {
      return 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png';
    }

    const arts = this.currentModpack.art.filter(art => art.type === 'square');
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

  > header {
    padding: 2rem;
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
    background-color: black;
    flex-direction: column-reverse;
    padding: 1rem;
    overflow: auto;
    font-family: 'Consolas', 'Courier New', Courier, monospace;

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
