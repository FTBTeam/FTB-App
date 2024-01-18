<template>
  <div id="app" class="theme-dark" :class="{'macos': isMac}">
    <title-bar />
    
    <div class="app-container" v-if="!appReadyToGo">
      Not ready
      <pre>App Ready: {{appReadyToGo}}
App Starting: {{appStarting}}
App Loaded: {{appLoaded}}
App Installing: {{appInstalling}}
App Installing Stage: {{appInstallStage}}
App Connecting: {{appConnecting}}
App Reconnecting: {{isReconnecting}}
      </pre>
      
      <div class="initializing" v-if="appStarting && !appInstalling">
        Starting up
      </div>
  
      <!-- App trying to connect to subprocess -->
      <div class="installing" v-if="appInstalling">
        Installing {{appInstallStage}}
      </div>
      
      <div class="connecting" v-if="appConnecting && !appInstalling">
        Connecting {{reconnectAttempts}}
      </div>
      
      <!-- App has connected but is trying to reconnect -->
      <div class="reconnecting" v-if="!appStarting && !appInstalling && isReconnecting">
        Reconnecting
      </div>

      <div class="progress">
        <div class="bar"></div>
      </div>
    </div>
    
    <!-- App has connected and is ready to use -->
    <div class="app-container" v-if="appReadyToGo" :class="{'no-system-bar': systemBarDisabled}">
      <main class="main">
        <sidebar v-if="showSidebar" />
        <div class="app-content relative">
          <router-view />
        </div>
        <ad-aside v-show="advertsEnabled" />
      </main>
    </div>
    
    <global-components v-if="appReadyToGo" />
<!--      <div class="app-container centered" :class="{'no-system-bar': !hasInitialized || (hasInitialized && systemBarDisabled)}" v-else>-->
<!--        <div class="pushed-content">-->
<!--          <report-form-->
<!--            v-if="websockets.reconnects > 10 && this.loading"-->
<!--            :loadingFailed="loading"-->
<!--            :websocketsFailed="!websockets || websockets.reconnects > 10"-->
<!--            :websockets="websockets"-->
<!--            :max-tries="10"-->
<!--          />-->
<!--          <div-->
<!--            class="container flex pt-1 flex-wrap overflow-x-auto justify-center flex-col"-->
<!--            style="flex-direction: column; justify-content: center; align-items: center"-->
<!--            v-else-->
<!--          >-->
<!--            <img src="../assets/images/ftb-logo-full.svg" width="300" class="loader-logo-animation" />-->
<!--            <div class="progress">-->
<!--              <div class="bar"></div>-->
<!--            </div>-->
<!--            <em class="mt-6">{{ stage }}</em>-->
<!--          </div>-->
<!--        </div>-->
<!--      </div>-->
  </div>
</template>

