<template>
  <div id="app" class="theme-dark" :class="{'macos': isMac}">
    <title-bar />
    <div v-if="appReady && !appInstalled">
      <div class="app-container">
        <router-view />
      </div>
    </div>
    <template v-else>
      <div class="app-container" :class="{'no-system-bar': systemBarDisabled}" v-if="websockets.socket.isConnected && !loading">
        <main class="main">
          <sidebar v-if="showSidebar" />
          <div class="app-content relative">
            <router-view />
          </div>
          <ad-aside v-show="advertsEnabled" />
        </main>
      </div>
      <div class="app-container centered" :class="{'no-system-bar': !hasInitialized || (hasInitialized && systemBarDisabled)}" v-else>
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
    </template>

    <global-components />
  </div>
</template>

<script lang="ts">
import Sidebar from '@/components/layout/sidebar/Sidebar.vue';
import TitleBar from '@/components/layout/TitleBar.vue';
import {Component, Vue, Watch} from 'vue-property-decorator';
import {Action, Getter, State} from 'vuex-class';
import {SocketState} from '@/modules/websocket/types';
import {SettingsState} from '@/modules/settings/types';
import platfrom from '@/utils/interface/electron-overwolf';
import ReportForm from '@/components/templates/ReportForm.vue';
import AdAside from '@/components/layout/AdAside.vue';
import GlobalComponents from '@/components/templates/GlobalComponents.vue';
import {AuthState} from '@/modules/auth/types';
import {ns} from '@/core/state/appState';
import {AsyncFunction} from '@/core/@types/commonTypes';
import {requiresWsControllers} from '@/core/controllerRegistry';
import {createLogger} from '@/core/logger';
import {RouterNames} from '@/router';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import os from 'os';

@Component({
  components: {
    GlobalComponents,
    Sidebar,
    TitleBar,
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

  @Getter("ready", ns("v2/app")) appReady!: boolean
  @Getter("installed", ns("v2/app")) appInstalled!: boolean
  
  private loggger = createLogger(MainApp.name + ".vue");
  
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
    this.loggger.info("Mounted MainApp");
    if (this.$router.currentRoute.name !== RouterNames.ONBOARING && !this.appInstalled) {
      this.$router.push({
        name: RouterNames.ONBOARING
      })
      
      this.loggger.debug("Not ready, redirecting to onboarding")
    }
    
    const interval = setInterval(() => {
      // Waiting for the app to be ready
      if (this.appReady && this.appInstalled) {
        clearInterval(interval);
        this.startupApp();
      }
    }, 1000)
  }
  
  startupApp() {
    this.isMac = os.type() === 'Darwin';

    this.loggger.debug("Starting ping poll");
    this.registerPingCallback((data: any) => {
      if (data.type === 'ping') {
        gobbleError(() => sendMessage("pong", {}, 500))
      }
    });

    this.loggger.debug("Starting exit callback and setting up title bar");
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
  }

  destroyed() {
    if (this.pollRef) {
      clearInterval(this.pollRef);
    }
  }
  
  @Watch('websockets', { deep: true })
  public async onWebsocketsChange(newVal: SocketState, oldVal: SocketState) {
    if (newVal.socket.isConnected && this.loading) {
      this.loggger.info("Websockets connected, loading app");
      this.loading = false;
      await this.setupApp();
      this.loggger.info("Finished loading app");
    }

    if (!newVal.socket.isConnected && !this.loading) {
      this.loggger.warn("Websockets disconnected, unloading app");
      this.loggger.debug("Notifying all controllers of disconnected status")
      requiresWsControllers.forEach(e => e.onDisconnected());
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
    
    this.loggger.debug("Notifying all controllers of connected status")
    requiresWsControllers.forEach(e => e.onConnected());
  }

  public async fetchStartData() {
    this.loggger.info("Starting startup jobs");
    for (const job of this.startupJobs) {
      this.loggger.info(`Starting ${job.name}`)
      await job.action();
      
      // TODO: (M#01) FINISH THIS
      this.loggger.info(`Finished ${job.name}`)
      job.done = true;
    }
  }

  get showSidebar() {
    return !this.$route.path.startsWith('/settings');
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

  get systemBarDisabled() {
    return !this.settings.settings.useSystemWindowStyle ?? false;
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  height: 100%;
  position: relative;
  
  &.no-system-bar {
    height: calc(100% - 2rem);
  }

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
    height: 100%;

    &.no-system-bar {
      // Title bar on macos is 1.8rem not 2rem
      height: calc(100% - 1.8rem);
    }
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
</style>