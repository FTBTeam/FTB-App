'use strict';
import { Friend } from './modules/auth/types';
import {app, protocol, BrowserWindow, remote, shell, ipcMain, dialog, session} from 'electron';
import path from 'path';
import os from 'os';
import fs from 'fs';
import {createProtocol} from 'vue-cli-plugin-electron-builder/lib';
import * as log from 'electron-log';
import childProcess from 'child_process';
// @ts-ignore
import {Client} from 'irc-framework';
import fetch, {Response} from 'node-fetch';
import { FriendListResponse } from './types';

Object.assign(console, log.functions);
app.console = log;
const isDevelopment = process.env.NODE_ENV !== 'production' || process.argv.indexOf('--dev') !== -1;

log.transports.file.resolvePath = (variables, message): string => {
    return path.join(process.cwd(), 'electron.log');
}

let win: BrowserWindow | null;
let friendsWindow: BrowserWindow | null;

let mtIRCCLient: Client;
declare const __static: string;

protocol.registerSchemesAsPrivileged([{scheme: 'app', privileges: {secure: true, standard: true}}]);

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
    const currentPath = process.cwd();
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

let authData: any;
const seenModpacks: MTModpacks = {};
let friends: FriendListResponse = {friends: [], requests: []};

ipcMain.on('sendMeSecret', (event) => {
    event.reply('hereIsSecret', {port: wsPort, secret: wsSecret, isDevMode: isDevelopment});
});

ipcMain.on('openOauthWindow', (event, data) => {
    createOauthWindow();
});

ipcMain.on('showFriends', () => {
    createFriendsWindow();
});

ipcMain.on('getFriends', (event) => {
    // If only I had some friends...
    event.reply('ooohFriend', friends);
});

ipcMain.on('checkFriends', async (event) => {
    friends = await getFriends();
    if(mtIRCCLient !== undefined){
        friends.friends.forEach((friend: Friend) => {
            mtIRCCLient.whois(friend.shortHash);
        });
        friends.requests.forEach((friend: Friend) => {
            mtIRCCLient.whois(friend.shortHash);
        });
    }
});

ipcMain.on('sendMessage', async (event, data) => {
    if (!mtIRCCLient) {
        return;
    }
    const friend: Friend = data.friend;
    const message = data.message;
    mtIRCCLient.say(friend.shortHash, message);
});

async function getMTIRC() {
    return fetch(`https://api.creeper.host/minetogether/chatserver`).then((resp: Response) => resp.json()).then((data: any) => {
        return {
            host: data.server.address,
            port: data.server.port,
        };
    }).catch((err: any) => {
        log.error('Failed to get details about MineTogether servers', err);
        return undefined;
    });
}

async function getProfile(hash: string) {
    return fetch(`https://api.creeper.host/minetogether/profile`, {headers: {
        'Content-Type': 'application/json'
    }, method: 'POST', body: JSON.stringify({target: hash})}).then((resp: Response) => resp.json()).then((data: any) => {
        return data;
    }).catch((err: any) => {
        log.error('Failed to get details about MineTogether profile', hash, err);
        return undefined;
    });
}


async function getFriendCode(hash: string) {
    return fetch(`https://api.creeper.host/minetogether/friendcode`, {headers: {
        'Content-Type': 'application/json'
    }, method: 'POST', body: JSON.stringify({hash: hash})}).then((resp: Response) => resp.json()).then((data: any) => {
        return data.code;
    }).catch((err: any) => {
        log.error('Failed to get details about MineTogether profile', hash, err);
        return undefined;
    });
}

async function addFriend(code: string, display: string) {
    return fetch(`https://api.creeper.host/minetogether/requestfriend`, {headers: {
        'Content-Type': 'application/json'
    }, method: 'POST', body: JSON.stringify({target: code, hash: authData.mc.hash, display: display})}).then((resp: Response) => resp.json()).then((data: any) => {
        if(data.status === "success"){
            return true;
        } else {
            return false;
        }
    }).catch((err: any) => {
        log.error('Failed to add new friend', code, display, err);
        return undefined;
    });
}

