'use strict';
import { Friend } from './modules/auth/types';
import {app, protocol, BrowserWindow, remote, shell, ipcMain, dialog, session} from 'electron';
import path from 'path';
import os from 'os';
import fs from 'fs';
import {createProtocol} from 'vue-cli-plugin-electron-builder/lib';
import * as log from 'electron-log';
import childProcess from 'child_process';
import axios from 'axios';
// @ts-ignore
// import {Client} from 'irc-framework';
import Client from './ircshim';
import { FriendListResponse } from './types';

Object.assign(console, log.functions);
app.console = log;
const isDevelopment = process.env.NODE_ENV !== 'production' || process.argv.indexOf('--dev') !== -1;

log.transports.file.resolvePath = (variables, message): string => {
    return path.join(process.execPath.substring(0, process.execPath.lastIndexOf(path.sep)), 'electron.log');
};

const httpClient = axios.create();
httpClient.defaults.timeout = 5000;

let win: BrowserWindow | null;
let friendsWindow: BrowserWindow | null;

let protocolURL: string | null;

for (let i = 0; i < process.argv.length; i++) {
    if (process.argv[i].indexOf('ftb://') !== -1) {
        protocolURL = process.argv[i];
        break;
    }
}

let mtIRCCLient: Client | undefined;
declare const __static: string;

protocol.registerSchemesAsPrivileged([{scheme: 'app', privileges: {secure: true, standard: true}}]);
app.setAsDefaultProtocolClient('ftb');

let wsPort: number;
let wsSecret: string;
if (process.argv.indexOf('--ws') !== -1) {
    console.log('We have a --ws');
    const wsArg =  process.argv[process.argv.indexOf('--ws') + 1];
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
            console.log('child process exited with ' +
            `code ${code} and signal ${signal}`);
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
const friends: FriendListResponse = {friends: [], requests: []};

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
    event.reply('hereIsSecret', {port: wsPort, secret: wsSecret, isDevMode: isDevelopment});
});

