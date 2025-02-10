'use strict';
import {app, BrowserWindow, dialog, ipcMain, protocol, session, shell} from 'electron';
import path from 'path';
import os from 'os';
import fs from 'fs';
import * as electronLogger from 'electron-log';
import install, {VUEJS_DEVTOOLS} from 'electron-devtools-installer';
import {createProtocol} from '@ftbapp/vue-cli-plugin-electron-builder/lib';
import {createLogger} from '@/core/logger';
import https from 'https';
import AdmZip from 'adm-zip';
import {ChildProcess, execSync, spawn} from 'child_process';
import {autoUpdater} from 'electron-updater';
import {getAppHome} from '@/nuturalHelpers';

const protocolSpace = 'ftb';
const logger = createLogger('background.ts');

autoUpdater.logger = electronLogger;

// Let the app downgrade if the latest version has been removed from the CDN
const appHome = getAppHome(os.platform(), os.homedir(), path.join);

autoUpdater.allowDowngrade = true;
autoUpdater.channel = loadChannel();

const logAndEmit = (event: string, ...args: any[]) => {
  logger.debug("Emitting downloader event", event, args)
  ipcMain.emit(event, ...args);
}

let checkUpdaterLock = false;

autoUpdater.on('checking-for-update', () => logAndEmit('updater:checking-for-update'));
autoUpdater.on("update-cancelled", () => logAndEmit('updater:update-cancelled'));
autoUpdater.on('update-available', () => logAndEmit('updater:update-available'));
autoUpdater.on('update-not-available', () => logAndEmit('updater:update-not-available'));
autoUpdater.on('error', (error) => logAndEmit('updater:error', JSON.stringify(error)));
autoUpdater.on('download-progress', (progress) => logAndEmit('updater:download-progress', progress));
autoUpdater.on('update-downloaded', (info) => {
  logger.debug("Update downloaded", info)
  ipcMain.emit('updater:update-downloaded');
  updateApp("UpdateDownloaded");
});

/**
 * Fix for SUID sandbox issues on Linux
 * See: https://github.com/electron/electron/issues/17972
 */
if (process.platform == "linux") {
  app.commandLine.appendSwitch("--no-sandbox");
}

function getAppSettings(appSettingsPath: string) {
  if (!fs.existsSync(appSettingsPath)) {
    return null;
  }
  
  try {
    return JSON.parse(fs.readFileSync(appSettingsPath, 'utf-8'));
  } catch (e) {
    return null;
  }
}

logger.debug('App home is', appHome)

let cachedProcessData = null as {
  pid: number;
  port: number;
  secret: string;
} | null;

let preserveSubprocess = false;
let usingLegacySettings = false;
const appSettingsPathLegacy = path.join(appHome, 'bin', 'settings.json');
const appSettingsPath = path.join(appHome, "storage", 'settings.json');
let appSettings = getAppSettings(appSettingsPath);
if (appSettings === null) {
  logger.debug("App settings not found, trying legacy settings")
  usingLegacySettings = true;
  appSettings = getAppSettings(appSettingsPathLegacy);
}

const appLogsParent = path.join(appHome, 'logs');
try {
  if (!fs.existsSync(appLogsParent)) {
    fs.mkdirSync(appLogsParent, { recursive: true });
  }
} catch (e) {
  logger.error("Failed to create app logs parent directory", e)
}

const electronLogFile = path.join(appLogsParent, 'ftb-app-electron.log');
if (fs.existsSync(electronLogFile)) {
  try {
    fs.writeFileSync(electronLogFile, '')
  } catch (e) {
    logger.error("Failed to clear electron log file", e)
  }
}
electronLogger.transports.file.resolvePath = () => 
  electronLogFile;

Object.assign(console, electronLogger.functions);
(app as any).console = electronLogger;

const isDevelopment = process.env.NODE_ENV !== 'production' || process.argv.indexOf('--dev') !== -1;

protocol.registerSchemesAsPrivileged([{ scheme: protocolSpace, privileges: { secure: true, standard: true } }]);

if (process.env.NODE_ENV === 'development' && process.platform === 'win32') {
  app.setAsDefaultProtocolClient(protocolSpace, process.execPath, [path.resolve(process.argv[1])]);
} else {
  app.setAsDefaultProtocolClient(protocolSpace);
}

let appCommunicationSetup = false;