<script lang="ts">
import Sidebar from '@/components/layout/sidebar/Sidebar.vue';
import TitleBar from '@/components/layout/TitleBar.vue';
import {Component, Vue, Watch} from 'vue-property-decorator';
import {Action, Getter, State} from 'vuex-class';
import {SocketState} from '@/modules/websocket/types';
import {SettingsState} from '@/modules/settings/types';
import platform from '@/utils/interface/electron-overwolf';
import ReportForm from '@/components/templates/ReportForm.vue';
import AdAside from '@/components/layout/AdAside.vue';
import GlobalComponents from '@/components/templates/GlobalComponents.vue';
import {AuthState} from '@/modules/auth/types';
import {ns} from '@/core/state/appState';
import {AsyncFunction} from '@/core/@types/commonTypes';
import {requiresWsControllers} from '@/core/controllerRegistry';
import {createLogger} from '@/core/logger';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import os from 'os';
import {constants} from '@/core/constants';

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
  @State('websocket') websockets!: SocketState;
  @State('settings') settings!: SettingsState;
  @State('auth') auth!: AuthState;
  @Action('loadSettings', { namespace: 'settings' }) loadSettings: any;
  @Action('registerPingCallback') registerPingCallback: any;
  @Action('loadProfiles', { namespace: 'core' }) loadProfiles!: AsyncFunction;
  @Action('loadInstances', ns("v2/instances")) loadInstances!: AsyncFunction;
  @Getter("getDebugDisabledAdAside", {namespace: 'core'}) debugDisabledAdAside!: boolean;
  
  @Action("storeWsSecret", ns("v2/app")) storeWsSecret!: (secret: string) => Promise<void>

  private logger = createLogger("MainApp.vue");
  
  // App runtime
  loading: boolean = true;
  platform = platform;
  hasInitialized = false;
  isMac: boolean = false;

  // App init
  appStarting = true;
  appLoaded = false;
  appInstalling = false;
  appInstallStage = "";

  startupJobs = [
    {
      name: "Settings",
      done: false,
      action: () => this.loadSettings()
    },
    {
      name: "Profiles",
      done: false,
      action: () => this.loadProfiles()
    },
    {
      name: "Loading Installed Instances",
      done: false,
      action: () => this.loadInstances()
    },
  ]

  allJobsDone() {
    return this.startupJobs.every(job => job.done)
  }
  
  async mounted() {
    this.isMac = os.type() === 'Darwin';

    this.appStarting = true;
    this.appLoaded = false;
    
    this.logger.info("Mounted MainApp");
    
    // Check if the app is installed
    let installed = true;
    if (!this.platform.isOverwolf()) {
      installed = await platform.get.app.runtimeAvailable();
      
      if (!installed) {
        // We need to install the app
        const result = await this.installApp();
        
        // TODO: Error check
      }
    }

    await this.startApp();
    this.appLoaded = true;
  }
  
  async installApp() {
    this.appInstalling = true;
    this.logger.info("Installing app");
    // TODO: Progress
    await this.platform.get.app.installApp((stage: any) => this.appInstallStage = stage, () => {
      console.log("Update")
    })
    this.appInstalling = false;
  }
  
  async startApp() {
    this.appStarting = false;
    this.logger.info("Starting app");
    
    if (!constants.isProduction) {
      this.logger.info("Starting production app");

      // If we're in dev, we just default to localhost
      await this.connectToWebsockets();
      return;
    }

    const appData: any = await platform.get.app.startSubprocess();
    this.logger.info("Started app subprocess", appData);
    
    // TODO: Error handling, maybe try again?
    
    // Finally we need to get the app to connect to the subprocess
    await this.connectToWebsockets(appData.port, appData.secret);
  }
  
  async connectToWebsockets(port: number = 13377, secret: string = "") {    
    // Store the secret
    this.logger.info("Starting websocket connection on port", port, "with secret", secret);
    await this.storeWsSecret(secret);
    this.$connect('ws://localhost:' + port);
  }

  async setupApp() {
    if (!this.hasInitialized) {
      await this.fetchStartData();
      
      this.logger.debug("Starting ping poll");
      this.registerPingCallback((data: any) => {
        if (data.type === 'ping') {
          gobbleError(() => sendMessage("pong", {}, 500))
        }
      });
      
      this.hasInitialized = true;
    }
    
    this.platform.get.actions.onAppReady();

    this.logger.debug("Notifying all controllers of connected status")
    requiresWsControllers.forEach(e => e.onConnected());
  }

  public async fetchStartData() {
    this.logger.info("Starting startup jobs");
    for (const job of this.startupJobs) {
      this.logger.info(`Starting ${job.name}`)
      await job.action();

      // TODO: (M#01) FINISH THIS
      this.logger.info(`Finished ${job.name}`)
      job.done = true;
    }
  }
  
  @Watch('websockets', { deep: true })
  public async onWebsocketsChange(newVal: SocketState, oldVal: SocketState) {
    if (newVal.socket.isConnected && this.loading) {
      this.logger.info("Websockets connected, loading app");
      this.loading = false;
      await this.setupApp();
      this.logger.info("Finished loading app");
    }

    if (!newVal.socket.isConnected && !this.loading) {
      this.logger.warn("Websockets disconnected, unloading app");
      this.logger.debug("Notifying all controllers of disconnected status")
      requiresWsControllers.forEach(e => e.onDisconnected());
      this.loading = true;
    }
  }
  
  get appConnecting() {
    return !this.websockets.socket.isConnected
  }
  
  get reconnectAttempts() {
    return this.websockets.reconnects
  }
  
  get isReconnecting() {
    return this.appConnecting && !this.websockets.firstStart
  }

  get appReadyToGo() {
    return !this.appStarting
      && this.appLoaded
      && !this.appInstalling
      && !this.isReconnecting
      && !this.appConnecting
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