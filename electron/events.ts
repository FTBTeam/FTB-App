import { app, BrowserWindow, clipboard, dialog, ipcMain, shell } from 'electron';
import os from 'os';
import { appHome, reloadMainWindow, updateApp } from './main';
import fs from 'fs';
import https from 'https';
import { execSync } from 'child_process';
import AdmZip from 'adm-zip';
import { autoUpdater } from 'electron-updater';
import path from 'path';
import { JavaVerifier } from './javaVerifier.ts';
import log from 'electron-log/main';

let checkUpdaterLock = false;

ipcMain.handle("os/platform", async () => {
  switch (os.type()) {
    case 'Darwin':
      return 'mac';
    case 'Linux':
      return 'linux';
    default:
      return 'windows';
  }
})

ipcMain.handle("os/arch", async () => {
  return os.arch();
});

ipcMain.on('action/quit-app', () => {
  log.debug("Quitting app")
  process.exit(1);
});

ipcMain.on('action/open-link', (_, data) => {
  shell.openExternal(data).catch(log.error)
});

ipcMain.on("action/reload-main-window", async () => {
  await reloadMainWindow()
});

ipcMain.on("action/write-to-clipboard", async (_, data) => {
  clipboard.writeText(data);
});

ipcMain.on('action/open-dev-tools', (event) => {
  const window = BrowserWindow.fromWebContents(event.sender);
  if (!window) {
    return;
  }
  
  if (window) {
    // If dev tools is already open, focus it
    if (window.webContents.isDevToolsOpened()) {
      window.webContents.closeDevTools();
      window.webContents.openDevTools();
    } else {
      window.webContents.openDevTools();
    }
  }
});

ipcMain.on('action/control-window', (event, data) => {
  const window = BrowserWindow.fromWebContents(event.sender);
  if (window) {
    switch (data.action) {
      case 'close':
        window.close();
        if (!import.meta.env.PROD) {
          window.webContents.closeDevTools();
        }

        break;
      case 'minimize':
        window.minimize();
        break;
      case 'maximize':
        if (!window.isMaximized()) {
          window.maximize();
        } else {
          window.unmaximize();
        }
        break;
    }
  }
});

ipcMain.handle('action/select-file', async (event) => {
  const window = BrowserWindow.fromWebContents(event.sender);
  if (!window) {
    return null;
  }

  log.debug("Selecting file using the frontend")
  const result = await dialog.showOpenDialog(window, {
    properties: ['openFile', 'showHiddenFiles', 'dontAddToRecent'],
    filters: [
      {
        name: 'Java',
        extensions: ['*'],
      },
    ],
  });

  if (result.filePaths.length > 0) {
    return result.filePaths[0];
  }
});

ipcMain.handle('action/open-finder', async (_, data) => {
  try {
    await shell.openPath(data);
    return true;
  } catch (e) {
    log.error("Failed to open finder", e)
    return false;
  }
});

ipcMain.handle('action/select-folder', async (event, data) => {
  const window = BrowserWindow.fromWebContents(event.sender);
  if (!window) {
    log.debug("Win is null, unable to select folder")
    return null;
  }

  const result: any = await dialog.showOpenDialog(window, {
    properties: ['openDirectory'],
    defaultPath: data,
  });

  if (result.filePaths.length > 0) {
    return result.filePaths[0];
  } else {
    log.debug("No file paths returned from dialog, this could be an error or just a cancel")
    return null;
  }
});

ipcMain.handle("action/download-file", async (_, args) => {
  const url = args.url;
  const path = args.path;

  // Node fetch no work and other libraries are too big or require esm
  return await downloadFile(url, path);
});

ipcMain.handle("action/app/get-channel", async () => {
  const channel = autoUpdater.channel;
  if (!channel) {
    const appVersion = autoUpdater.currentVersion.version;
    if (appVersion.includes("-beta")) {
      return "beta";
    } else if (appVersion.includes("-alpha")) {
      return "alpha";
    } else {
      return "stable";
    }
  }

  return channel;
})

ipcMain.handle("action/java/verify", async () => {
  const verifier = new JavaVerifier();
  try {
    return await verifier.verifyJava();
  } catch (e) {
    log.error(e)
    return false;
  }
})

ipcMain.handle("action/app/update", async () => {
  updateApp("Auto updater")
})

