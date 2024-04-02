import {logger} from '@/core/logger';
import {ipcRenderer} from 'electron';

logger.info("Hello from prelaunch.ts");

const statusElement = document.getElementById('status');

function updateElementText(text: string) {
  if (!statusElement) {
    logger.error("Failed to get #status element");
    return;
  }
  
  statusElement.innerText = text;
}

// Start a failsafe timer to launch the app after 30 seconds
setTimeout(() => {
  logger.error("Failsafe timer expired, launching app");
  
  // Get the #bypass-button
  const btn = document.getElementById('bypass-button');
  if (!btn) {
    logger.error("Failed to get #bypass-button");
    return;
  }
  
  btn.addEventListener('click', async () => {
    logger.info("Bypass button clicked");
    await ipcRenderer.invoke('app:launch');
  });
  
  btn.classList.remove('hidden');
}, 30000);

ipcRenderer.invoke('updater:check-for-update')
  .then(async (updater) => {
    logger.info("Received updater", updater);
    if (updater) {
      updateElementText("Preparing update...");
      return;
    }

    await ipcRenderer.invoke('app:launch');
  })
  .catch((e) => {
    logger.error("Failed to get updater", e);
    // Just launch the app
    ipcRenderer.invoke('app:launch').catch((e) => logger.error("Failed to launch app", e));
  });

ipcRenderer.on('updater:update-not-available', async() => {
  logger.info("Update not available");

  await ipcRenderer.invoke('app:launch')
});

ipcRenderer.on('updater:error', async (e) => {
  logger.error("Updater error", e);
  await ipcRenderer.invoke('app:launch')
});

ipcRenderer.on('updater:download-progress', (progress: any) => {
  logger.info("Download progress", progress);
  updateElementText(`Downloading update: ${Math.round(progress.percent)}%`);
});

ipcRenderer.on('updater:update-downloaded', async () => {
  logger.info("Update downloaded, restarting app");
  updateElementText("Update downloaded, restarting...");
});