'use strict';
import { app, BrowserWindow, dialog, ipcMain, protocol, session, shell, Menu } from 'electron';
import path from 'path';
import os from 'os';
import fs from 'fs';
import * as log from 'electron-log';
import childProcess from 'child_process';
import Client from './ircshim';
import { FriendListResponse } from './types';
import install, { VUEJS_DEVTOOLS } from 'electron-devtools-installer';
import {createProtocol} from 'vue-cli-plugin-electron-builder/lib';

const protocolSpace = 'ftb';

function getAppHome() {
  if (os.platform() === "darwin") {
    return path.join(os.homedir(), 'Library', 'Application Support', '.ftba');
  } else {
    return path.join(os.homedir(), '.ftba');
  }
}

log.transports.file.resolvePath = (vars, message) => path.join(getAppHome(), 'logs', 'ftb-app-electron.log');

Object.assign(console, log.functions);
(app as any).console = log;

const isDevelopment = process.env.NODE_ENV !== 'production' || process.argv.indexOf('--dev') !== -1;

log.transports.file.resolvePath = (variables, message): string => {
  return path.join(process.execPath.substring(0, process.execPath.lastIndexOf(path.sep)), 'electron.log');
};

protocol.registerSchemesAsPrivileged([{ scheme: protocolSpace, privileges: { secure: true, standard: true } }]);

if (process.env.NODE_ENV === 'development' && process.platform === 'win32') {
  app.setAsDefaultProtocolClient(protocolSpace, process.execPath, [path.resolve(process.argv[1])]);
} else {
  app.setAsDefaultProtocolClient(protocolSpace);
}

let win: BrowserWindow | null;
let friendsWindow: BrowserWindow | null;

let protocolURL: string | null;

for (let i = 0; i < process.argv.length; i++) {
  if (process.argv[i].indexOf(protocolSpace) !== -1) {
    protocolURL = process.argv[i];
    break;
  }
}

let mtIRCCLient: Client | undefined;
declare const __static: string;

let wsPort: number;
let wsSecret: string;
if (process.argv.indexOf('--ws') !== -1) {
  console.log('We have a --ws');
  const wsArg = process.argv[process.argv.indexOf('--ws') + 1];
  const wsArgSplit = wsArg.split(':');
  wsPort = Number(wsArgSplit[0]);
  wsSecret = wsArgSplit[1];
} else {
  console.log('Setting default port and secret');
  wsPort = 13377;
  wsSecret = '';
}

if (process.argv.indexOf('--pid') === -1) {
  console.log('No backend found, starting our own');
  const ourPID = process.pid;
  console.log('Our PID is', ourPID);
  console.log('Exec path is', process.execPath);
  const currentPath = process.execPath.substring(0, process.execPath.lastIndexOf(path.sep));
  console.log('Current working directory is', currentPath);
  let binaryFile = 'FTBApp';
  const operatingSystem = os.platform();
  if (operatingSystem === 'win32') {
    binaryFile += '.exe';
  }
  binaryFile = path.join(currentPath, '..', binaryFile);
  if (fs.existsSync(binaryFile)) {
    console.log('Starting process of backend', binaryFile);
    const child = childProcess.execFile(binaryFile, ['--pid', ourPID.toString()]);
    child.on('exit', (code, signal) => {
      console.log('child process exited with ' + `code ${code} and signal ${signal}`);
    });
    child.on('error', (err) => {
      console.error('Error starting binary', err);
    });
    // @ts-ignore
    child.stdout.on('data', (data) => {
      console.log(`child stdout:\n${data}`);
    });
    // @ts-ignore
    child.stderr.on('data', (data) => {
      console.error(`child stderr:\n${data}`);
    });
  } else {
    console.log('Could not find the binary to launch backend', binaryFile);
  }
}

export interface MTModpacks {
  [index: string]: string;
}

let userData: any;
let authData: any;
let sessionString: string;
const seenModpacks: MTModpacks = {};
const friends: FriendListResponse = { online: [], offline: [], pending: [] };

ipcMain.on('websocketReceived', (event, message) => {
  if (!mtIRCCLient) {
    return;
  }
  if (message.type === 'ircEvent') {
    message.type = message.jsEvent;
    mtIRCCLient.messageReceived(message);
  }
});

ipcMain.on('sendMeSecret', (event) => {
  event.reply('hereIsSecret', { port: wsPort, secret: wsSecret, isDevMode: isDevelopment });
});

ipcMain.on('showFriends', () => {
  if (userData) {
    createFriendsWindow();
  }
});

ipcMain.on('appReady', async (event) => {
  if (protocolURL !== null) {
    event.reply('parseProtocolURL', protocolURL);
  }
});

ipcMain.on('updateSettings', async (event, data) => {
  if (friendsWindow !== null && friendsWindow !== undefined) {
    friendsWindow.webContents.send('updateSettings', data);
  }
  if (data.sessionString) {
    sessionString = data.sessionString;
    if (!userData && win) {
      win.webContents.send('getNewSession', sessionString);
    }
  }
});

ipcMain.on('session', (event, data) => {
  if (!authData) {
    authData = data;
  }
});