ipcMain.handle("action/app/change-channel", async (_, data) => {
  log.debug("Changing app channel", data)

  autoUpdater.channel = data;
  updateChannel(data);

  const updateResult = await autoUpdater.checkForUpdates();
  if (updateResult?.downloadPromise) {
    const version = await updateResult.downloadPromise;
    // Ask the user if they want to update
    log.debug("Update downloaded", version)

    const latestVersion = autoUpdater.currentVersion.version;
    const currentVersion = app.getVersion();

    log.debug("Latest version", latestVersion)
    log.debug("Current version", currentVersion)

    const result = await dialog.showMessageBox({
      message: `A new version of the app is available, would you like to update?, ${latestVersion} -> ${currentVersion}`,
      type: "info",
      buttons: ["Yes", "No"],
      defaultId: 0,
    })
    
    if (result.response === 0) {
      updateApp("ChannelChange");
    }
  }
})

ipcMain.handle("action/extract-file", async (_, args) => {
  const input = args.input;
  const output = args.output;

  if (!fs.existsSync(output)) {
    fs.mkdirSync(output, { recursive: true });
  }

  log.log("Extracting file", input, output)

  if (input.endsWith(".tar.gz")) {
    return extractTarball(input, output);
  } else if (input.endsWith(".zip")) {
    return extractZip(input, output);
  }

  return false
});

ipcMain.handle("action/test-java-version", async (_, args) => {
  const { jreExecPath } = args;
  if (!jreExecPath) {
    return false;
  }
  
  try {
    execSync(`"${jreExecPath}" --version`, { stdio: 'ignore' });
  } catch (e) {
    log.error("Failed to execute java version command", e)
    return false;
  }
});

ipcMain.handle('updater:check-for-update', async () => {
  if (checkUpdaterLock) {
    log.debug("Updater is already checking for updates, returning")
    return false;
  }

  checkUpdaterLock = true;
  const result = await autoUpdater.checkForUpdates();

  if (result?.downloadPromise) {
    log.debug("Waiting for download promise")
    const version = await result.downloadPromise;
    log.debug("Download promise resolved", version)
    checkUpdaterLock = false;
    return true;
  }

  checkUpdaterLock = false;
  return false;
})

// OW Electron stuff
ipcMain.handle("ow/cpm/is-required", async () => {
  return (app as any).overwolf.isCMPRequired();
})

ipcMain.handle("ow/cpm/open-window", async (_, data) => {
  (app as any).overwolf.openCMPWindow({
    tab: data ?? "purposes"
  });
});

// TODO: Moves these somewhere else plz

/**
 * Download method that uses native node modules and supports redirects
 */
function downloadFile(url: string, path: string): Promise<boolean> {
  return new Promise((resolve, reject) => {
    const file = fs.createWriteStream(path);

    const download = (url: string, redirects = 0) => {
      if (redirects > 10) {
        reject(`Too many redirects for '${url}'`);
        return;
      }

      https.get(url, function(response: any) {
        if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
          // If it's a redirect, make a request to the redirect location
          download(response.headers.location, redirects + 1);
        } else if (response.statusCode !== 200) {
          // If the status code is not 200, reject the promise
          reject(new Error(`Failed to get '${url}' (${response.statusCode})`));
        } else {
          // Otherwise, pipe the response into the file
          response.pipe(file);

          file.on('finish', function() {
            file.close();
            resolve(true);
          });

          file.on('error', (err) => {
            reject(err);
          });
        }
      }).on('error', (err) => {
        reject(err);
      });
    };

    download(url);
  });
}

function extractTarball(tarballPath: string, outputPath: string) {
  // It looks like tar is available on all platforms, so we can just use that `tar -xzf`
  // But let's redirect the contents to the output path
  // And capture the output so we can log it
  const command = `tar -xzf "${tarballPath}" -C "${outputPath}"`;
  log.debug("Extracting tarball", command)
  const output = execSync(command).toString();
  log.debug("Tarball extraction output", output)

  return true;
}

function extractZip(zipPath: string, outputPath: string) {
  const zip = new AdmZip(zipPath);
  zip.extractAllTo(outputPath, true);
  return true;
}

function updateChannel(channel: string) {
  try {
    const parent = path.join(appHome, 'storage');
    const channelFile = path.join(parent, 'electron-settings.json');
    if (!fs.existsSync(parent)) {
      fs.mkdirSync(parent, { recursive: true });
    }

    let existingData: any | null = null;
    if (fs.existsSync(channelFile)) {
      existingData = JSON.parse(fs.readFileSync(channelFile, 'utf-8'));
    }

    const newData = {
      ...existingData,
      channel
    };

    fs.writeFileSync(channelFile, JSON.stringify(newData, null, 2));
  } catch (e) {
    log.error("Failed to update channel", e)
  }
}