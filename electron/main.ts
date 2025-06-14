import { app, BrowserWindow, ipcMain, shell } from 'electron';
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import { autoUpdater } from 'electron-updater';
import { getAppHome } from '../src/utils/nuturalHelpers.ts';
import os from 'os';
import fs from 'fs';
import { ChildProcess, spawn } from 'child_process';
import log from 'electron-log/main';

autoUpdater.logger = log;

export const appHome = logAndReturn(getAppHome(os.platform(), os.homedir(), path.join), "App home directory is: ");

log.transports.file.resolvePathFn = () => path.join(appHome, 'logs', 'electron-main.log');
log.initialize();

const __dirname = logAndReturn(path.dirname(fileURLToPath(import.meta.url)), "Resolved __dirname to: ");

let openDebugTools = !!process.env.VITE_DEV_SERVER_URL;
for (let i = 0; i < process.argv.length; i++) {
  if (process.argv[i] === '--open-dev-tools') {
    log.info("Debug tools flag found in args, opening dev tools");
    openDebugTools = true;
    break;
  }
}

log.log("FTB App starting...")
import './events.ts'

const protocolSpace = 'ftb';
if (!import.meta.env.PROD && process.platform === 'win32') {
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
let preserveSubprocess = false;

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

process.env.APP_ROOT = logAndReturn(path.join(__dirname, '..'), "App root directory is: ");

// 🚧 Use ['ENV_NAME'] avoid vite:define plugin - Vite@2.x
export const VITE_DEV_SERVER_URL = process.env['VITE_DEV_SERVER_URL']
export const MAIN_DIST = path.join(process.env.APP_ROOT, 'dist-electron')
export const RENDERER_DIST = path.join(process.env.APP_ROOT, 'dist')

process.env.VITE_PUBLIC = VITE_DEV_SERVER_URL ? path.join(process.env.APP_ROOT, 'public') : RENDERER_DIST

//#region AutoUpdater Setup
autoUpdater.allowDowngrade = true;
autoUpdater.channel = logAndReturn(loadChannel(), "Loaded auto updater channel: ");

autoUpdater.on('checking-for-update', () => LogAndEmit.create('updater:checking-for-update').execute());
autoUpdater.on("update-cancelled", (info) => LogAndEmit.create('updater:update-cancelled').meta(info).execute());
autoUpdater.on('update-available', (info) => LogAndEmit.create('updater:update-available').meta(info).args(info.version).execute());
autoUpdater.on('update-not-available', (info) => LogAndEmit.create('updater:update-not-available').meta(info).execute());
autoUpdater.on('error', (error, message) => LogAndEmit.create('updater:error').meta({error: error, message: message}).execute());
autoUpdater.on('download-progress', (progress) => LogAndEmit.create('updater:download-progress')
  .meta(process)
  .args({
    total: progress.total,
    delta: progress.delta,
    transferred: progress.transferred,
    percent: progress.percent,
    bytesPerSecond: progress.bytesPerSecond
  })
  .execute());

autoUpdater.on('update-downloaded', (event) => {
  LogAndEmit.create('updater:update-downloaded')
    .meta(event)
    .execute()
});

ipcMain.on("updater:download-update", () => {
  autoUpdater.downloadUpdate().catch((e) => log.error("Failed to download update", e));
})
//#endregion

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

let subprocess: ChildProcess | null = null;
export let win: BrowserWindow | null;
export let prelaunchWindow: BrowserWindow | null = null;

ipcMain.on('action/launch-app', async () => {
  log.debug("Launching app")
  if (win) {
    // This shouldn't happen, but if it does, just close the window
    win.close();
  }

  createWindow().catch(e => log.error("Failed to create window", e));
  // Kill the prelaunch window
  if (prelaunchWindow) {
    prelaunchWindow.destroy()
  }
})

async function createPreLaunchWindow() {
  if (prelaunchWindow) {
    prelaunchWindow.destroy();
    prelaunchWindow = null;
    log.log("Prelaunch window already exists, closing it")
  }
  
  log.debug("Creating prelaunch window")
  
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
    prelaunchWindow.loadURL(VITE_DEV_SERVER_URL + "prelaunch.html").catch(log.warn)
  } else {
    // win.loadFile('dist/index.html')
    prelaunchWindow.loadFile(path.join(RENDERER_DIST, 'prelaunch.html')).catch(log.warn)
  }

  prelaunchWindow.on("ready-to-show", () => {
    // Try and force focus on the prelaunchWindow
    prelaunchWindow?.show();
    if (!prelaunchWindow?.isFocused()) {
      prelaunchWindow?.focus();
    }
    
    if (openDebugTools) {
      log.log("Opening dev tools")
      prelaunchWindow?.webContents.openDevTools();
    }
  });

  prelaunchWindow.on('closed', () => {
    prelaunchWindow = null;
  });
}

ipcMain.on("prelaunch/im-ready", async () => {
  if (VITE_DEV_SERVER_URL) {
    LogAndEmit.create("updater:update-not-available").meta("No updates available as we are in dev mode").execute()
    return;
  }
  
  log.info("Prelaunch window is ready, checking for updates");
  autoUpdater.checkForUpdates()
    .catch(e => log.error("Failed to check for updates", e));
});

async function createWindow() {
  log.log("Creating main window")
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
  
  win.on("ready-to-show", () => {
    if (openDebugTools) {
      log.log("Opening dev tools on main window")
      win?.webContents.openDevTools();
    }
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
  
  win.on('closed', () => {
    // Kill the subprocess if it's running
    if (subprocess !== null && !preserveSubprocess) {
      subprocess.kill();
    }
    win = null;
  });
}

export async function reloadMainWindow() {
  if (!win) {
    return;
  }

  preserveSubprocess = true;
  log.info("Reloading main window")

  // Create a tmp window to keep the app alive
  const tmpWindow = new BrowserWindow({
    show: false,
  });

  // Reset the port and secret scanning
  appCommunicationSetup = false;

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
    log.log("Activated")
    await createPreLaunchWindow()
  }
})