let subprocess: ChildProcess | null = null;
let win: BrowserWindow | null;
let prelaunchWindow: BrowserWindow | null = null;

let protocolURL: string | null;

for (let i = 0; i < process.argv.length; i++) {
  if (process.argv[i].indexOf(protocolSpace) !== -1) {
    protocolURL = process.argv[i];
    break;
  }
}

declare const __static: string;

ipcMain.on('appReady', async (event) => {
  if (protocolURL !== null) {
    event.reply('parseProtocolURL', protocolURL);
  }
});

ipcMain.on("restartApp", () => {
  reloadMainWindow()
});

ipcMain.handle("app.change-channel", async (event, data) => {
  logger.debug("Changing app channel", data)
  
  autoUpdater.channel = data;
  updateChannel(data);
  
  const updateResult = await autoUpdater.checkForUpdates();
  if (updateResult?.downloadPromise) {
    const version = await updateResult.downloadPromise;
    // Ask the user if they want to update
    logger.debug("Update downloaded", version)
    
    const latestVersion = autoUpdater.currentVersion.version;
    const currentVersion = app.getVersion();
    
    logger.debug("Latest version", latestVersion)
    logger.debug("Current version", currentVersion)
    
    if (confirm(`A new version of the app is available, would you like to update?, ${latestVersion} -> ${currentVersion}`)) {
      updateApp("ChannelChange");
    }
  }
})

