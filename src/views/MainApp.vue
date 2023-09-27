<template>
  <div id="app" class="theme-dark" :class="{'macos': isMac}">
    <title-bar :is-dev="isDev" />
    <div class="app-container" v-if="websockets.socket.isConnected && !loading">
      <main class="main">
        <sidebar :is-dev="isDev" v-if="showSidebar" />
        <div class="app-content relative">
          <router-view />
        </div>
        <ad-aside v-show="advertsEnabled" :is-dev="isDev" />
      </main>
    </div>
    <div class="app-container centered" v-else>
      <div class="pushed-content">
        <report-form
          v-if="websockets.reconnects > 10 && this.loading"
          :loadingFailed="loading"
          :websocketsFailed="!websockets || websockets.reconnects > 10"
          :websockets="websockets"
          :max-tries="10"
        />
        <div
          class="container flex pt-1 flex-wrap overflow-x-auto justify-center flex-col"
          style="flex-direction: column; justify-content: center; align-items: center"
          v-else
        >
          <img src="../assets/images/ftb-logo-full.svg" width="300" class="loader-logo-animation" />
          <div class="progress">
            <div class="bar"></div>
          </div>
          <em class="mt-6">{{ stage }}</em>
        </div>
      </div>
    </div>

    <global-components />
  </div>
</template>

<script lang="ts">
import Sidebar from '@/components/layout/sidebar/Sidebar.vue';
import TitleBar from '@/components/layout/TitleBar.vue';
import {Component, Vue, Watch} from 'vue-property-decorator';
import {Action, Getter, State} from 'vuex-class';
import {SocketState} from '@/modules/websocket/types';
import FTBModal from '@/components/atoms/FTBModal.vue';
import {SettingsState} from '@/modules/settings/types';
import platfrom from '@/utils/interface/electron-overwolf';
import ReportForm from '@/components/templates/ReportForm.vue';
import AdAside from '@/components/layout/AdAside.vue';
import GlobalComponents from '@/components/templates/GlobalComponents.vue';
import {RouterNames} from '@/router';
import {AuthState} from '@/modules/auth/types';
import {AppStoreModules, ns} from '@/core/state/appState';
import {AsyncFunction} from '@/core/@types/commonTypes';
import { sendMessage } from '@/core/websockets/websocketsApi';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import os from 'os';

@Component({
  components: {
    GlobalComponents,
    Sidebar,
    TitleBar,
    FTBModal,
    ReportForm,
    AdAside,
  },
})
export default class MainApp extends Vue {
  @State('websocket') public websockets!: SocketState;
  @State('settings') public settings!: SettingsState;
  @State('auth') public auth!: AuthState;
  @Action('loadSettings', { namespace: 'settings' }) public loadSettings: any;
  @Action('saveSettings', { namespace: 'settings' }) private saveSettings!: any;
  @Action('disconnect') public disconnect: any;
  private loading: boolean = true;

  @Action('registerExitCallback') private registerExitCallback: any;
  @Action('registerPingCallback') private registerPingCallback: any;

  @Action('loadProfiles', { namespace: 'core' }) private loadProfiles!: AsyncFunction;
  @Action('loadInstances', ns("v2/instances")) private loadInstances!: AsyncFunction;
  
  @Getter("getDebugDisabledAdAside", {namespace: 'core'}) private debugDisabledAdAside!: boolean

  private platfrom = platfrom;
  private windowId: string | null = null;

  stage = 'Setting up...';
  hasInitialized = false;

  public isMac: boolean = false;
  
  startupJobs = [
    {
      "name": "Settings",
      "done": false,
      "action": () => this.loadSettings()
    },
    {
      "name": "Profiles",
      "done": false,
      "action": () => this.loadProfiles()
    },
    {
      "name": "MineTogether",
      "done": false,
      "action": () => {}
    },
    {
      "name": "Loading Installed Instances",
      "done": false,
      "action": () => this.loadInstances()
    },
  ]
  
  allJobsDone() {
    return this.startupJobs.every(job => job.done)
  }

  private pollRef: number | null = null;
  
