<script lang="ts" setup>
import Sidebar from '@/components/layout/sidebar/Sidebar.vue';
import TitleBar from '@/components/layout/TitleBar.vue';
import {SocketState} from '@/modules/websocket/types';
import {SettingsState} from '@/modules/settings/types';
import platform from '@/utils/interface/electron-overwolf';
import AdAside from '@/components/layout/AdAside.vue';
import GlobalComponents from '@/components/layout/GlobalComponents.vue';
import {ns} from '@/core/state/appState';
import {AsyncFunction} from '@/core/types/commonTypes';
import {createLogger} from '@/core/logger';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {constants} from '@/core/constants';
import {adsEnabled, emitter} from '@/utils';
import Loader from '@/components/ui/Loader.vue';
import Onboarding from '@/components/modals/Onboarding.vue';
import { UiButton, Modal } from '@/components/ui';
import {alertController} from '@/core/controllers/alertController';
import { onMounted, ref, computed } from 'vue';
import { services } from '@/bootstrap.ts';
import { useRouter } from 'vue-router';
import { useInstanceStore } from '@/store/instancesStore.ts';

// TODO: [port] Fix me
// @State('settings') settings!: SettingsState;
// @Action('loadSettings', { namespace: 'settings' }) loadSettings: any;
// @Action('registerPingCallback') registerPingCallback: any;
// @Action('loadProfiles', { namespace: 'core' }) loadProfiles!: AsyncFunction;
// @Action('loadInstances', ns("v2/instances")) loadInstances!: AsyncFunction;
// @Getter("getDebugDisabledAdAside", {namespace: 'core'}) debugDisabledAdAside!: boolean;

const router = useRouter()

const settings: SettingsState = {} as any;
const loadSettings: any = null;
const loadProfiles: AsyncFunction = null;
const loadInstances: AsyncFunction = null;
const debugDisabledAdAside: boolean = false;

const logger = createLogger("MainApp.vue");
const instanceStore = useInstanceStore();

// App runtime
const loading = ref(true);
const hasInitialized = ref(false);
const isMac = ref(false);

const appConnected = ref(false);

// App init
const appStarting = ref(true);
const appLoaded = ref(false);
const appInstalling = ref(false);
const appInstallStage = ref("");

// App install
const appInstallFailed = ref(false);
const appStartupFailed = ref(false);
const appInstallError = ref("");

// Onboarding
const showOnboarding = ref(false);

const startupJobs = ref([
  {
    name: "Settings",
    done: false,
    action: () => loadSettings()
  },
  {
    name: "Init App",
    done: false,
    action: () => initApp()
  }
])

const postStartupJobs = ref([
  {
    name: "Profiles",
    done: false,
    action: () => loadProfiles()
  },
  {
    name: "Loading Installed Instances",
    done: false,
    action: () => loadInstances()
  },
])

function allJobsDone() {
  return startupJobs.value.every(job => job.done)
}

onMounted(async () => {
  logger.info("App started on ", constants.platform)
  useSystemBar().catch(logger.error)
  startNetworkMonitor();
  
  // Technically not possible to error but we have to make the lint happy
  initApp().catch(logger.error)
  
  // useSystemBar().catch(console.error)
  // startNetworkMonitor();
  //
  // logger.info("App started on ", constants.platform)
  // isMac.value = false// os.type() === 'Darwin'; // TODO: [port] Fix me
  //
  // appStarting.value = true;
  // appLoaded.value = false;
  //
  // logger.info("Mounted MainApp for ", constants.platform);
  //
  // // Check if the app is installed
  // let installed = true;
  // if (!platform.isOverwolf() && constants.isProduction) {
  //   installed = await platform.get.app.runtimeAvailable();
  //
  //   logger.info("App installed", installed);
  //   if (!installed) {
  //     // We need to install the app
  //     try {
  //       await installApp();
  //     } catch (e: any) {
  //       appInstallFailed.value = true;
  //       appInstallError.value = e?.customMessage || "The app has failed to install the necessary files, please try again or contact support...";
  //       logger.error("Failed to install app", e);
  //     }
  //   }
  // }
  //
  // await startApp();
  // appLoaded.value = true;
  //
  // emitter.on("ws.message", (data: any) => {
  //   if (data?.type === "refreshInstancesRequest") {
  //     loadInstances();
  //   }
  // })
})