ipcMain.handle("app.get-channel", async () => {
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

ipcMain.on('openModpack', (event, data) => {
  if (win !== null && win !== undefined) {
    logger.debug("Opening modpack", data)
    win.webContents.send('openModpack', data);
  } else {
    logger.warn("Win is null or undefined")
  }
});

ipcMain.on('expandMeScotty', (event, data) => {
  const window = BrowserWindow.fromWebContents(event.sender);
  if (window) {
    let [width, height] = window.getSize();
    if (data.width) {
      width = data.width;
    }
    if (data.height) {
      height = data.height;
    }
    window.setSize(width, height);
  }
});

app.on('open-url', async (event, customSchemeData) => {
  // event.preventDefault();

  win?.webContents.send('parseProtocolURL', customSchemeData);
});

ipcMain.handle('selectFolder', async (event, data) => {
  if (win === null) {
    logger.debug("Win is null, unable to select folder")
    return;
  }

  const result: any = await dialog.showOpenDialog(win, {
    properties: ['openDirectory'],
    defaultPath: data,
  });

  if (result.filePaths.length > 0) {
    return result.filePaths[0];
  } else {
    logger.debug("No file paths returned from dialog, this could be an error or just a cancel")
    return null;
  }
});

const reloadMainWindow = async () => {
  if (!win) {
    return;
  }

  preserveSubprocess = true;
  logger.info("Reloading main window")
  
  // Create a tmp window to keep the app alive
  const tmpWindow = new BrowserWindow({
    show: false,
  });

  // Reset the port and secret scanning
  appCommunicationSetup = false;
  
  // Enable the windows frame
  win.destroy()
  await createWindow(true)

  // Close the tmp window
  tmpWindow.close();
}

ipcMain.handle('setSystemWindowStyle', async (event, data) => {
  const typedData = data as boolean;
  
  if (!win) {
    return;
  }
  
  logger.info("Setting system window style to", typedData)
  
  // Reload the app settings
  if (usingLegacySettings) {
    appSettings.useSystemWindowStyle = typedData;  
  } else {
    appSettings.appearance.useSystemWindowStyle = typedData;
  }
  
  await reloadMainWindow();
});

ipcMain.handle('openFinder', async (event, data) => {
  try {
    await shell.openPath(data);
    return true;    
  } catch (e) {
    logger.error("Failed to open finder", e)
    return false;
  }
});

ipcMain.handle('selectFile', async () => {
  if (win === null) {
    return null;
  }

  logger.debug("Selecting file using the frontend")
  const result = await dialog.showOpenDialog(win, {
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

ipcMain.on('windowControls', (event, data) => {
  const window = BrowserWindow.fromWebContents(event.sender);
  if (window) {
    switch (data.action) {
      case 'close':
        window.close();
        if (process.env.NODE_ENV !== 'production') {
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

ipcMain.on('quit_app', () => {
  logger.debug("Quitting app")
  process.exit(1);
});

ipcMain.on('logout', () => {
  logger.debug("Logging out from the frontend")
});


ipcMain.on('openLink', (event, data) => {
  shell.openExternal(data);
});

ipcMain.on('openDevTools', () => {
  if (win) {
    // If dev tools is already open, focus it
    if (win.webContents.isDevToolsOpened()) {
      win.webContents.closeDevTools();
      win.webContents.openDevTools();
    } else {
      win.webContents.openDevTools();
    }
  }
});

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

ipcMain.handle("downloadFile", async (event, args) => {
  const url = args.url;
  const path = args.path;

  // Node fetch no work and other libraries are too big or require esm
  return await downloadFile(url, path);
});

function extractTarball(tarballPath: string, outputPath: string) {
  // It looks like tar is available on all platforms, so we can just use that `tar -xzf`
  // But let's redirect the contents to the output path
  // And capture the output so we can log it
  const command = `tar -xzf "${tarballPath}" -C "${outputPath}"`;
  logger.debug("Extracting tarball", command)
  const output = execSync(command).toString();
  logger.debug("Tarball extraction output", output)
  
  return true;
}

function extractZip(zipPath: string, outputPath: string) {
  const zip = new AdmZip(zipPath);
  zip.extractAllTo(outputPath, true);
  
  return true;
}

ipcMain.handle("extractFile", async (event, args) => {
  const input = args.input;
  const output = args.output;
  
  if (!fs.existsSync(output)) {
    fs.mkdirSync(output, { recursive: true });
  }
  
  console.log("Extracting file", input, output)

  if (input.endsWith(".tar.gz")) {
    return extractTarball(input, output);
  } else if (input.endsWith(".zip")) {
    return extractZip(input, output);
  }

  return false
});

ipcMain.handle("getAppExecutablePath", async (event, args) => {
  return app.getAppPath();
});

ipcMain.handle("ow:cpm:is_required", async (event) => {
  return (app as any).overwolf.isCMPRequired();
})

ipcMain.handle("ow:cpm:open_window", async (event, data) => {
  (app as any).overwolf.openCMPWindow({
    tab: data ?? "purposes" 
  });
});

ipcMain.handle("startSubprocess", async (event, args) => {
  if (process.env.NODE_ENV !== 'production') {
    logger.debug("Not starting subprocess in dev mode")
    return;
  }

  const javaPath = args.javaPath;
  const argsList = args.args as string[]
  const env = args.env as string[]

  if (subprocess !== null && cachedProcessData === null) {
    // Check if the process is still running
    let pid = subprocess.pid;
    subprocess.unref();

    // Kill the subprocess
    if (pid) {
      process.kill(pid, 'SIGINT');
    }
    
    logger.debug("Subprocess is already running, killing it")
  }
  
  if (subprocess && cachedProcessData !== null) {
    return {
      port: cachedProcessData.port,
      secret: cachedProcessData.secret
    }
  }

  const correctedPath = javaPath.replace(/\\/g, "/");
  logger.debug("Starting subprocess", correctedPath, argsList)

  const electronPid = process.pid;
  argsList.push(...["--pid", "" + electronPid])

  // Spawn the process so it can run in the background and capture the output
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      reject("Subprocess timed out")
    }, 30_000) // 30 seconds

    const mappedEnv = env.reduce((curr, itt) => {
      const [key, value] = itt.split("=");
      curr[key] = value;
      return curr;
    }, {} as Record<string, string>)

    logger.debug("Mapped env", mappedEnv)
    logger.debug("Args list", argsList)

    appCommunicationSetup = false;
    subprocess = spawn(correctedPath, argsList, {
      detached: true,
      stdio: ['ignore', 'pipe', 'pipe'], // ignore stdin, pipe stdout and stderr
      env: {
        ...process.env,
        ...mappedEnv,
      },
      cwd: appHome
    });

    subprocess.stderr?.on('data', (data) => {
      let outputData = data.toString();
      if (outputData.endsWith("\n")) {
        outputData = outputData.slice(0, -1);
      }
      logger.debug("Subprocess stderr", outputData)
    });

    subprocess.stdout?.on('data', (data) => {
      let outputData = data.toString();
      if (outputData.endsWith("\n")) {
        outputData = outputData.slice(0, -1);
      }

      logger.debug("Subprocess stdout", outputData)
      if (!appCommunicationSetup) {
        if (data.includes("{T:CI")) {
          const regex = /{p:([0-9]+);s:([^}]+)}/gi;
          const matches = regex.exec(data.toString());

          if (matches !== null) {
            const [, port, secret] = matches;
            logger.debug("Found port and secret", port, secret)
            appCommunicationSetup = true;
            clearTimeout(timeout);
            
            if (subprocess?.pid) {
              cachedProcessData = {
                pid: subprocess.pid,
                port: parseInt(port),
                secret: secret
              };
              
              logger.debug("Cached process data", cachedProcessData)
            }
            
            resolve({
              port,
              secret
            })
          }
        }
      }
    });

    subprocess?.on('error', (err) => {
      logger.error("Subprocess error", err)
    });

    subprocess.on('exit', (code, signal) => {
      logger.debug("Subprocess exited", code, signal)
    });

    subprocess?.on('close', (code) => {
      logger.debug("Subprocess closed", code)
    });
  })
});

async function createPreLaunchWindow(source: string) {
  logger.debug("Creating prelaunch window", source)
  
  prelaunchWindow = new BrowserWindow({
    title: 'FTB Launch',
    minWidth: 300,
    minHeight: 400,
    width: 300,
    height: 400,
    maxWidth: 300,
    maxHeight: 400,
    resizable: false,
    frame: false,
    titleBarStyle: 'hidden',
    show: false,
    backgroundColor: '#151515',
    // Remove close button on mac
    closable: false,
    minimizable: false,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    },
  });

  if (process.env.WEBPACK_DEV_SERVER_URL) {
    logger.debug("Loading dev server url")
    await prelaunchWindow.loadURL(`${process.env.WEBPACK_DEV_SERVER_URL as string}/prelaunch.html`);

    if (process.env.NODE_ENV !== "production") {
      logger.debug("Opening dev tools for prelaunch window")
      prelaunchWindow.webContents.openDevTools({
        mode: 'detach',
      });
    }
  } else {
    logger.debug("Loading app from built files")
    createProtocol(protocolSpace)
    await prelaunchWindow.loadURL(`${protocolSpace}://./prelaunch.html`);
  }

  prelaunchWindow.on("ready-to-show", () => {
    // Try and force focus on the prelaunchWindow
    prelaunchWindow?.show();
  });
  
  prelaunchWindow.on('closed', () => {
    prelaunchWindow = null;
  });
}

ipcMain.handle('updater:check-for-update', async () => {
  if (checkUpdaterLock) {
    logger.debug("Updater is already checking for updates, returning")
    return false;
  }
  
  checkUpdaterLock = true;
  const result = await autoUpdater.checkForUpdates();
  
  if (result?.downloadPromise) {
    logger.debug("Waiting for download promise")
    const version = await result.downloadPromise;
    logger.debug("Download promise resolved", version)
    checkUpdaterLock = false;
    return true;
  }

  checkUpdaterLock = false;
  return false;
})

ipcMain.handle('app:launch', async () => {
  logger.debug("Launching app")
  if (win) {
    // This shouldn't happen, but if it does, just close the window
    win.close();
  }
  
  createWindow().catch(e => logger.error("Failed to create window", e));
  // Kill the prelaunch window
  if (prelaunchWindow) {
    prelaunchWindow.destroy()
  }
})

ipcMain.handle('app:quit-and-install', async () => {
  updateApp("QuitAndInstall")
});

function updateApp(source: string) {
  logger.debug("Quitting and installing app from: ", source)

  // Get all known windows and close them
  logger.debug("Removing all listeners and closing windows");
  app.removeAllListeners('window-all-closed');
  
  logger.debug("Closing all windows")
  BrowserWindow.getAllWindows().forEach((window) => {
    logger.debug("Closing window", window.id)
    window.removeAllListeners('close');
    window.destroy();
  });
  
  logger.debug("Quitting app")
  autoUpdater.quitAndInstall();
}

async function createWindow(reset = false) {
  
  logger.debug("Creating main window")
  let useSystemFrame = false;
  if (usingLegacySettings) {
    useSystemFrame = appSettings?.useSystemWindowStyle === "true" ?? false;
  } else {
    useSystemFrame = appSettings.appearance.useSystemWindowStyle
  }
  
  win = new BrowserWindow({
    title: 'FTB App',

    // Size Settings
    minWidth: 1220,
    minHeight: 895,
    width: 1545,
    height: 900,
    frame: useSystemFrame,
    titleBarStyle: useSystemFrame ? 'default' : 'hidden',
    autoHideMenuBar: true,
    show: false,
    backgroundColor: '#2a2a2a',
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false,
      disableBlinkFeatures: 'Auxclick',
      webSecurity: false,
    },
  });
  
  win.once('ready-to-show', async () => {
    // We update system frame but also the localStorage for remounts on the frontend
    await win?.webContents.executeJavaScript(`
      localStorage.setItem("useSystemFrame", "${useSystemFrame ? 'true' : 'false'}")
      document.body.classList.add('loaded')
    `.trim());

    // Try and force focus on the window
    win?.show();
  });
  
  win.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url);
    return { action: 'deny' }
  })

  win.webContents.session.webRequest.onBeforeSendHeaders((details, callback) => {
    callback({ requestHeaders: { Origin: '*', ...details.requestHeaders } });
  });

  win.webContents.session.webRequest.onHeadersReceived((details, callback) => {
    callback({
      responseHeaders: {
        'Access-Control-Allow-Origin': ['*'],
        ...details.responseHeaders,
      },
    });
  });
  
  if (process.env.WEBPACK_DEV_SERVER_URL) {
    logger.debug("Loading dev server url")
    await win.loadURL(process.env.WEBPACK_DEV_SERVER_URL as string);
    
    if (process.env.NODE_ENV !== "production") {
      logger.debug("Opening dev tools for main window")
      win.webContents.openDevTools({
        mode: 'detach',
      });
    }
  } else {
    logger.debug("Loading app from built files")
    createProtocol(protocolSpace)
    await win.loadURL(`${protocolSpace}://./index.html`);
  }

  if (reset) {
    preserveSubprocess = false;
  }

  win.on('closed', () => {
    // Kill the subprocess if it's running
    if (subprocess !== null && !preserveSubprocess) {
      subprocess.kill();
    }
    win = null;
  });
}

app.on('ready', async () => {
  if (isDevelopment) {
    await install(VUEJS_DEVTOOLS);
  }
});

app.on('window-all-closed', () => {
  // if (process.platform !== 'darwin') {
  app.quit();
  // }
});

app.on('activate', () => {
  if (!prelaunchWindow && !win) {
    createPreLaunchWindow("AppActivate");
  }
});

app.commandLine.appendSwitch('disable-features', 'OutOfBlinkCors');

const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
  logger.debug("Preventing extra windows of the app from loading")
  app.quit();
} else {
  logger.debug("Lock found, let's try and focus the app")
  app.on('second-instance', (event, commandLine) => {
    // Someone tried to run a second instance, we should focus our window.
    // log.info(`Event: ${event.s}`)
    if (win) {
      if (win.isMinimized()) {
        logger.debug("Window is minimized, restoring")
        win.restore();
      }
      win.focus();
      commandLine.forEach((c) => {
        logger.debug("Command line arg", c)
        if (c.indexOf('ftb://') !== -1) {
          logger.debug("Sending protocol url to frontend")
          // @ts-ignore
          win.webContents.send('parseProtocolURL', c);
        }
      });
    }
  });

  app.on('ready', async () => {
    logger.info("App is ready")
    createPreLaunchWindow("AppReady");
    session.defaultSession.webRequest.onHeadersReceived((details, callback) => {
      if (details.url.indexOf('twitch.tv') !== -1) {
        if (details.responseHeaders) {
          if (details.responseHeaders['Content-Security-Policy'] !== undefined) {
            details.responseHeaders['Content-Security-Policy'] = [];
          }
        }
      }
      callback({ responseHeaders: details.responseHeaders });
    });
  });
}

if (isDevelopment) {
  if (process.platform === 'win32') {
    process.on('message', (data) => {
      if (data === 'graceful-exit') {
        app.quit();
      }
    });
  } else {
    process.on('SIGTERM', () => {
      app.quit();
    });
  }
}

function loadChannel() {
  try {
    const channelFile = path.join(appHome, 'storage', 'electron-settings.json');
    if (!fs.existsSync(channelFile)) {
      return 'stable';
    }

    try {
      const channelData = JSON.parse(fs.readFileSync(channelFile, 'utf-8'));
      if (channelData.channel) {
        return channelData.channel;
      }
    } catch (e) {
      logger.error("Failed to load channel data", e)
    }

  } catch (e) {
    logger.error("Failed to load channel", e)
  }
  
  return 'stable';
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
    logger.error("Failed to update channel", e)
  }
}