async function getFriends(): Promise<FriendListResponse> {
    return fetch(`https://api.creeper.host/minetogether/listfriend`, {headers: {
        'Content-Type': 'application/json',
    }, method: 'POST', body: JSON.stringify({hash: authData.mc.hash})})
    .then((response: any) => response.json())
    .then(async (data: any) => {
        console.log(data);
        let friendsList: Friend[] = data.friends as Friend[];
        friendsList = friendsList.map((friend: Friend) => {
            if(friend.hash){
                const shortHash = `MT${friend.hash.substring(0, 15).toUpperCase()}`;
                friend.shortHash = shortHash;
            }
            return friend;
        }) ;
        let requests: Friend[] = data.requests as Friend[]
        requests = requests.map((friend: Friend) => {
            if(friend.hash){
                const shortHash = `MT${friend.hash.substring(0, 15).toUpperCase()}`;
                friend.shortHash = shortHash;
            }
            return friend;
        });
        return {
            friends: friendsList,
            requests,
        }
    }).catch((err: any) => {
        log.error('Failed to get details about MineTogether friends', err);
        return {
            friends: [],
            requests: [],
        };
    });
}

ipcMain.on('disconnect', (event) => {
    if(mtIRCCLient){
        mtIRCCLient.quit();
        mtIRCCLient = undefined;
    }
})

ipcMain.on('sendFriendRequest', async (event, data) => {
    console.log("Going to send friend request", data)
    if(mtIRCCLient){
        let profile = await getProfile(authData.mc.hash);
        let displayName = profile.profileData[authData.mc.hash].hash.short;
        console.log("Going to send CTCP request to", data.target, authData.mc.friendCode, displayName);
        mtIRCCLient.ctcpRequest(data.target, 'FRIENDREQ', authData.mc.friendCode, displayName)
    }
});

ipcMain.on('acceptFriendRequest', async (event, data) => {
    console.log("Going to accept friend request", data)
    if(mtIRCCLient){
        let profile = await getProfile(authData.mc.hash);
        let displayName = profile.profileData[authData.mc.hash].hash.short;
        console.log("Going to send CTCP request to", data.target, authData.mc.friendCode, displayName);
        mtIRCCLient.ctcpRequest(data.target, 'FRIENDACC', authData.mc.friendCode, displayName)
    }
});

