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

onMounted(() => {
  log.log('Prelauncher mounted');

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
  log.info("Checking for Java");
  if (checkingForJava.value) {
    return;
  }
  
  checkingForJava.value = true;
  updating.value = false;
  updateProgress.value = 0;
  checkingForUpdates.value = false;
  
  const result = await window.ipcRenderer.invoke("action/java/verify")
  if (result) {
    startApp()
    return;
  }
  
  log.log('Total failure, java not installed');
  
  // If we get here, java is not installed
  totalFailure.value = true;
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
  log.log('Update complete');
  
  let secondsPassed = 0;
  const interval = setInterval(() => {
    secondsPassed++;
    restartCountdown.value = 5 - secondsPassed;
    
    if (secondsPassed >= 5) {
      clearInterval(interval);
      window.ipcRenderer.send('action/app/update');
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
    } catch (e) {
      log.error(e)

      javaCheckStatus.value = `Java check failed, retrying... (${attempts}/10)`
      // Wait 2 seconds before trying again
      await new Promise((resolve) => setTimeout(resolve, 2000));
    }
  }
}
</script>

<template>
  <div id="highlighter"></div>
  
  <div id="app-prelauncher">
    <img src="/assets/images/branding/ftb-logo-full.svg" alt="FTB Logo" width="140" style="margin-bottom: 2rem;" />

    <div id="status">
      We're just getting things ready
    </div>
    
    <p v-if="totalFailure">Something has unrecoverably failed, please try restarting</p>
    <p v-else-if="checkingForUpdates">Checking for updates</p>
    <p v-else-if="updating">Updating to {{updateName}} ({{updateProgress}}%)</p>
    <p v-else>{{ javaCheckStatus }}</p>

    <div id="bypass-button" :class="{'hidden': !showBypassButton}" @click="() => checkForJava()">
      Skip
    </div>
  </div>
</template>

<style lang="scss">
#app-prelauncher {
  flex: 1;
  background-color: rgba(42, 42, 42, 0.49);
  backdrop-filter: blur(80px);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  color: white;
  padding: 3rem;
  text-align: center;
  border-radius: 5px;
  line-height: 1.7em;
  position: relative;
  z-index: 2;
  margin: .5rem;
}

#highlighter {
  position: absolute;
  height: 100%;
  width: 80px;
  background-color: #B11917;
  left: 50%;
  top: 50%;
  transform: translateX(-50%);
  transform-origin: center top;
  filter: blur(30px);
  opacity: .45;
  animation: highlighter 4s infinite linear;
}

@keyframes highlighter {
  0% {
    transform: translateX(-50%) rotateZ(0deg);
    background-color: #B11917;
  }
  25% {
    transform: translateX(-50%) rotateZ(90deg);
    background-color: #798B2F;
  }
  50% {
    transform: translateX(-50%) rotateZ(180deg);
    background-color: #0787C1;
  }
  75% {
    transform: translateX(-50%) rotateZ(270deg);
    background-color: #798B2F;
  }
  100% {
    transform: translateX(-50%) rotateZ(360deg);
    background-color: #B11917;
  }
}

#bypass-button {
  width: 100%;
  padding: .8rem 1rem;
  text-align: center;
  background-color: rgba(44, 44, 44, 0.65);
  cursor: pointer;
  transition: background-color .2s;
  margin-top: 2rem;
  border-radius: 8px;
  font-weight: bold;
}

#bypass-button:hover {
  background-color: #3a3a3a;
}

.hidden {
  display: none;
}
</style>