ipcMain.on('openOauthWindow', (event, data) => {
    createOauthWindow();
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
        if (userData.accounts.find((s: any) => s.identityProvider === 'mcauth') !== undefined) {
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


ipcMain.on('selectFolder', async (event, data) => {
    if (win === null) {
        return;
    }
    const result = await dialog.showOpenDialog(win, {
        properties: ['openDirectory'],
        defaultPath: data,
    });
    if (result.filePaths.length > 0) {
        event.reply('setInstanceFolder', result.filePaths[0]);
    }
});

ipcMain.on('windowControls', (event, data) => {
    const window = BrowserWindow.fromWebContents(event.sender);
    if (window) {
        log.info(data);
        switch (data.action) {
            case 'close':
                window.close();
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
        title: 'FTB Desktop App',

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
            webSecurity: false,
            nodeIntegration: true,
            disableBlinkFeatures: 'Auxclick',
        },
    });

    friendsWindow.webContents.on('new-window', (event, url) => {
        event.preventDefault();
        shell.openExternal(url);
    });

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

function createWindow() {

    win = new BrowserWindow({
        title: 'FTB Desktop App',

        // Other
        icon: path.join(__static, 'favicon.ico'),
        // Size Settings
        minWidth: 1000,
        minHeight: 626,
        // maxWidth: 1000,
        // maxHeight: 626,
        height: 626,
        width: 1000,
        frame: false,
        titleBarStyle: 'hidden',
        webPreferences: {
            nodeIntegration: true,
            disableBlinkFeatures: 'Auxclick',
        },
    });

    win.webContents.on('new-window', (event, url) => {
        event.preventDefault();
        shell.openExternal(url);
    });

    if (process.env.WEBPACK_DEV_SERVER_URL) {
        win.loadURL(process.env.WEBPACK_DEV_SERVER_URL as string);
        if (!process.env.IS_TEST) {
            win.webContents.openDevTools();
        }
    } else {
        createProtocol('app');
        win.loadURL('app://./index.html');
    }

    win.on('closed', () => {
        win = null;
        if (friendsWindow !== undefined && friendsWindow !== null) {
            friendsWindow.close();
        }
    });
    if (process.env.NODE_ENV !== 'production') {
        BrowserWindow.addDevToolsExtension('node_modules/vue-devtools/vender');
    }
}


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
            if (win.isMinimized()) { win.restore(); }
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

    // Create myWindow, load the rest of the app, etc...
    // app.whenReady().then(() => {
    // })
    app.on('ready', async () => {
        createWindow();
        session.defaultSession.webRequest.onHeadersReceived((details, callback) => {
            if (details.url.indexOf('twitch.tv') !== -1) {
                if (details.responseHeaders) {
                    if (details.responseHeaders['Content-Security-Policy'] !== undefined) {
                        details.responseHeaders['Content-Security-Policy'] = [];
                    }
                }
            } else if (details.url.indexOf('https://www.creeperhost.net/json/modpacks/modpacksch/') !== -1) {
                if (details.responseHeaders) {
                    if (details.responseHeaders['access-control-allow-origin'] !== undefined) {
                        details.responseHeaders['access-control-allow-origin'] = ['*'];
                    }
                }
            }
            callback({ responseHeaders: details.responseHeaders });
        });
    });
}


// app.on('ready', async () => {
//     createWindow();
//     session.defaultSession.webRequest.onHeadersReceived((details, callback) => {
//         if (details.url.indexOf('twitch.tv') !== -1) {
//             if (details.responseHeaders) {
//                 if (details.responseHeaders['Content-Security-Policy'] !== undefined) {
//                     details.responseHeaders['Content-Security-Policy'] = [];
//                 }
//             }
//         }
//         callback({ responseHeaders: details.responseHeaders });
//     });
// });

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

async function getMTSelf(cookie: string) {
    // try {
    //     const response = await httpClient.get(`https://minetogether.io/api/me`, {headers: {Cookie: 'PHPSESSID=' + cookie, 'App-Auth': }});
    //     const user = response.data;
    //     return user;
    // } catch (err) {
    //     log.error(`Error getting MineTogether chat servers`, err);
    //     return undefined;
    // }
}
// Oauth Window

function createOauthWindow() {
    const window = new BrowserWindow({
        title: 'FTB Desktop App',

        // Other
        icon: path.join(__static, 'favicon.ico'),
        // Size Settings
        minWidth: 0,
        minHeight: 0,
        // maxWidth: 1000,
        // maxHeight: 626,
        height: 800,
        width: 550,
        // frame: false,
        titleBarStyle: 'hidden',
        webPreferences: {
            nodeIntegration: true,
            disableBlinkFeatures: 'Auxclick',
        },
    });

    // window.setMenu(null);
    window.loadURL('https://minetogether.io/api/login');
    window.webContents.session.webRequest.onHeadersReceived({urls: []}, (details, callback) => {
        if (details.url.indexOf('https://minetogether.io/api/redirect') !== -1) {
            if (details.responseHeaders) {
                if (details.responseHeaders['app-auth'] && win) {
                    win.webContents.send('setSessionString', details.responseHeaders['app-auth'][0]);
                }
            }
        }
        callback({});
    });
    window.webContents.on('did-redirect-navigation', async (event, url, isInPlace, isMainFrame, frameProcessId, frameRoutingId) => {
        if (url.startsWith('https://minetogether.io/profile')) {
            window.webContents.session.cookies.get({name: 'PHPSESSID'})
            .then(async (cookies) => {
                if (cookies.length === 1) {
                    if (win) {
                        win.webContents.send('setSessionID', cookies[0].value);
                    }
                    authData = cookies[0].value;
                }
            }).catch((error) => {
              console.log(error);
            });
            window.close();
        }
    });
}
