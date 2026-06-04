import path from 'node:path'
import os from 'node:os';
import { fileURLToPath } from 'node:url'

import {app, BrowserWindow, ipcMain, shell} from 'electron';
import { autoUpdater } from 'electron-updater';
import log from 'electron-log/main';

import './events.ts'
import { getAppHome } from '../src/utils/nuturalHelpers.ts';
import {AppData} from "./app";
import {setupUpdater} from "./updater.ts";
import {logAndReturn} from "./helpers/utils.ts";
import {LogAndEmit} from "./helpers/loggingEmitter.ts";
import {startSubprocess} from "./subprocess.ts";

// Constants
const __dirname = logAndReturn(path.dirname(fileURLToPath(import.meta.url)), "Resolved __dirname to: ");

process.env.APP_ROOT = logAndReturn(path.join(__dirname, '..'), "App root directory is: ");

export const VITE_DEV_SERVER_URL = process.env['VITE_DEV_SERVER_URL']
export const RENDERER_DIST = path.join(process.env.APP_ROOT, 'dist')

process.env.VITE_PUBLIC = VITE_DEV_SERVER_URL ? path.join(process.env.APP_ROOT, 'public') : RENDERER_DIST

// App & State
export const appHome = logAndReturn(getAppHome(os.platform(), os.homedir(), path.join), "App home directory is: ");

export const appData: AppData = {
  protocolSpace: 'ftb',
  state: {
    appStarted: false,
    initialProtocolUrl: null,
    ws: null,
    isSubprocessSetup: false,
    preserveSubprocess: false,
  },
  subprocess: null,
  mainWindow: null,
  prelaunchWindow: null,
  options: {
    startInFullscreen: false,
    openDebugTools: !!process.env.VITE_DEV_SERVER_URL,
  },
  paths: {
    home: appHome,
    logFile: path.join(appHome, 'logs', 'electron-main.log')
  }
}

autoUpdater.logger = log;

log.transports.file.resolvePathFn = () => appData.paths.logFile;
log.initialize();

log.info("App has been started with the following:\n" + JSON.stringify(process.argv, null, 2))

// Protocol parsing.
for (let i = 0; i < process.argv.length; i++) {
  if (process.argv[i].indexOf(appData.protocolSpace) !== -1) {
    appData.state.initialProtocolUrl = process.argv[i];
    break;
  }
  
  if (process.argv[i] === '--open-dev-tools') {
    log.info("Debug tools flag found in args, opening dev tools");
    appData.options.openDebugTools = true;
    break;
  }
  
  if (process.argv[i] === '--fullscreen') {
    appData.options.startInFullscreen = true;
  }
}

if (!appData.options.startInFullscreen) {
  // Apparently this being 1 flags as the steamdeck being in gamescope
  if (process.env["SteamDeck"] && process.env["SteamDeck"] === "1") {
    appData.options.startInFullscreen = true;
  }
}

log.log("FTB App starting...")
setupUpdater(appData)

// Register the custom protocol handler for the app
if (!import.meta.env.PROD && process.platform === 'win32') {
  app.setAsDefaultProtocolClient(appData.protocolSpace, process.execPath, [path.resolve(process.argv[1])]);
} else {
  app.setAsDefaultProtocolClient(appData.protocolSpace);
}

/**
 * Fix for SUID sandbox issues on Linux
 * See: https://github.com/electron/electron/issues/17972
 */
if (process.platform == "linux") {
  app.commandLine.appendSwitch("--no-sandbox");
}

log.info("Auto updater config: ")
for (const [key, value] of Object.entries(autoUpdater)) {
  // If it's a function or a property that starts with an underscore, skip it
  if (typeof value === 'function' || key.startsWith('_') || key === 'logger' || key === 'signals' || key.toLowerCase().includes("promise")) {
    continue;
  }
  
  if (typeof value === 'object' && value !== null) {
    try {
      log.info(`  ${key}: ${JSON.stringify(value)}`);
    } catch (e) {
      log.info("  failed to stringify value for key", key, "value type:", typeof value);
    }
    
    continue;
  }
  
  log.info(`  ${key}: ${value} (type: ${typeof value})`);
}