app.whenReady().then(() => {
  createPreLaunchWindow();
  
  // protocol.handle(protocolSpace, async (event) => {
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
  if (webContents && openDebugTools) {
    log.info("Render process gone, opening devtools")
    webContents.openDevTools();
  }
});

app.on("open-url", async (_, customSchemeData) => {
  if (win) {
    win.webContents.send('parseProtocolURL', customSchemeData);
  }
});

// app.on('activate', () => {
//   if (!prelaunchWindow && !win) {
//     createPreLaunchWindow("AppActivate").catch(log.error)
//   }
// });

const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
  log.debug("Preventing extra windows of the app from loading", import.meta.env.MODE)
  if (!import.meta.env.PROD) {
    // Find the other instance and yeet it
    app.quit();
  }
} else {
  log.debug("Lock found, let's try and focus the app")
  app.on('second-instance', (event, commandLine) => {
    // Someone tried to run a second instance, we should focus our window.
    log.info(`Event: ${event}, commandLine: ${commandLine}`);
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
          log.debug("Window is minimized, restoring")
          win.restore();
        }
        win.focus();
        commandLine.forEach((c) => {
          log.debug("Command line arg", c)
          if (c.indexOf('ftb://') !== -1) {
            log.debug("Sending protocol url to frontend")
            // @ts-ignore
            win.webContents.send('parseProtocolURL', c);
          }
        });
      }
    }
  });
}

ipcMain.handle("startSubprocess", async (_, args) => {
  log.debug("Starting subprocess")
  if (!import.meta.env.PROD) {
    log.debug("Not starting subprocess in dev mode")
    return;
  }
  
  log.log("Starting subprocess")

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
        log.error("Failed to kill subprocess", e)
      }
    }

    log.debug("Subprocess is already running, killing it")
  }

  if (subprocess && cachedProcessData !== null) {
    return {
      port: cachedProcessData.port,
      secret: cachedProcessData.secret
    }
  }

  const correctedPath = javaPath.replace(/\\/g, "/");
  log.debug("Starting subprocess", correctedPath, argsList)

  const electronPid = process.pid;
  argsList.push(...["--pid", "" + electronPid])
  if (process.argv.includes("ignore-pid-checks")) {
    argsList.push("--ignore-pid-checks")
  }

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

    log.debug("Mapped env", mappedEnv)
    log.debug("Args list", argsList)

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
      log.debug("Subprocess stderr", outputData)
    });

    subprocess.stdout?.on('data', (data) => {
      let outputData = data.toString();
      if (outputData.endsWith("\n")) {
        outputData = outputData.slice(0, -1);
      }

      log.debug("Subprocess stdout", outputData)
      if (!appCommunicationSetup) {
        if (data.includes("Backend Ready! Port=")) {
          const port = parseInt(outputData.match(/Port=(\d+)/)![1]);
          const secret = outputData.match(/OneTimeToken=(\w+-\w+-\w+-\w+-\w+)/)![1];

          if (!(!port || !secret || isNaN(port))) {
            log.debug('Found port and secret', port, secret);
            appCommunicationSetup = true;
            clearTimeout(timeout);

            if (subprocess?.pid) {
              cachedProcessData = {
                pid: subprocess.pid,
                port: port,
                secret: secret,
              };

              log.debug('Cached process data', cachedProcessData);
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
      log.error("Subprocess error", err)
    });

    subprocess.on('exit', (code, signal) => {
      log.debug("Subprocess exited", code, signal)
    });

    subprocess?.on('close', (code) => {
      log.debug("Subprocess closed", code)
    });
  })
});

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
      log.error("Failed to load channel data", e)
    }

  } catch (e) {
    log.error("Failed to load channel", e)
  }

  return 'stable';
}

export function updateApp(source: string) {
  log.debug("Quitting and installing app from: ", source)

  // Get all known windows and close them
  log.debug("Removing all listeners and closing windows");
  app.removeAllListeners('window-all-closed');

  log.debug("Closing all windows")
  BrowserWindow.getAllWindows().forEach((window) => {
    log.debug("Closing window", window.id)
    window.removeAllListeners('close');
    window.destroy();
  });

  log.debug("Quitting app")
  autoUpdater.quitAndInstall();
}

class LogAndEmit {
  private readonly eventName: string;
  private _args: any[] = [];
  private _meta: any | null = {};
  
  private constructor(name: string) {
    this.eventName = name;
  }
  
  public static create(name: string) {
    return new LogAndEmit(name);
  }
  
  public args(...args: any[]) {
    this._args = args;
    return this;
  }
  
  public meta(meta: any) {
    this._meta = meta;
    return this;
  }
  
  public execute() {
    log.debug("Emitting event", this.eventName, this._args, this._meta);
    if (!prelaunchWindow && !win) {
      log.warn("No window to send event to, skipping", this.eventName);
      return;
    }
    
    const target = prelaunchWindow ? prelaunchWindow : win;
    if (!target) {
      log.warn("No target window to send event to, skipping", this.eventName);
      return;
    }
    
    target.webContents.send(this.eventName, ...this._args);
  }
}

function logAndReturn<T>(value: T, message: string): T {
  log.debug(message, value);
  return value;
}

// async function logAndReturnAsync<T>(value: T, message: string): Promise<T> {
//   log.debug(message, value);
//   return value;
// }