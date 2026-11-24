<script lang="ts" setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue';
import { ProgressInfo } from '@/prelaunch/app';
import log from 'electron-log/renderer'

/**
 * Prelaunch flow
 * - Check for updates
 * - If updates are available, download them and apply them
 * - If no updates are available, continue
 * - Check to see if java is installed and working
 * - If java is not installed, install it,
 * - If java is installed, continue
 * - Hand off to the main app
 */

const checkingForUpdates = ref(true);
const updating = ref(false);
const updateProgress = ref(0);
const restartCountdown = ref(0);

const updateName = ref('Next');
const checkingForJava = ref(false);
const javaCheckStatus = ref('Checking for Java');

const showBypassButton = ref(false); // Show the bypass button after 60 seconds
const totalFailure = ref(false);

const forceShowRetry = ref(false);

const statusMessages = [
  "We're just getting things ready",
  "Having fun on GTA 6?",
  "Gathering blocks...",
  "Time for a break from Vice City?",
  "Took less than 13 years to load, promise",
  "Swapping the getaway car for a minecart",
  "Punching trees, no wanted level required",
  "Six stars? We only do diamonds",
  "Teaching Steve how to drive",
  "Building a safehouse out of cobblestone",
  "Trading the yacht for a boat made of oak",
  "Spinning up the Create contraptions...",
  "Untangling your AE2 channels",
  "DireWire? Where!",
  "Calibrating the Mekanism reactor (safely)",
  "Does anyone else hear ticking?...",
  "Feeding the quarry another chunk",
  "Convincing GregTech to let you craft a pickaxe",
  "Completing Quests...",
  "Wiring up the base with cables you'll regret",
  "Asking the Draconic Evolution core nicely",
  "Bribing the TPS to stay at 20",
  "Preloading Frames...",
  "Don't look now, we're loading...",
];
const STATUS_ROTATE_MS = 4000;

const statusMessage = ref(statusMessages[0]);
let statusInterval: ReturnType<typeof setInterval> | undefined;

let statusQueue: string[] = shuffle(statusMessages.slice(1));

// Pulled from utils but we don't have them here...
function shuffle<T>(items: T[]): T[] {
  // Fisher-Yates
  for (let i = items.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [items[i], items[j]] = [items[j], items[i]];
  }
  return items;
}

// Select the next message without using one from this "shuffle"
function nextStatusMessage() {
  if (statusQueue.length === 0) {
    statusQueue = shuffle([...statusMessages]);
    if (statusQueue.length > 1 && statusQueue[0] === statusMessage.value) {
      statusQueue.push(statusQueue.shift()!);
    }
  }

  statusMessage.value = statusQueue.shift()!;
}

onMounted(() => {
  log.log('Prelauncher mounted');
  statusInterval = setInterval(nextStatusMessage, STATUS_ROTATE_MS);

  if (!import.meta.env.PROD) {
    log.info("Skipping update and java check in dev mode");
    startApp();
    return;
  }

  if (window.nodeUtils.os.isFlatPak()) {
    log.info("Flatpak detected, skipping update check");
    progressToJavaCheck({});
    return;
  }

  window.ipcRenderer.on("updater:checking-for-update", onCheckingForUpdate)
  window.ipcRenderer.on('updater:download-progress', onDownloadProgress);
  window.ipcRenderer.on("updater:update-downloaded", onUpdateComplete);
  window.ipcRenderer.on("updater:update-available", onUpdateAvailable);
  window.ipcRenderer.on("updater:update-not-available", progressToJavaCheck);
  window.ipcRenderer.on("updater:update-cancelled", progressToJavaCheck)
  window.ipcRenderer.on("updater:error", progressToJavaCheck)

  window.ipcRenderer.on("java/message", onJavaVerifyMessage)

  nextTick(() => {
    window.ipcRenderer.send("prelaunch/im-ready")
  })

  setTimeout(() => {
    if (!checkingForUpdates.value) {
      return; // No bypass if we're done checking for updates
    }

    log.info("Giving up, showing bypass button");
    showBypassButton.value = true;
  }, 60_000); // Wait 60 seconds, then give up and show the bypass button
})

onUnmounted(() => {
  log.log('Prelauncher unmounted');
  clearInterval(statusInterval);
  window.ipcRenderer.off("updater:checking-for-update", onCheckingForUpdate)
  window.ipcRenderer.off('updater:download-progress', onDownloadProgress);
  window.ipcRenderer.off("updater:update-downloaded", onUpdateComplete);
  window.ipcRenderer.off("updater:update-available", onUpdateAvailable);
  window.ipcRenderer.off("updater:update-not-available", progressToJavaCheck);
  window.ipcRenderer.off("updater:update-cancelled", progressToJavaCheck);
  window.ipcRenderer.off("updater:error", progressToJavaCheck);
  
  window.ipcRenderer.off("java/message", onJavaVerifyMessage)
})