ipcMain.on('action/launch-app', async () => {
  log.debug("Launching app")
  if (appData.mainWindow) {
    // This shouldn't happen, but if it does, just close the window
    appData.mainWindow.close();
  }

  createWindow().catch(e => log.error("Failed to create window", e));
  // Kill the prelaunch window
  if (appData.prelaunchWindow) {
    appData.prelaunchWindow.destroy()
  }
})

async function createPreLaunchWindow() {
  if (appData.prelaunchWindow) {
    appData.prelaunchWindow.destroy();
    appData.prelaunchWindow = null;
    log.log("Prelaunch window already exists, closing it")
  }
  
  log.debug("Creating prelaunch window")
  
  const newWindow = new BrowserWindow({
    title: 'FTB Launch',
    minWidth: 380,
    minHeight: 510,
    width: 380,
    height: 510,
    maxWidth: 380,
    maxHeight: 510,
    resizable: false,
    frame: false,
    titleBarStyle: 'hidden',
    show: false,
    backgroundColor: '#151515',
    trafficLightPosition: {
      x: 1000, y: 0 // Hacks baby! Allows the window to close but removeas the close button from the window
    },
    minimizable: false,
    webPreferences: {
      preload: path.join(__dirname, 'preload.mjs'),
      contextIsolation: true,
      sandbox: false
    },
  });
  
  // Assign the new window to the app data
  appData.prelaunchWindow = newWindow;

  if (VITE_DEV_SERVER_URL) {
    newWindow.loadURL(VITE_DEV_SERVER_URL + "prelaunch.html").catch(log.warn)
  } else {
    // win.loadFile('dist/index.html')
    newWindow.loadFile(path.join(RENDERER_DIST, 'prelaunch.html')).catch(log.warn)
  }

  newWindow.on("ready-to-show", () => {
    // Try and force focus on the newWindow
    newWindow?.show();
    if (!newWindow?.isFocused()) {
      newWindow?.focus();
    }
    
    if (appData.options.openDebugTools) {
      log.log("Opening dev tools")
      newWindow?.webContents.openDevTools();
    }
  });

  newWindow.on('closed', () => {
    appData.prelaunchWindow = null;
  });
  
}

ipcMain.on("prelaunch/im-ready", async () => {
  if (VITE_DEV_SERVER_URL) {
    LogAndEmit.create("updater:update-not-available", appData).meta("No updates available as we are in dev mode").execute()
    return;
  }
  
  log.info("Prelaunch window is ready, checking for updates");
  autoUpdater.checkForUpdates()
    .catch(e => log.error("Failed to check for updates", e));
});

async function createWindow() {
  log.log("Creating main window")
  const newWindow = new BrowserWindow({
    title: 'FTB App',
    // icon: path.join(process.env.VITE_PUBLIC, 'favicon.ico'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.mjs'),
      contextIsolation: true,
      sandbox: false
    },
    autoHideMenuBar: true,
    titleBarStyle: "hidden",
    minWidth: 1220,
    minHeight: 895,
    width: 1545,
    height: 900,
    frame: false,
    backgroundColor: '#2a2a2a',
    ...(appData.options.startInFullscreen ? {
      fullscreen: true,
    } : {})
  })

  // Assign the new window to the app data
  appData.mainWindow = newWindow;
  
  newWindow.on("ready-to-show", () => {
    if (appData.options.openDebugTools) {
      log.log("Opening dev tools on main window")
      newWindow.webContents.openDevTools();
    }

    if (appData.options.startInFullscreen) {
      newWindow.maximize()
    }
  })

  if (VITE_DEV_SERVER_URL) {
    await newWindow.loadURL(VITE_DEV_SERVER_URL)
  } else {
    await newWindow.loadFile(path.join(RENDERER_DIST, 'index.html'))
  }

  newWindow.webContents.setWindowOpenHandler((data) => {
    const {url} = data;
    if (url.startsWith(appData.protocolSpace)) {
      return { action: 'allow' };
    }
    
    shell.openExternal(url);
    return { action: 'deny' };
  });
  
  newWindow.on('closed', () => {
    // Kill the subprocess if it's running
    if (appData.subprocess !== null && !appData.state.preserveSubprocess) {
      appData.subprocess.kill();
    }
    appData.mainWindow = null;
  });
}

