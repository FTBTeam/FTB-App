import { app, BrowserWindow, ipcMain, shell } from 'electron';
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import { autoUpdater } from 'electron-updater';
import { createLogger } from '../src/core/logger.ts';
import * as electronLogger from 'electron-log';
import { getAppHome } from '../src/nuturalHelpers.ts';
import os from 'os';
import fs from 'fs';
import { ChildProcess, spawn } from 'child_process';

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export const appHome = getAppHome(os.platform(), os.homedir(), path.join);

const logger = createLogger('main.ts');
autoUpdater.logger = electronLogger;

import './events.ts'

const protocolSpace = 'ftb';
if (process.env.NODE_ENV === 'development' && process.platform === 'win32') {
  app.setAsDefaultProtocolClient(protocolSpace, process.execPath, [path.resolve(process.argv[1])]);
} else {
  app.setAsDefaultProtocolClient(protocolSpace);
}

// export let startupProtocolUrl: string | null;

let cachedProcessData = null as {
  pid: number;
  port: number;
  secret: string;
} | null;

let appCommunicationSetup = false;

/**
 * Fix for SUID sandbox issues on Linux
 * See: https://github.com/electron/electron/issues/17972
 */
if (process.platform == "linux") {
  app.commandLine.appendSwitch("--no-sandbox");
}

// for (let i = 0; i < process.argv.length; i++) {
//   if (process.argv[i].indexOf(protocolSpace) !== -1) {
//     startupProtocolUrl = process.argv[i];
//     break;
//   }
// }

process.env.APP_ROOT = path.join(__dirname, '..')

// 🚧 Use ['ENV_NAME'] avoid vite:define plugin - Vite@2.x
export const VITE_DEV_SERVER_URL = process.env['VITE_DEV_SERVER_URL']
export const MAIN_DIST = path.join(process.env.APP_ROOT, 'dist-electron')
export const RENDERER_DIST = path.join(process.env.APP_ROOT, 'dist')

process.env.VITE_PUBLIC = VITE_DEV_SERVER_URL ? path.join(process.env.APP_ROOT, 'public') : RENDERER_DIST

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

logger.debug('App home is', appHome)

autoUpdater.allowDowngrade = true;
autoUpdater.channel = loadChannel();

let subprocess: ChildProcess | null = null;
let win: BrowserWindow | null;
let prelaunchWindow: BrowserWindow | null = null;

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

async function createWindow() {
  win = new BrowserWindow({
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
    backgroundColor: '#2a2a2a'
  })

  if (VITE_DEV_SERVER_URL) {
    await win.loadURL(VITE_DEV_SERVER_URL)
  } else {
    // win.loadFile('dist/index.html')
    await win.loadFile(path.join(RENDERER_DIST, 'index.html'))
  }

  win.webContents.setWindowOpenHandler((data) => {
    const {url} = data;
    if (url.startsWith(protocolSpace)) {
      return { action: 'allow' };
    }
    
    shell.openExternal(url);
    return { action: 'deny' };
  });
  
  // A failsafe for if the main window ever isn't on localhost, it'll be redirected pack
  win.webContents.on('did-navigate', (_, url) => {
    if (!VITE_DEV_SERVER_URL) {
      if (!url.startsWith(protocolSpace)) {
        win?.loadFile(path.join(RENDERER_DIST, 'index.html')).catch(logger.error)
        console.log("Redirecting to app URL");
      }
    }
  });
}

export async function reloadMainWindow() {
  if (!win) {
    return;
  }

  // TODO: Add back
  // preserveSubprocess = true;
  console.info("Reloading main window")

  // Create a tmp window to keep the app alive
  const tmpWindow = new BrowserWindow({
    show: false,
  });

  // Reset the port and secret scanning
  // TODO: Add back
  // appCommunicationSetup = false;

  // Enable the windows frame
  win.destroy()
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
    win = null
  }
})

app.on('activate', async () => {
  // On OS X it's common to re-create a window in the app when the
  // dock icon is clicked and there are no other windows open.
  if (BrowserWindow.getAllWindows().length === 0) {
    await createWindow()
  }
})

app.whenReady().then(createWindow)
app.on("open-url", async (_, customSchemeData) => {
  if (win) {
    win.webContents.send('parseProtocolURL', customSchemeData);
  }
});

// app.on('activate', () => {
//   if (!prelaunchWindow && !win) {
//     createPreLaunchWindow("AppActivate").catch(console.error)
//   }
// });

