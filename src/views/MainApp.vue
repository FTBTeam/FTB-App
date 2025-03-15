<template>
  <div id="app" class="theme-dark" :class="{'macos': isMac}">
    <title-bar />
    
    <div class="app-container relative flex justify-center items-center" v-if="!appReadyToGo">
      <div class="text-center">
        <loader :title="status" sub-title="We're just getting some things ready... This shouldn't take long!" />
        
        <p v-if="appInstalling" class="text-center mt-2">
          {{appInstallStage}}
        </p>
        
        <div class="debug font-mono font-bold flex gap-6 items-center">
          <div class="debug-item" :class="{active: appReadyToGo}">R</div>
          <div class="debug-item" :class="{active: appStarting}">S</div>
          <div class="debug-item" :class="{active: appLoaded}">L</div>
          <div class="debug-item" :class="{active: appInstalling}">I</div>
          <div class="debug-item" :class="{active: appConnecting}">C</div>
          <div class="debug-item" :class="{active: isReconnecting}">RE</div>
          <div class="opacity-25">
            Attempts {{reconnectAttempts}}
          </div>
        </div>
      </div>
    </div>
    
    <!-- App has connected and is ready to use -->
    <div class="app-container" v-if="appReadyToGo">
      <main class="main">
        <sidebar v-if="showSidebar" />
        <div class="app-content relative">
          <router-view />
        </div>
        <ad-aside v-show="advertsEnabled" :hide-ads="showOnboarding" />
      </main>
    </div>
    
    <global-components v-if="appReadyToGo" />
    <onboarding v-if="showOnboarding" @accepted="showOnboarding = false" />
    
    <modal :open="appInstallFailed || appStartupFailed" title="Something's gone wrong!" sub-title="Looks like there might be a problem...">
      <p class="mb-1">{{appInstallError}}</p>
      
      <p>Sometimes these things happen, it could be a random failure, try restarting the app and if that doesn't work, try reaching out on our Discord (linked below)</p>
      <template #footer>
        <div class="flex gap-4 justify-end items-center">
          <ui-button size="small" type="danger" class="mr-auto" @click="() => {
            appInstallFailed = false
            appStartupFailed = false
          }">Let me in anyway...</ui-button>
          <ui-button type="info" :icon="['fab', 'discord']" @click="platform.get.utils.openUrl('https://go.ftb.team/ftb-app-support-discord')">Support</ui-button>
          <ui-button type="success" icon="power-off" @click="restartApp">Restart</ui-button>
        </div>
      </template>
    </modal>
  </div>
</template>

<script lang="ts">
import Sidebar from '@/components/layout/sidebar/Sidebar.vue';
import TitleBar from '@/components/layout/TitleBar.vue';
import {SocketState} from '@/modules/websocket/types';
import {SettingsState} from '@/modules/settings/types';
import platform from '@/utils/interface/electron-overwolf';
import AdAside from '@/components/layout/AdAside.vue';
import GlobalComponents from '@/components/layout/GlobalComponents.vue';
import {ns} from '@/core/state/appState';
import {AsyncFunction} from '@/core/@types/commonTypes';
import {createLogger} from '@/core/logger';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import os from 'os';
import {constants} from '@/core/constants';
import {adsEnabled, emitter} from '@/utils';
import Loader from '@/components/ui/Loader.vue';
import Onboarding from '@/components/modals/Onboarding.vue';
import UiButton from '@/components/ui/UiButton.vue';
import {alertController} from '@/core/controllers/alertController';

@Component({
  components: {
    UiButton,
    Onboarding,
    Loader,
    GlobalComponents,
    Sidebar,
    TitleBar,
    AdAside,
  },
})
export default class MainApp extends Vue {
  @State('websocket') websockets!: SocketState;
  @State('settings') settings!: SettingsState;
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
  
  appInstallFailed = false;
  appStartupFailed = false;
  appInstallError = "";

  showOnboarding = false;

  startupJobs = [
    {
      name: "Settings",
      done: false,
      action: () => this.loadSettings()
    },
    {
      name: "Init App",
      done: false,
      action: () => this.initApp()
    }
  ]
  
  postStartupJobs = [
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
    this.useSystemBar().catch(console.error)
    
    this.startNetworkMonitor();
    this.logger.info("App started on ", constants.platform)
    this.isMac = os.type() === 'Darwin';

    this.appStarting = true;
    this.appLoaded = false;
    
    this.logger.info("Mounted MainApp for ", constants.platform);
    
    // Check if the app is installed
    let installed = true;
    if (!this.platform.isOverwolf() && constants.isProduction) {
      installed = await platform.get.app.runtimeAvailable();
      
      this.logger.info("App installed", installed);
      if (!installed) {
        // We need to install the app
        try {
          await this.installApp();
        } catch (e: any) {
          this.appInstallFailed = true;
          this.appInstallError = e?.customMessage || "The app has failed to install the necessary files, please try again or contact support...";
          this.logger.error("Failed to install app", e);
        }
      }
    }

    await this.startApp();
    this.appLoaded = true;
    
    emitter.on("ws.message", (data: any) => {
      if (data?.type === "refreshInstancesRequest") {
        this.loadInstances();
      }
    })
  }
  
  async initApp() {
    this.logger.info("Initializing app");
    try {
      this.logger.debug("Sending appInit message");
      const reply = await sendMessage("appInit", {});
      this.logger.debug("Received appInit reply", reply);
      if (!reply?.success) {
        this.logger.error("Failed to initialize app", reply);
        return;
      }
      
      this.logger.info("App initialized from the subprocess");
    } catch (e) {
      console.error(e);
    }
  }

