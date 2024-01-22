'use strict';
import {app, BrowserWindow, dialog, ipcMain, protocol, session, shell} from 'electron';
import path from 'path';
import os from 'os';
import fs from 'fs';
import * as electronLogger from 'electron-log';
import install, {VUEJS_DEVTOOLS} from 'electron-devtools-installer';
import {createProtocol} from 'vue-cli-plugin-electron-builder/lib';
import {createLogger} from '@/core/logger';
import https from 'https';
import AdmZip from 'adm-zip';
import {ChildProcess, execSync, spawn} from 'child_process';

const protocolSpace = 'ftb';
const logger = createLogger('background.ts');

function getAppHome() {
  if (os.platform() === "darwin") {
    return path.join(os.homedir(), 'Library', 'Application Support', '.ftba');
  } else {
    return path.join(os.homedir(), '.ftba');
  }
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

const appHome = getAppHome();
logger.debug('App home is', appHome)

let usingLegacySettings = false;
const appSettingsPathLegacy = path.join(appHome, 'bin', 'settings.json');
const appSettingsPath = path.join(appHome, "storage", 'settings.json');
let appSettings = getAppSettings(appSettingsPath);
if (appSettings === null) {
  logger.debug("App settings not found, trying legacy settings")
  usingLegacySettings = true;
  appSettings = getAppSettings(appSettingsPathLegacy);
}

electronLogger.transports.file.resolvePath = (variables, message) => 
  path.join(appHome, 'logs', 'ftb-app-electron.log');

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
// let friendsWindow: BrowserWindow | null;

let protocolURL: string | null;

for (let i = 0; i < process.argv.length; i++) {
  if (process.argv[i].indexOf(protocolSpace) !== -1) {
    protocolURL = process.argv[i];
    break;
  }
}

declare const __static: string;

let userData: any;

ipcMain.on('appReady', async (event) => {
  if (protocolURL !== null) {
    event.reply('parseProtocolURL', protocolURL);
  }
});

ipcMain.on("restartApp", () => {
  reloadMainWindow()
});

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

  const result = await dialog.showOpenDialog(win, {
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
  
  logger.info("Reloading main window")
  
  // Create a tmp window to keep the app alive
  const tmpWindow = new BrowserWindow({
    show: false,
  });

  // Enable the windows frame
  win.destroy()
  await createWindow()

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

ipcMain.handle('selectFile', async (event) => {
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

ipcMain.on('quit_app', (event, data) => {
  logger.debug("Quitting app")
  process.exit(1);
});

ipcMain.on('logout', (event, data) => {
  // if (friendsWindow) {
  //   friendsWindow.close();
  // }
  
  logger.debug("Logging out from the frontend")
  userData = undefined;
});


ipcMain.on('openLink', (event, data) => {
  shell.openExternal(data);
});

ipcMain.on('openDevTools', (event, data) => {
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

ipcMain.handle("startSubprocess", async (event, args) => {
  if (process.env.NODE_ENV !== 'production') {
    logger.debug("Not starting subprocess in dev mode")
    return;
  }
  
  const javaPath = args.javaPath;
  const argsList = args.args;
  
  if (subprocess !== null) {
    logger.debug("Subprocess is already running, killing it")
    subprocess.kill();
  }
  
  logger.debug("Starting subprocess", javaPath, argsList)
  // Spawn the process so it can run in the background and capture the output
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      reject("Subprocess timed out")
    }, 30_000) // 30 seconds
    
    subprocess = spawn(javaPath, argsList, {
      detached: true,
      stdio: 'pipe'
    });

    subprocess.stderr?.on('data', (data) => {
      logger.debug("Subprocess stderr", data.toString())
    });

    subprocess.stdout?.on('data', (data) => {
      logger.debug("Subprocess stdout", data.toString())
      if (!appCommunicationSetup) {
        if (data.includes("{T:CI")) {
          const regex = /{p:([0-9]+);s:([^}]+)}/gi;
          const matches = regex.exec(data.toString());
          
          if (matches !== null) {
            const [, port, secret] = matches;
            logger.debug("Found port and secret", port, secret)
            appCommunicationSetup = true;
            clearTimeout(timeout);
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

    subprocess?.on('close', (code) => {
      logger.debug("Subprocess closed", code)
    });
  })
});

// function createFriendsWindow() {
//   if (friendsWindow !== null && friendsWindow !== undefined) {
//     friendsWindow.focus();
//     if (win) {
//       win.webContents.send('setFriendsWindow', true);
//     }
//     return;
//   }
//   friendsWindow = new BrowserWindow({
//     title: 'FTB App',
//
//     // Other
//     icon: path.join(__static, 'favicon.ico'),
//     // Size Settings
//     minWidth: 300,
//     minHeight: 626,
//     // maxWidth: 1000,
//     // maxHeight: 626,
//     height: 626,
//     width: 300,
//     frame: false,
//     titleBarStyle: 'hidden',
//     backgroundColor: '#313131',
//     webPreferences: {
//       nodeIntegration: true,
//       contextIsolation: false,
//       disableBlinkFeatures: 'Auxclick',
//     },
//   });
//
//   friendsWindow.webContents.setWindowOpenHandler((details) => {
//     shell.openExternal(details.url);
//     return { action: 'deny' }
//   })
//
//   if (process.env.WEBPACK_DEV_SERVER_URL) {
//     friendsWindow.loadURL(`${process.env.WEBPACK_DEV_SERVER_URL as string}#chat`);
//     if (!process.env.IS_TEST) {
//       friendsWindow.webContents.openDevTools();
//     }
//   } else {
//     friendsWindow.loadURL('app://./index.html#chat');
//   }
//   if (win) {
//     win.webContents.send('setFriendsWindow', true);
//   }
//   friendsWindow.on('closed', () => {
//     friendsWindow = null;
//     if (win) {
//       win.webContents.send('setFriendsWindow', false);
//     }
//   });
// }

async function createWindow() {
  logger.debug("Creating main window")
  let useSystemFrame = false;
  if (usingLegacySettings) {
    useSystemFrame = appSettings?.useSystemWindowStyle === "true" ?? false;
  } else {
    useSystemFrame = appSettings.appearance.useSystemWindowStyle
  }
  
  win = new BrowserWindow({
    title: 'FTB App',

    // Other
    icon: path.join(__static, 'favicon.ico'),
    // Size Settings
    minWidth: 1120,
    minHeight: 800,
    width: 1320,
    height: 800,
    frame: useSystemFrame,
    titleBarStyle: useSystemFrame ? 'default' : 'hidden',
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false,
      disableBlinkFeatures: 'Auxclick',
      webSecurity: false,
    },
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
      logger.debug("Opening dev tools")
      win.webContents.openDevTools({
        mode: 'detach',
      });
    }

    win.blur();
    setTimeout(() => {
      win?.focus();
    }, 100)
  } else {
    logger.debug("Loading app from built files")
    createProtocol(protocolSpace)
    await win.loadURL(`${protocolSpace}://./index.html`);
  }

  win.on('closed', () => {
    // Kill the subprocess if it's running
    if (subprocess !== null) {
      subprocess.kill();
    }
    win = null;
    // if (friendsWindow) {
    //   friendsWindow.close();
    // }
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
  if (win === null) {
    createWindow();
  }
});

app.commandLine.appendSwitch('disable-features', 'OutOfBlinkCors');

const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
  logger.debug("Preventing extra windows of the app from loading")
  app.quit();
} else {
  logger.debug("Lock found, let's try and focus the app")
  app.on('second-instance', (event, commandLine, workingDirectory) => {
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
    createWindow();
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