app.on('window-all-closed', () => {
  app.quit();
});

const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
  logger.debug("Preventing extra windows of the app from loading")
  app.quit();
} else {
  logger.debug("Lock found, let's try and focus the app")
  app.on('second-instance', (_, commandLine) => {
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
}

ipcMain.handle("startSubprocess", async (_, args) => {
  // if (process.env.NODE_ENV !== 'production') {
  //   logger.debug("Not starting subprocess in dev mode")
  //   return;
  // }
  //
  // const javaPath = args.javaPath;
  // const argsList = args.args as string[]
  // const env = args.env as string[]
  //
  // if (subprocess !== null && cachedProcessData === null) {
  //   // Check if the process is still running
  //   let pid = subprocess.pid;
  //   subprocess.unref();
  //
  //   // Kill the subprocess
  //   if (pid) {
  //     try {
  //       process.kill(pid, 'SIGINT');
  //     } catch (e) {
  //       logger.error("Failed to kill subprocess", e)
  //     }
  //   }
  //
  //   logger.debug("Subprocess is already running, killing it")
  // }
  //
  // if (subprocess && cachedProcessData !== null) {
  //   return {
  //     port: cachedProcessData.port,
  //     secret: cachedProcessData.secret
  //   }
  // }
  //
  // const correctedPath = javaPath.replace(/\\/g, "/");
  // logger.debug("Starting subprocess", correctedPath, argsList)
  //
  // const electronPid = process.pid;
  // argsList.push(...["--pid", "" + electronPid])
  //
  // // Spawn the process so it can run in the background and capture the output
  // return new Promise((resolve, reject) => {
  //   const timeout = setTimeout(() => {
  //     reject("Subprocess timed out")
  //   }, 30_000) // 30 seconds
  //
  //   const mappedEnv = env.reduce((curr, itt) => {
  //     const [key, value] = itt.split("=");
  //     curr[key] = value;
  //     return curr;
  //   }, {} as Record<string, string>)
  //
  //   logger.debug("Mapped env", mappedEnv)
  //   logger.debug("Args list", argsList)
  //
  //   appCommunicationSetup = false;
  //   subprocess = spawn(correctedPath, argsList, {
  //     detached: true,
  //     stdio: ['ignore', 'pipe', 'pipe'], // ignore stdin, pipe stdout and stderr
  //     env: {
  //       ...process.env,
  //       ...mappedEnv,
  //     },
  //     cwd: appHome
  //   });
  //
  //   subprocess.stderr?.on('data', (data) => {
  //     let outputData = data.toString();
  //     if (outputData.endsWith("\n")) {
  //       outputData = outputData.slice(0, -1);
  //     }
  //     logger.debug("Subprocess stderr", outputData)
  //   });
  //
  //   subprocess.stdout?.on('data', (data) => {
  //     let outputData = data.toString();
  //     if (outputData.endsWith("\n")) {
  //       outputData = outputData.slice(0, -1);
  //     }
  //
  //     logger.debug("Subprocess stdout", outputData)
  //     if (!appCommunicationSetup) {
  //       if (data.includes("Backend Ready! Port=")) {
  //         const port = parseInt(outputData.match(/Port=(\d+)/)![1]);
  //         const secret = outputData.match(/OneTimeToken=(\w+-\w+-\w+-\w+-\w+)/)![1];
  //
  //         if (!(!port || !secret || isNaN(port))) {
  //           logger.debug('Found port and secret', port, secret);
  //           appCommunicationSetup = true;
  //           clearTimeout(timeout);
  //
  //           if (subprocess?.pid) {
  //             cachedProcessData = {
  //               pid: subprocess.pid,
  //               port: port,
  //               secret: secret,
  //             };
  //
  //             logger.debug('Cached process data', cachedProcessData);
  //           }
  //
  //           resolve({
  //             port,
  //             secret,
  //           });
  //         }
  //       }
  //     }
  //   });
  //
  //   subprocess?.on('error', (err) => {
  //     logger.error("Subprocess error", err)
  //   });
  //
  //   subprocess.on('exit', (code, signal) => {
  //     logger.debug("Subprocess exited", code, signal)
  //   });
  //
  //   subprocess?.on('close', (code) => {
  //     logger.debug("Subprocess closed", code)
  //   });
  // })
});

function logAndEmit(event: string, ...args: any[]) {
  logger.debug("Emitting downloader event", event, args)
  ipcMain.emit(event, ...args);
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

export function updateApp(source: string) {
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