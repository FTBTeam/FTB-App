import { app, BrowserWindow, ipcMain, shell } from 'electron';
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import { autoUpdater } from 'electron-updater';
import { getAppHome } from '../src/utils/nuturalHelpers.ts';
import os from 'os';
import fs from 'fs';
import { ChildProcess, spawn } from 'child_process';

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export const appHome = getAppHome(os.platform(), os.homedir(), path.join);

// const console = createLogger('main.ts');
// autoUpdater.console = electronLogger;

console.log("FTB App starting...")
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
autoUpdater.on("update-cancelled", (info) => logAndEmit('updater:update-cancelled', info));
autoUpdater.on('update-available', (info) => logAndEmit('updater:update-available', info));
autoUpdater.on('update-not-available', (info) => logAndEmit('updater:update-not-available', info));
autoUpdater.on('error', (error, message) => logAndEmit('updater:error', JSON.stringify(error), message));
autoUpdater.on('download-progress', (progress) => logAndEmit('updater:download-progress', progress));
autoUpdater.on('update-downloaded', (event) => {
  console.debug("Update downloaded", event)
  ipcMain.emit('updater:update-downloaded');
});

console.debug('App home is', appHome)

autoUpdater.allowDowngrade = true;
autoUpdater.channel = loadChannel();

let subprocess: ChildProcess | null = null;
export let win: BrowserWindow | null;
export let prelaunchWindow: BrowserWindow | null = null;

ipcMain.on('action/launch-app', async () => {
  console.debug("Launching app")
  if (win) {
    // This shouldn't happen, but if it does, just close the window
    win.close();
  }

  createWindow().catch(e => console.error("Failed to create window", e));
  // Kill the prelaunch window
  if (prelaunchWindow) {
    prelaunchWindow.destroy()
  }
})

async function createPreLaunchWindow() {
  if (prelaunchWindow) {
    prelaunchWindow.destroy();
    prelaunchWindow = null;
    console.log("Prelaunch window already exists, closing it")
  }
  
  console.debug("Creating prelaunch window")
  
  prelaunchWindow = new BrowserWindow({
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

  if (VITE_DEV_SERVER_URL) {
    prelaunchWindow.loadURL(VITE_DEV_SERVER_URL + "prelaunch.html").catch(console.warn)
  } else {
    // win.loadFile('dist/index.html')
    prelaunchWindow.loadFile(path.join(RENDERER_DIST, 'prelaunch.html')).catch(console.warn)
  }

  prelaunchWindow.on("ready-to-show", () => {
    // Try and force focus on the prelaunchWindow
    prelaunchWindow?.show();
    if (!prelaunchWindow?.isFocused()) {
      prelaunchWindow?.focus();
    }
    
    if (VITE_DEV_SERVER_URL) {
      console.log("Opening dev tools")
      prelaunchWindow?.webContents.openDevTools();
    }
  });

  prelaunchWindow.on('closed', () => {
    prelaunchWindow = null;
  });
}

ipcMain.on("prelaunch/im-ready", async () => {
  if (VITE_DEV_SERVER_URL) {
    logAndEmit("updater:update-not-available", "No updates available as we are in dev mode")
    return;
  }
  
  // Check for updates
  // TODO: Add a way to disable this
});

async function createWindow() {
  console.log("Creating main window")
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
        win?.loadFile(path.join(RENDERER_DIST, 'index.html')).catch(console.error)
        console.log("Redirecting to app URL");
      }
    }
  });

  // TODO: Add back
  win.on('closed', () => {
    // Kill the subprocess if it's running
    // if (subprocess !== null && !preserveSubprocess) {
    //   subprocess.kill();
    // }
    win = null;
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
    prelaunchWindow = null;
  }
})

app.on('activate', async () => {
  // On OS X it's common to re-create a window in the app when the
  // dock icon is clicked and there are no other windows open.
  if (BrowserWindow.getAllWindows().length === 0) {
    console.log("Activated")
    await createPreLaunchWindow()
  }
})

app.whenReady().then(createPreLaunchWindow)
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