async function connectToIRC(){
    if(mtIRCCLient !== undefined){
        log.error("Tried to connect to IRC when we're already connected");
        return;
    }
    const mtDetails = await getMTIRC();
    if (mtDetails === undefined) {
        log.error('Failed to get details about MineTogether servers');
        return;
    }
    mtIRCCLient = new Client();
    mtIRCCLient.connect({
        host: mtDetails.host,
        port: mtDetails.port,
        nick: authData.mc.mtusername,
        gecos: '{"p":""}',
    });
    friends = await getFriends();
    friends.friends.forEach((friend: Friend) => {
        mtIRCCLient.whois(friend.shortHash);
    });
    friends.requests.forEach((friend: Friend) => {
        mtIRCCLient.whois(friend.shortHash);
    });
    mtIRCCLient.on('whois', async (event: any) => {
        if (event.nick) {
            let friend = friends.friends.find((f: Friend) => f.shortHash === event.nick);
            if (friend === undefined) {
                friend = friends.requests.find((f: Friend) => f.shortHash === event.nick);
                if(friend === undefined){
                    return;
                }
            }
            if (event.error) {
                friend.online = false;
            } else {
                friend.online  = true;
                if (event.real_name) {
                    let realName;
                    try {
                        realName = JSON.parse(event.real_name);
                    } catch (e){
                        console.log('Invalid real name', event.real_name);
                        if (win) {
                            win.webContents.send('ooohFriend', friends);
                        }
                        if (friendsWindow) {
                            friendsWindow.webContents.send('ooohFriend', friends);
                        }
                        return;
                    }
                    friend.currentPack = '';
                    if (realName.b) {
                        friend.currentPack = realName.b;
                    } else if (realName.p) {
                        friend.currentPack = realName.p;
                    }
                    if (friend.currentPack === undefined) {
                        return;
                    }
                    if (seenModpacks[friend.currentPack] !== undefined) {
                        friend.currentPack = seenModpacks[friend.currentPack];
                    } else {
                        if (!isNaN(parseInt(friend.currentPack, 10))) {
                            await fetch(`https://creeperhost.net/json/modpacks/twitch/${friend.currentPack}`).then((resp: Response) => resp.json()).then((data: any) => {
                                if (data.name) {
                                    const fixedString = data.name.replace(/[CurseForge/UNSUPPORTED]/, '');
                                    // @ts-ignore
                                    seenModpacks[friend.currentPack] = fixedString;
                                    // @ts-ignore
                                    friend.currentPack = fixedString;
                                }
                            });
                        } else if (friend.currentPack.length > 0) {
                            const fixedString = friend.currentPack.replace(/\\u003/, '=');
                            await fetch(`https://creeperhost.net/json/modpacks/modpacksch/${fixedString}`).then((resp: Response) => resp.json()).then((data: any) => {
                                if (data.name) {
                                    // @ts-ignore
                                    seenModpacks[friend.currentPack] = data.name;
                                    // @ts-ignore
                                    friend.currentPack = data.name;
                                }
                            });
                        }
                    }
                }
            }
            if (win) {
                win.webContents.send('ooohFriend', friends);
            }
            if (friendsWindow) {
                friendsWindow.webContents.send('ooohFriend', friends);
            }
        }
    });
    mtIRCCLient.on('ctcp request', (event: any) => {
        console.log("CTCP event", event)
        if (friendsWindow !== undefined && friendsWindow !== null){
            if(event.type === "FRIENDREQ"){
                let args = event.message.substring("FRIENDREQ".length, event.message.length).split(" ");
                let [code, ...rest] = args;
                friendsWindow.webContents.send('newFriendRequest', {from: event.nick, displayName: rest.join(' '), friendCode: code});
            } else if(event.type === "FRIENDACC"){
                let args = event.message.substring("FRIENDACC".length, event.message.length).split(" ");
                let [code, ...rest] = args;
                addFriend(code, rest.join(' '))
            }
        }
    })
    mtIRCCLient.on('message', (event: any) => {
        console.log(event);
        if (event.type === 'privmsg') {
            if (friendsWindow) {
                const friend = friends.friends.find((f: Friend) => f.shortHash === event.nick);
                if (friend === undefined) {
                    return;
                }
                friendsWindow.webContents.send('newMessage', {from: event.nick, friend, message: event.message, date: new Date().getTime()});
            }
        }
    });
}

ipcMain.on('authData', async (event, data) => {
    authData = JSON.parse(data.replace(/(<([^>]+)>)/ig, ''));
    authData.mc.friendCode = await getFriendCode(authData.mc.hash);
    // @ts-ignore
    win.webContents.send('hereAuthData', authData);
    // @ts-ignore
    if (friendsWindow !== undefined && friendsWindow !== null) {
        friendsWindow.webContents.send('hereAuthData', authData);
    }
    connectToIRC();
});

ipcMain.on('gimmeAuthData', (event) => {
    if (authData) {
        event.reply('hereAuthData', authData);
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


function createFriendsWindow() {
    if (friendsWindow !== null && friendsWindow !== undefined) {
        friendsWindow.focus();
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

    friendsWindow.on('closed', () => {
        friendsWindow = null;
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


app.on('ready', async () => {
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
    window.loadURL('https://auth.modpacks.ch/login');
    window.webContents.on('did-redirect-navigation', async (event, url, isInPlace, isMainFrame, frameProcessId, frameRoutingId) => {
        if (url.startsWith('https://auth.modpacks.ch/auth')) {
            await window.webContents.executeJavaScript(`
                require('electron').ipcRenderer.send('authData', document.body.innerHTML);
            `);
            window.close();
        }
    });
}