  async installApp() {
    this.appInstalling = true;
    this.logger.info("Installing app");
    
    await this.platform.get.app.installApp((stage: any) => this.appInstallStage = stage, () => {}, false)
    
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

    this.logger.info("Sending start subprocess message");
    try {
      let appData: any = await platform.get.app.startSubprocess();
      
      // False means update
      if (!appData) {
        try {
          await this.platform.get.app.updateApp(
            (stage: any) => this.appInstallStage = stage,
            () => this.logger.info("Update complete")
          );
        } catch (e: any) {
          this.appInstallFailed = true;
          this.appInstallError = e?.customMessage || "The app has failed to update, please try again or contact support...";
          this.logger.error("Failed to update app", e);
        }
        
        appData = await platform.get.app.startSubprocess();
      }
      
      this.logger.info("Started app subprocess", appData);
      if (!appData.port || !appData.secret) {
        this.appStartupFailed = true;
        this.appInstallError = "The app has failed to start due to connection issues, please try again or contact support...";
        this.logger.error("Failed to start subprocess due to connection issues", appData);
        return;
      }

      // Finally we need to get the app to connect to the subprocess
      await this.connectToWebsockets(appData.port, appData.secret);
    } catch (e: any) {
      // Same as the install, grab the custom error if possible
      this.appStartupFailed = true;
      this.appInstallError = e?.customMessage || "The app has failed to start, please try again or contact support...";
      this.logger.error("Failed to start subprocess", e);
    }
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

      this.checkForFirstRun()
        .then(() => this.logger.info("Finished checking for first run"))
        .catch(error => this.logger.error("Failed to check for first run", error));
      
      for (const job of this.postStartupJobs) {
        this.logger.info(`Starting post ${job.name}`)
        await job.action();
      }
    }
    
    this.platform.get.actions.onAppReady();

    this.logger.debug("Notifying all controllers of connected status")
  }

  public async fetchStartData() {
    this.logger.info("Starting startup jobs");
    for (const job of this.startupJobs) {
      this.logger.info(`Starting ${job.name}`)
      await job.action();
      job.done = true;
      this.logger.info(`Finished ${job.name}`)
    }
  }
  
  @Watch('websockets', { deep: true })
  public async onWebsocketsChange(newVal: SocketState) {
    if (newVal.socket.isConnected && this.loading) {
      this.logger.info("Websockets connected, loading app");
      this.loading = false;
      await this.setupApp();
      this.logger.info("Finished loading app");
    }

    if (!newVal.socket.isConnected && !this.loading) {
      this.logger.warn("Websockets disconnected, unloading app");
      this.logger.debug("Notifying all controllers of disconnected status")
      this.loading = true;
    }
  }
  
  async checkForFirstRun() {
    if (!this.platform.isElectron()) {
      return;
    }
    
    if (await this.platform.get.app.cpm.required() && await this.platform.get.app.cpm.isFirstLaunch()) {
     this.showOnboarding = true;
    }
  }
  
  startNetworkMonitor() {
    if (!navigator.onLine) {
      alertController.warning("It looks like you've lost your internet connection, the app may not work as expected");
    }
    
    window.addEventListener("offline", () => {
      alertController.warning("It looks like you've lost your internet connection, the app may not work as expected");
    });

    window.addEventListener("online", () => {
      alertController.success("You're back online, the app should be working as expected");
    });
  }

  async restartApp() {
    this.platform.get.actions.restartApp();
  }
  
  async useSystemBar() {
    // Wait until we see the loaded class on the body or after 5 seconds
    await new Promise(res => {
      let timeout: any = null;
      let interval: any = null;
      
      timeout = setTimeout(() => {
        if (interval) clearInterval(interval)
        if (timeout) clearTimeout(timeout)
        res(null)
      }, 5000) // fail after 5 seconds
      
      interval = setInterval(() => {
        if (document.body.classList.contains("loaded")) {
          if (interval) clearInterval(interval)
          if (timeout) clearTimeout(timeout)
          res(null)
        }
      }, 100)
    })
    
    const useSystemBarRaw = localStorage.getItem("useSystemFrame") ?? "false"
    const useSystemBar = useSystemBarRaw === "true";
    
    if (useSystemBar) {      
      if (!document.body.classList.contains("system-frame")) {
        document.body.classList.add("system-frame")
      }
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
      && this.allJobsDone()
  }

  get advertsEnabled(): boolean {
    return adsEnabled(this.settings.settings, this.debugDisabledAdAside);
  }

  get systemBarDisabled() {
    return !this.settings?.settings?.appearance?.useSystemWindowStyle ?? false;
  }
  
  get status() {
    if (this.appStarting && !this.appInstalling) {
      return "Starting up"
    } else if (this.appInstalling) {
      return "Installing"
    } else if (this.appConnecting && !this.isReconnecting && !this.appInstalling) {
      return "Connecting..."
    } else if (!this.appStarting && !this.appInstalling && this.isReconnecting) {
      return "Reconnecting..."
    }
  }

  get showSidebar() {
    return !this.$route.path.startsWith('/settings');
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  height: calc(100% - 2rem);
  position: relative;
  
  .macos & {
    // Title bar on macos is 1.8rem not 2rem
    height: calc(100% - 1.8rem);
  }

  .system-frame & {
    height: 100%;
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

.debug {
  position: absolute;
  left: 1.5rem;
  bottom: 1rem;
 
  .debug-item {
    color: rgba(white, .2);
    transition: color .25s ease-in-out;
    
    &.active {
      color: var(--color-primary-button);
    }
  }
}
</style>