console.info("Hello from prelaunch.ts");

const statusElement = document.getElementById('status');

function updateElementText(text: string) {
  if (!statusElement) {
    console.error("Failed to get #status element");
    return;
  }

  statusElement.innerText = text;
}

// Start a failsafe timer to launch the app after 30 seconds
setTimeout(() => {
  console.error("Failsafe timer expired, launching app");

  // Get the #bypass-button
  const btn = document.getElementById('bypass-button');
  if (!btn) {
    console.error("Failed to get #bypass-button");
    return;
  }

  btn.addEventListener('click', async () => {
    console.info("Bypass button clicked");
    await window.ipcRenderer.invoke('app:launch');
  });

  btn.classList.remove('hidden');
}, 30000);

window.ipcRenderer.invoke('updater:check-for-update')
  .then(async (updater) => {
    console.info("Received updater", updater);
    if (updater) {
      updateElementText("Preparing update...");
      return;
    }

    await window.ipcRenderer.invoke('app:launch');
  })
  .catch((e) => {
    console.error("Failed to get updater", e);
    // Just launch the app
    window.ipcRenderer.invoke('app:launch').catch((e) => console.error("Failed to launch app", e));
  });

window.ipcRenderer.on("updater:update-cancelled", async () => {
  console.info("Update cancelled");
  await window.ipcRenderer.invoke('app:launch');
});

window.ipcRenderer.on('updater:update-not-available', async() => {
  console.info("Update not available");

  await window.ipcRenderer.invoke('app:launch')
});

window.ipcRenderer.on('updater:error', async (e) => {
  console.error("Updater error", e);
  await window.ipcRenderer.invoke('app:launch')
});

window.ipcRenderer.on('updater:download-progress', (progress: any) => {
  console.info("Download progress", progress);
  updateElementText(`Downloading update: ${Math.round(progress.percent)}%`);
});

window.ipcRenderer.on('updater:update-downloaded', async () => {
  console.info("Update downloaded, restarting app");
  updateElementText("Update downloaded, restarting...");
});