const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
  console.debug("Preventing extra windows of the app from loading", process.env.NODE_ENV)
  if (process.env.NODE_ENV === 'production') {
    app.quit();
  }
} else {
  console.debug("Lock found, let's try and focus the app")
  app.on('second-instance', (event, commandLine) => {
    // Someone tried to run a second instance, we should focus our window.
    console.info(`Event: ${event}, commandLine: ${commandLine}`);
    if (prelaunchWindow) {
      if (prelaunchWindow.isMinimized()) {
        prelaunchWindow.restore()
      }
      
      if (!prelaunchWindow.isFocused()) {
        prelaunchWindow.focus();
      }
    } else {
      if (win) {
        if (win.isMinimized()) {
          console.debug("Window is minimized, restoring")
          win.restore();
        }
        win.focus();
        commandLine.forEach((c) => {
          console.debug("Command line arg", c)
          if (c.indexOf('ftb://') !== -1) {
            console.debug("Sending protocol url to frontend")
            // @ts-ignore
            win.webContents.send('parseProtocolURL', c);
          }
        });
      }
    }
  });
}

ipcMain.handle("startSubprocess", async (_, args) => {
  if (process.env.NODE_ENV !== 'production') {
    console.debug("Not starting subprocess in dev mode")
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
      try {
        process.kill(pid, 'SIGINT');
      } catch (e) {
        console.error("Failed to kill subprocess", e)
      }
    }

    console.debug("Subprocess is already running, killing it")
  }

  if (subprocess && cachedProcessData !== null) {
    return {
      port: cachedProcessData.port,
      secret: cachedProcessData.secret
    }
  }

  const correctedPath = javaPath.replace(/\\/g, "/");
  console.debug("Starting subprocess", correctedPath, argsList)

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

    console.debug("Mapped env", mappedEnv)
    console.debug("Args list", argsList)

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
      console.debug("Subprocess stderr", outputData)
    });

    subprocess.stdout?.on('data', (data) => {
      let outputData = data.toString();
      if (outputData.endsWith("\n")) {
        outputData = outputData.slice(0, -1);
      }

      console.debug("Subprocess stdout", outputData)
      if (!appCommunicationSetup) {
        if (data.includes("Backend Ready! Port=")) {
          const port = parseInt(outputData.match(/Port=(\d+)/)![1]);
          const secret = outputData.match(/OneTimeToken=(\w+-\w+-\w+-\w+-\w+)/)![1];

          if (!(!port || !secret || isNaN(port))) {
            console.debug('Found port and secret', port, secret);
            appCommunicationSetup = true;
            clearTimeout(timeout);

            if (subprocess?.pid) {
              cachedProcessData = {
                pid: subprocess.pid,
                port: port,
                secret: secret,
              };

              console.debug('Cached process data', cachedProcessData);
            }

            resolve({
              port,
              secret,
            });
          }
        }
      }
    });

    subprocess?.on('error', (err) => {
      console.error("Subprocess error", err)
    });

    subprocess.on('exit', (code, signal) => {
      console.debug("Subprocess exited", code, signal)
    });

    subprocess?.on('close', (code) => {
      console.debug("Subprocess closed", code)
    });
  })
});

function logAndEmit(event: string, ...args: any[]) {
  console.debug("Emitting downloader event", event, args)
  if (prelaunchWindow) {
    prelaunchWindow.webContents.send(event, ...args);
  }
  
  if (win) {
    win.webContents.send(event, ...args);
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
      console.error("Failed to load channel data", e)
    }

  } catch (e) {
    console.error("Failed to load channel", e)
  }

  return 'stable';
}

export function updateApp(source: string) {
  console.debug("Quitting and installing app from: ", source)

  // Get all known windows and close them
  console.debug("Removing all listeners and closing windows");
  app.removeAllListeners('window-all-closed');

  console.debug("Closing all windows")
  BrowserWindow.getAllWindows().forEach((window) => {
    console.debug("Closing window", window.id)
    window.removeAllListeners('close');
    window.destroy();
  });

  console.debug("Quitting app")
  autoUpdater.quitAndInstall();
}