async function checkForJava() {
  if (checkingForJava.value) {
    return;
  }
  
  log.info("Checking for Java");
  checkingForJava.value = true;
  updating.value = false;
  updateProgress.value = 0;
  checkingForUpdates.value = false;

  try {
    const result = await window.ipcRenderer.invoke("action/java/verify")
    if (result) {
      startApp()
      return;
    }

    log.log('Total failure, java not installed');

    // If we get here, java is not installed
    totalFailure.value = true;
  } finally {
    checkingForJava.value = false;
  }
}

function retry() {
  log.info("Retrying startup");
  totalFailure.value = false;
  javaCheckStatus.value = 'Checking for Java';

  attemptCheckJava().catch(e => {
    log.log(e);
    totalFailure.value = true
  })
}

function startApp() {
  log.log('Starting app');
  window.ipcRenderer.send('action/launch-app');
}

// Events
function onJavaVerifyMessage(_: any, message: string) {
  javaCheckStatus.value = message;
}

function onCheckingForUpdate(_: any) {
  log.info("Checking for updates");
  checkingForUpdates.value = true;
  updating.value = false;
  updateProgress.value = 0;
}

function onUpdateComplete(_: any) {
  log.info("Update complete");
  updating.value = false;
  checkingForUpdates.value = false;
  updateProgress.value = 0;
  
  let secondsPassed = 0;
  const interval = setInterval(() => {
    secondsPassed++;
    restartCountdown.value = 5 - secondsPassed;
    
    if (secondsPassed >= 5) {
      window.ipcRenderer.send('action/app/update');
      clearInterval(interval);
    }
  }, 1000);
}

function onDownloadProgress(_: any, info: any) {
  log.info("Download progress", info);
  const typedInfo = info as ProgressInfo;
  if (!updating.value) {
    updating.value = true;
    checkingForUpdates.value = false;
  }
  
  updateProgress.value = typedInfo.percent
}

function onUpdateAvailable(_: any, version: any) {
  log.info("Update available", version);
  updating.value = true;
  checkingForUpdates.value = false;
  updateName.value = version;
  
  // Start the download (Just jump start it if needed)
  window.ipcRenderer.send("updater:download-update");
}

function progressToJavaCheck(_: any) {
  log.info("No updates available, proceeding to Java check");
  updating.value = false;
  checkingForUpdates.value = false;
  updateProgress.value = 0;
  log.log('No updates available');
  
  // Check for java
  attemptCheckJava().catch(e => {
    log.log(e);
    totalFailure.value = true
  })
}

async function attemptCheckJava() {
  log.info("Attempting to check for Java");
  
  let attempts = 0;
  while (attempts++ < 10) {
    try {
      await checkForJava()
      return;
    } catch (e) {
      log.error(e)

      javaCheckStatus.value = `Java check failed, retrying... (${attempts}/10)`
      // Wait 2 seconds before trying again
      await new Promise((resolve) => setTimeout(resolve, 2000));
    }
  }

  log.log('Java check failed after all attempts');
  totalFailure.value = true;
}
</script>

<template>
  <div id="app-prelauncher">
    <div class="card">
      <div id="highlighter"></div>
      <img src="/assets/images/branding/ftb-gta-logo.webp" alt="FTB Logo" width="240" style="margin-bottom: 2rem;" />

      <div id="bypass-button" :class="{'hidden': !showBypassButton}" @click="() => checkForJava()">
        Skip
      </div>

      <div id="retry-button" v-if="totalFailure || forceShowRetry" @click="retry">
        Retry
      </div>
    </div>

    <div class="text">
      <div class="status-line">
        <Transition name="status-fade" mode="out-in">
          <div id="status" :key="statusMessage">
            {{ statusMessage }}
          </div>
        </Transition>
      </div>

      <p v-if="totalFailure">Something has unrecoverably failed, please try restarting</p>
      <p v-else-if="checkingForUpdates">Checking for updates</p>
      <p v-else-if="updating">Updating to {{updateName}} ({{updateProgress}}%)</p>
      <p v-else>{{ javaCheckStatus }}</p>
    </div>
  </div>
</template>

<style lang="scss">
#app-prelauncher {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background-color: #151515;
  color: white;
  text-align: center;
}