async function initApp() {
  console.log("Init app")
  await waitForSocket()
  
  console.log("Connected")
  appConnected.value = true
  await instanceStore.loadInstances();
  console.log(appConnected.value)
}

async function waitForSocket() {
  // Wait for websocket
  await new Promise((res) => {
    function check() {
      if (services.websocket.isAlive()) {
        res(null);
      } else {
        setTimeout(check, 1000);
      }
    }
    
    check();
  });
}

async function installApp() {
  appInstalling.value = true;
  logger.info("Installing app");

  await platform.get.app.installApp((stage: any) => appInstallStage.value = stage, () => {}, false)

  appInstalling.value = false;
}

async function startApp() {
  appStarting.value = false;
  logger.info("Starting app");

  if (!constants.isProduction) {
    logger.info("Starting production app");

    // If we're in dev, we just default to localhost
    await connectToWebsockets();
    return;
  }

  logger.info("Sending start subprocess message");
  try {
    let appData: any = await platform.get.app.startSubprocess();

    // False means update
    if (!appData) {
      try {
        await platform.get.app.updateApp(
          (stage: any) => appInstallStage.value = stage,
          () => logger.info("Update complete")
        );
      } catch (e: any) {
        appInstallFailed.value = true;
        appInstallError.value = e?.customMessage || "The app has failed to update, please try again or contact support...";
        logger.error("Failed to update app", e);
      }

      appData = await platform.get.app.startSubprocess();
    }

    logger.info("Started app subprocess", appData);
    if (!appData.port || !appData.secret) {
      appStartupFailed.value = true;
      appInstallError.value = "The app has failed to start due to connection issues, please try again or contact support...";
      logger.error("Failed to start subprocess due to connection issues", appData);
      return;
    }

    // Finally we need to get the app to connect to the subprocess
    await connectToWebsockets(appData.port, appData.secret);
  } catch (e: any) {
    // Same as the install, grab the custom error if possible
    appStartupFailed.value = true;
    appInstallError.value = e?.customMessage || "The app has failed to start, please try again or contact support...";
    logger.error("Failed to start subprocess", e);
  }
}

async function connectToWebsockets(port: number = 13377, secret: string = "") {
  // Store the secret
  logger.info("Starting websocket connection on port", port, "with secret", secret);
  // await storeWsSecret(secret);
  
  // TODO: Fix me plz
  // $connect('ws://localhost:' + port);
  await new Promise((res, rej) => {
    function check() {
      if (services.websocket.isAlive()) {
        res(null);
      } else {
        setTimeout(check, 1000);
      }
    }
    
    check();
  })
  
  console.log("Connected to websocket")
}

async function setupApp() {
  if (!hasInitialized.value) {
    await fetchStartData();

    logger.debug("Starting ping poll");
    registerPingCallback((data: any) => {
      if (data.type === 'ping') {
        gobbleError(() => sendMessage("pong", {}, 500))
      }
    });

    hasInitialized.value = true;

    checkForFirstRun()
      .then(() => logger.info("Finished checking for first run"))
      .catch(error => logger.error("Failed to check for first run", error));

    for (const job of postStartupJobs.value) {
      logger.info(`Starting post ${job.name}`)
      await job.action();
    }
  }

  platform.get.actions.onAppReady();

  logger.debug("Notifying all controllers of connected status")
}