ipcMain.on('user', (event, data) => {
  if (!userData) {
    userData = data;
    if (friendsWindow !== undefined && friendsWindow !== null) {
      friendsWindow.webContents.send('hereAuthData', userData);
    }
    log.info('Checking if linked Minecraft Account');
    if (userData?.accounts?.find((s: any) => s.identityProvider === 'mcauth') !== undefined) {
      log.info('Linked Minecraft account, connecting to IRC');
    }
  }
});

// ipcMain.on('authData', async (event, data) => {
//     authData = JSON.parse(data.replace(/(<([^>]+)>)/ig, ''));
//     authData.mc.friendCode = await getFriendCode(authData.mc.hash);
//     // @ts-ignore
//     win.webContents.send('hereAuthData', authData);
//     // @ts-ignore
//     if (friendsWindow !== undefined && friendsWindow !== null) {
//         friendsWindow.webContents.send('hereAuthData', authData);
//     }
//     connectToIRC();
// });

ipcMain.on('gimmeAuthData', (event) => {
  if (userData) {
    event.reply('hereAuthData', userData);
  }
});

ipcMain.on('openModpack', (event, data) => {
  if (win !== null && win !== undefined) {
    win.webContents.send('openModpack', data);
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
    return;
  }

  const result = await dialog.showOpenDialog(win, {
    properties: ['openDirectory'],
    defaultPath: data,
  });

  if (result.filePaths.length > 0) {
    return result.filePaths[0];
  } else {
    return null;
  }
});

ipcMain.handle('selectFile', async (event) => {
  if (win === null) {
    return null;
  }

  const result = await dialog.showOpenDialog(win, {
    properties: ['openFile', 'showHiddenFiles', 'dontAddToRecent'],
    filters: [
      {
        name: 'Java',
        extensions: ['.*'],
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
    log.info(data);
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
  process.exit(1);
});

ipcMain.on('logout', (event, data) => {
  if (friendsWindow) {
    friendsWindow.close();
  }
  if (mtIRCCLient) {
    mtIRCCLient.quit();
    mtIRCCLient = undefined;
  }
  userData = undefined;
});

ipcMain.on('openLink', (event, data) => {
  shell.openExternal(data);
});

function createFriendsWindow() {
  if (friendsWindow !== null && friendsWindow !== undefined) {
    friendsWindow.focus();
    if (win) {
      win.webContents.send('setFriendsWindow', true);
    }
    return;
  }
  friendsWindow = new BrowserWindow({
    title: 'FTB App',

    // Other
    icon: path.join(__static, 'favicon.ico'),
    // Size Settings
    minWidth: 300,
    minHeight: 626,
    // maxWidth: 1000,
    // maxHeight: 626,
    height: 626,
    width: 300,
    frame: false,
    titleBarStyle: 'hidden',
    backgroundColor: '#313131',
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false,
      disableBlinkFeatures: 'Auxclick',
    },
  });

  friendsWindow.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url);
    return { action: 'deny' }
  })

  if (process.env.WEBPACK_DEV_SERVER_URL) {
    friendsWindow.loadURL(`${process.env.WEBPACK_DEV_SERVER_URL as string}#chat`);
    if (!process.env.IS_TEST) {
      friendsWindow.webContents.openDevTools();
    }
  } else {
    friendsWindow.loadURL('app://./index.html#chat');
  }
  if (win) {
    win.webContents.send('setFriendsWindow', true);
  }
  friendsWindow.on('closed', () => {
    friendsWindow = null;
    if (win) {
      win.webContents.send('setFriendsWindow', false);
    }
  });
}

async function createWindow() {
  win = new BrowserWindow({
    title: 'FTB App',

    // Other
    icon: path.join(__static, 'favicon.ico'),
    // Size Settings
    minWidth: 1120,
    minHeight: 800,
    width: 1320,
    height: 800,
    frame: false,
    titleBarStyle: 'hidden',
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
    await win.loadURL(process.env.WEBPACK_DEV_SERVER_URL as string);
    
    if (process.env.NODE_ENV !== "production") {
      win.webContents.openDevTools();
    }

    win.blur();
    setTimeout(() => {
      win?.focus();
    }, 100)
  } else {
    createProtocol(protocolSpace)
    await win.loadURL(`${protocolSpace}://./index.html`);
  }

  win.on('closed', () => {
    win = null;
    if (friendsWindow) {
      friendsWindow.close();
    }
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
  log.debug('Not got the lock');
  app.quit();
} else {
  log.debug('Got the lock');
  app.on('second-instance', (event, commandLine, workingDirectory) => {
    // Someone tried to run a second instance, we should focus our window.
    // console.log(`Event: ${event.s}`)
    if (win) {
      if (win.isMinimized()) {
        win.restore();
      }
      win.focus();
      commandLine.forEach((c) => {
        log.info(c);
        if (c.indexOf('ftb://') !== -1) {
          log.info('parsing through protocol');
          // @ts-ignore
          win.webContents.send('parseProtocolURL', c);
        }
      });
    }
  });

  app.on('ready', async () => {
    // TODO: come back to this :D
    // var template = [
    //   ...Menu.getApplicationMenu()?.items as any,
    //   {label: 'View Sexy', submenu: [
    //       {label: 'HTML/Markdown', click: () => console.log("hello")}
    //     ]}
    // ];
    // Menu.setApplicationMenu(Menu.buildFromTemplate(template as any));
    //
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