  public mounted() {
    this.isMac = os.type() === 'Darwin';
    
    this.registerPingCallback((data: any) => {
      if (data.type === 'ping') {
        gobbleError(() => sendMessage("pong", {}, 500))
      }
    });

    this.platfrom.get.frame.setupTitleBar((windowId) => (this.windowId = windowId));

    // Only used on overwolf.
    this.registerExitCallback((data: any) => {
      if (data.type === 'yeetLauncher') {
        this.platfrom.get.actions.yeetLauncher(this.windowId, () => {
          this.saveSettings(this.settings?.settings);
          this.disconnect();
        });
      }
    });

    // this.pollRef = setInterval(() => {
    //   //Poll cloud saves
    //   console.log("pollCloudInstances")
    //   sendMessage("pollCloudInstances", {})
    //     .then(e => console.log("pollCloudInstances", e))
    //     .catch(e => console.error("pollCloudInstances", e))
    // }, 5000) as any
  }

  destroyed() {
    if (this.pollRef) {
      clearInterval(this.pollRef);
    }
  }
  
  @Watch('websockets', { deep: true })
  public async onWebsocketsChange(newVal: SocketState, oldVal: SocketState) {
    if (newVal.socket.isConnected && this.loading) {
      this.loading = false;
      await this.setupApp();
    }

    if (!newVal.socket.isConnected && !this.loading) {
      this.loading = true;
      this.stage = 'Attempting to reconnect to the apps agent...';
    }
  }

  async setupApp() {
    if (!this.hasInitialized) {
      await this.fetchStartData();
      this.hasInitialized = true;
    }
    this.platfrom.get.actions.onAppReady();
  }

  public async fetchStartData() {
    for (const job of this.startupJobs) {
      await job.action();
      console.log(`Finished ${job.name}`)
      job.done = true;
    }
  }

  get showSidebar() {
    return !this.$route.path.startsWith('/settings');
  }

  get isDev() {
    return this.$route.name === RouterNames.DEVELOPER;
  }

  get advertsEnabled(): boolean {
    if (process.env.NODE_ENV !== "production" && this.debugDisabledAdAside) {
      return false
    }
    
    if (!this.auth?.token?.activePlan) {
      return true;
    }

    // If this fails, show the ads
    return (this.settings?.settings?.showAdverts === true || this.settings?.settings?.showAdverts === 'true') ?? true;
  }
}
</script>

<style lang="scss" scoped>
.progress {
  margin-top: 4rem;
  width: 350px;
  height: 10px;
  background: rgba(gray, 0.2);
  border-radius: 10px;
  overflow: hidden;
  position: relative;

  .bar {
    width: 100%;
    height: 100%;
    background: var(--color-primary-button);
    position: absolute;
    left: -100%;

    animation: leftToRight 1.5s ease-in-out infinite;

    @keyframes leftToRight {
      0% {
        left: -100%;
      }
      50% {
        left: 0;
      }
      100% {
        left: 100%;
      }
    }
  }
}

.bottom-area {
  position: absolute;
  width: 100%;
  left: 0;
  bottom: 0;
}
</style>

<style lang="scss">
.app-container {
  height: calc(100% - 2rem);
  position: relative;

  &.centered {
    display: flex;
    align-items: center;
    justify-content: center;

    .pushed-content {
      margin-top: -5rem;
    }
  }
}

#app.macos {
  .app-container {
    // Title bar on macos is 1.8rem not 2rem
    height: calc(100% - 1.8rem);
  }
}

main.main {
  position: relative;
  z-index: 1;
  display: flex;
  height: 100%;
}

.app-content {
  flex: 1;
  min-height: 100%;
  overflow-y: auto;
}

.progress-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  bottom: 0;
  z-index: 4;
  width: 100%;
  background-color: var(--color-navbar);
  height: 40px;
  max-height: 40px;
}
.alert-close {
  position: fixed;
  stroke: #fff;
  right: 10px;
}
.slide-down-up-enter-active {
  transition: all 0.5s ease;
}
.slide-down-up-leave-active {
  transition: all 0.5s ease;
}
.slide-down-up-enter, .slide-leave-to
/* .slide-fade-leave-active below version 2.1.8 */ {
  max-height: 0;
}
.loader-logo-animation {
  animation-name: saturation;
  animation-duration: 1.8s;
  animation-iteration-count: infinite;
  animation-direction: alternate;
}
@keyframes saturation {
  from {
    filter: saturate(0);
  }
  to {
    filter: saturate(1);
  }
}
</style>