export async function reloadMainWindow() {
  if (!appData.mainWindow) {
    return;
  }

  appData.state.preserveSubprocess = true;
  log.info("Reloading main window")

  // Create a tmp window to keep the app alive
  const tmpWindow = new BrowserWindow({
    show: false,
  });

  // Reset the port and secret scanning
  appData.state.isSubprocessSetup = false;

  // Enable the windows frame
  appData.mainWindow.destroy()
  appData.mainWindow = null;
  
  await createWindow()

  // Close the tmp window
  tmpWindow.close();
}

// Quit when all windows are closed, except on macOS. There, it's common
// for applications and their menu bar to stay active until the user quits
// explicitly with Cmd + Q.
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
    
    if (appData.mainWindow) {
      appData.mainWindow.destroy();
      appData.mainWindow = null;
    }
    
    if (appData.prelaunchWindow) {
      appData.prelaunchWindow.destroy();
      appData.prelaunchWindow = null;
    }
    
    if (appData.subprocess) {
      appData.subprocess.kill();
      appData.subprocess = null;
    }
  }
})

app.on('activate', async () => {
  // On OS X it's common to re-create a window in the app when the
  // dock icon is clicked and there are no other windows open.
  if (BrowserWindow.getAllWindows().length === 0) {
    log.log("Activated")
    await createPreLaunchWindow()
  }
})

app.whenReady().then(() => {
  createPreLaunchWindow();
  
  // protocol.handle(appData.protocolSpace, async (event) => {
  //   if (event.url) {
  //   }
  //  
  //   return new Response(null);
  // })
})

app.on('render-process-gone', (_, webContents, details) => {
  log.log('render-process-gone', details);
  
  if (details.reason === 'clean-exit') {
    log.info("Render process exited cleanly, not reloading")
    return;
  }
  
  // yolo, try and open devtools to debug
  if (webContents && appData.options.openDebugTools) {
    log.info("Render process gone, opening devtools")
    webContents.openDevTools();
  }
});

// We either handle the protocol right away, or we store it in the app data until the app is ready and requests it from us.
app.on("open-url", async (e, customSchemeData) => {
  console.log("Received protocol URL", customSchemeData, "app started?", appData.state.appStarted)
  if (appData.state.appStarted && appData.mainWindow) {
    appData.mainWindow.webContents.send('parseProtocolURL', customSchemeData);
  } else {
    e.preventDefault();
    appData.state.initialProtocolUrl = customSchemeData;
  }
});

ipcMain.handle("startSubprocess", async (_, args) => {
  return startSubprocess(appData, args)
});

ipcMain.handle("protocol/get-startup-url", () => {
  return appData.state.initialProtocolUrl;
});

ipcMain.on("app/ready", () => {
  appData.state.appStarted = true;
  log.info("App is ready, appStarted flag set to true")
})

if (!app.requestSingleInstanceLock()) {
  log.debug("Preventing extra windows of the app from loading", import.meta.env.MODE)
  app.quit();
} else {
  log.debug("Lock found, let's try and focus the app")
  app.on('second-instance', (event, commandLine) => {
    // Someone tried to run a second instance, we should focus our window.
    log.info(`Event: ${event}, commandLine: ${commandLine}`);
    if (appData.prelaunchWindow) {
      if (appData.prelaunchWindow.isMinimized()) {
        appData.prelaunchWindow.restore()
      }
      
      if (!appData.prelaunchWindow.isFocused()) {
        appData.prelaunchWindow.focus();
      }
    } else {
      if (appData.mainWindow) {
        if (appData.mainWindow.isMinimized()) {
          log.debug("Window is minimized, restoring")
          appData.mainWindow.restore();
        }
        appData.mainWindow.focus();
        
        // Looks like this is triggered on the app
        commandLine.forEach((c) => {
          log.debug("Command line arg", c)
          if (c.indexOf('ftb://') !== -1) {
            log.debug("Sending protocol url to frontend")
            // @ts-ignore
            appData.mainWindow.webContents.send('parseProtocolURL', c);
          }
        });
      }
    }
  });
}