.card {
  flex: 1;
  min-height: 0;
  position: relative;
  overflow: hidden;
  isolation: isolate;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  border-radius: .75rem;
  background-color: #2a2a2a;
  background-image: url('/assets/images/ftb-gta.webp');
  background-position: center calc(50% - 10px);
  background-repeat: no-repeat;
  background-size: 400px;
  border: 1px solid rgba(255, 255, 255, .12);
  
  img {
    position: relative;
    z-index: 4;
  }
}

.text {
  flex: 0 0 auto;
  height: calc(2 * 1.5em + 1.75rem);
  padding: .875rem 1rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
  font-size: 13px;
  line-height: 1.5em;

  #status {
    font-weight: bold;
  }

  #status, p {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  p {
    color: rgba(255, 255, 255, .6);
  }
}

// Holographic effect, thanks internet.
#highlighter {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
  border-radius: inherit;
  overflow: hidden;

  background-image: repeating-linear-gradient(
    115deg,
    #ff2d95 0%,
    #ffb800 6%,
    #3dffa8 12%,
    #00c8ff 18%,
    #8f4dff 24%,
    #ff2d95 30%
  );
  
  background-size: 400% 400%;
  mix-blend-mode: color-dodge;
  opacity: .3;
  filter: saturate(1.3) brightness(.85);
  animation: holo-sheen 8s ease-in-out infinite alternate;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image:
      repeating-linear-gradient(-45deg, transparent 0 2px, rgba(255, 255, 255, .18) 2px 3px),
      radial-gradient(circle at 20% 30%, rgba(255, 255, 255, .5) 0 1px, transparent 2px),
      radial-gradient(circle at 70% 60%, rgba(255, 255, 255, .4) 0 1px, transparent 2px),
      radial-gradient(circle at 40% 80%, rgba(255, 255, 255, .45) 0 1px, transparent 2px);
    background-size: auto, 37px 41px, 53px 47px, 29px 31px;
    mix-blend-mode: overlay;
    animation: holo-sparkle 3s steps(6) infinite;
  }
  
  &::after {
    content: '';
    position: absolute;
    inset: -100%;
    background: linear-gradient(
      60deg,
      transparent 46%,
      rgba(255, 255, 255, .35) 48.5%,
      rgba(255, 255, 255, .9) 50%,
      rgba(255, 255, 255, .35) 51.5%,
      transparent 54%
    );
    transform: translateX(-70%) scaleX(1);
    animation: holo-glare 10s cubic-bezier(.45, 0, .2, 1) infinite;
  }
}

@keyframes holo-sheen {
  0% { background-position: 0% 50%; }
  100% { background-position: 100% 50%; }
}

@keyframes holo-sparkle {
  0% { opacity: .6; background-position: 0 0, 0 0, 0 0, 0 0; }
  50% { opacity: 1; background-position: 0 0, 7px 3px, -5px 9px, 4px -6px; }
  100% { opacity: .6; background-position: 0 0, 0 0, 0 0, 0 0; }
}

@keyframes holo-glare {
  0%, 8%     { transform: translateX(-70%) scaleX(1); }
  42%        { transform: translateX(70%) scaleX(1); }
  // Reset and go the other way
  42.01%, 58% { transform: translateX(70%) scaleX(-1); }
  92%, 100%  { transform: translateX(-70%) scaleX(-1); }
}

@media (prefers-reduced-motion: reduce) {
  #highlighter,
  #highlighter::before,
  #highlighter::after {
    animation: none;
  }
}

#bypass-button {
  position: relative;
  z-index: 4;
  width: 100%;
  padding: .6rem 1rem;
  text-align: center;
  background-color: rgba(0, 0, 0, .55);
  backdrop-filter: blur(5px);
  border: 1px solid rgba(255, 255, 255, .25);
  cursor: pointer;
  transition: background-color .2s;
  border-radius: .5rem;
  font-weight: bold;
}

#bypass-button:hover {
  background-color: rgba(0, 0, 0, .75);
}

#retry-button {
  position: absolute;
  right: 1rem;
  bottom: 1rem;
  z-index: 4;
  padding: .5rem 1.25rem;
  background-color: rgba(0, 0, 0, .55);
  backdrop-filter: blur(5px);
  border: 1px solid rgba(255, 255, 255, .25);
  border-radius: .5rem;
  font-weight: bold;
  cursor: pointer;
  transition: background-color .2s;

  &:hover {
    background-color: rgba(0, 0, 0, .75);
  }
}

.status-line {
  height: 1.5em;
}

.status-fade-enter-active,
.status-fade-leave-active {
  transition: opacity .3s ease, transform .3s ease;
}

.status-fade-enter-from {
  opacity: 0;
  transform: translateY(4px);
}

.status-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.hidden {
  display: none;
}
</style>