async function fetchStartData() {
  logger.info("Starting startup jobs");
  for (const job of startupJobs.value) {
    logger.info(`Starting ${job.name}`)
    await job.action();
    // TOOD: likely not reactive
    job.done = true;
    logger.info(`Finished ${job.name}`)
  }
}

// TODO: [port] Fix me
  // @Watch('websockets', { deep: true })
  // public async onWebsocketsChange(newVal: SocketState) {
  //   if (newVal.socket.isConnected && this.loading) {
  //     logger.info("Websockets connected, loading app");
  //     this.loading = false;
  //     await this.setupApp();
  //     logger.info("Finished loading app");
  //   }
  //
  //   if (!newVal.socket.isConnected && !this.loading) {
  //     logger.warn("Websockets disconnected, unloading app");
  //     logger.debug("Notifying all controllers of disconnected status")
  //     this.loading = true;
  //   }
  // }

async function checkForFirstRun() {
  if (!platform.isElectron()) {
    return;
  }

  if (await platform.get.app.cpm.required() && await platform.get.app.cpm.isFirstLaunch()) {
    showOnboarding.value = true;
  }
}

function startNetworkMonitor() {
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

async function restartApp() {
  platform.get.actions.restartApp();
}

async function useSystemBar() {
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

const appConnecting = computed(() => true)// !websockets.socket.isConnected) // TODO: [port] fixme
const reconnectAttempts = computed(() => 0)// websockets.reconnects) // TODO: [port] fixme
const isReconnecting = computed(() => appConnecting.value) //&& !websockets.firstStart) // TODO: [port] fixme
const appReadyToGo = appConnected.value //computed(() => !appStarting.value && appLoaded.value && !appInstalling && !isReconnecting && !appConnecting.value && allJobsDone())
const advertsEnabled = computed(() => adsEnabled(settings.settings, debugDisabledAdAside))
const systemBarDisabled = computed(() => !settings.settings.appearance.useSystemWindowStyle)

const status = computed(() => {
  if (appStarting.value && !appInstalling) {
    return "Starting up"
  } else if (appInstalling) {
    return "Installing"
  } else if (appConnecting.value && !isReconnecting && !appInstalling) {
    return "Connecting..."
  } else if (!appStarting && !appInstalling && isReconnecting) {
    return "Reconnecting..."
  }
})

const showSidebar = computed(() => !router.currentRoute.value.path.startsWith('/settings'))
</script>

<template>
  <div id="app" class="theme-dark" :class="{'macos': isMac}">
    <title-bar />
    
    <div class="app-container relative flex justify-center items-center" v-if="!appConnected">
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
    <div class="app-container" v-if="appConnected">
      <main class="main">
        <sidebar v-if="showSidebar" />
        <div class="app-content relative">
          <router-view />
        </div>
        <ad-aside v-show="advertsEnabled" :hide-ads="showOnboarding" />
      </main>
    </div>
    
    <GlobalComponents v-if="appReadyToGo" />
    <onboarding v-if="showOnboarding" @accepted="showOnboarding = false" />
    
    <modal :open="appInstallFailed || appStartupFailed" title="Something's gone wrong!" sub-title="Looks like there might be a problem...">
      <p class="mb-1">{{appInstallError}}</p>
      
      <p>Sometimes these things happen, it could be a random failure, try restarting the app and if that doesn't work, try reaching out on our Discord (linked below)</p>
      <template #footer>
        <div class="flex gap-4 justify-end items-center">
          <UiButton size="small" type="danger" class="mr-auto" @click="() => {
            appInstallFailed = false
            appStartupFailed = false
          }">Let me in anyway...</UiButton>
          <UiButton type="info" :icon="['fab', 'discord']" @click="platform.get.utils.openUrl('https://go.ftb.team/ftb-app-support-discord')">Support</UiButton>
          <UiButton type="success" icon="power-off" @click="restartApp">Restart</UiButton>
        </div>
      </template>
    </modal>
  </div>
</template>

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