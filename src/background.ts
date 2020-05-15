'use strict';

import { app, protocol, BrowserWindow, remote, shell, ipcMain } from 'electron';
import path from 'path';
import os from 'os';
import fs from 'fs';
import { createProtocol } from 'vue-cli-plugin-electron-builder/lib';
import * as log from 'electron-log';
import childProcess from 'child_process';
Object.assign(console, log.functions);
app.console = log;
const isDevelopment = process.env.NODE_ENV !== 'production';

const userPath = (app || remote.app).getPath('userData');

let win: BrowserWindow | null;
declare const __static: string;

protocol.registerSchemesAsPrivileged([{ scheme: 'app', privileges: { secure: true, standard: true } }]);

let wsPort: number;
let wsSecret: string;
if(process.argv.indexOf('--ws') !== -1){
    console.log("We have a --ws");
    const wsArg =  process.argv[process.argv.indexOf('--ws') + 1];
    const wsArgSplit = wsArg.split(':');
    wsPort = Number(wsArgSplit[0]);
    wsSecret = wsArgSplit[1];
} else {
    console.log("Setting default port and secret");
    wsPort = 13377;
    wsSecret = '';
}

ipcMain.on('sendMeSecret', (event) => {
    event.reply('hereIsSecret', {port: wsPort, secret: wsSecret});
});

ipcMain.on('updateSecret', (event, data) => {
    wsPort = data.port;
    wsSecret = data.secret;
});

if (process.argv.indexOf('--pid') === -1) {
    console.log("No backend found, starting our own");
    const ourPID = process.pid;
    console.log("Our PID is", ourPID);
    const currentPath =  process.cwd();
    console.log("Current working directory is", currentPath);
    let binaryFile = 'FTBApp';
    const operatingSystem = os.platform();
    if (operatingSystem === 'win32') {
        binaryFile += '.exe';
    }
    binaryFile = path.join(currentPath, "..", binaryFile);
    if (fs.existsSync(binaryFile)) {
        console.log("Starting process of backend", binaryFile);
        let child = childProcess.execFile(binaryFile, ['--pid', ourPID.toString()]);
        child.on('exit', (code, signal) => {
            console.log('child process exited with ' +
            `code ${code} and signal ${signal}`);
        });
        child.on('error', (err) => {
            console.error("Error starting binary", err);
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
        if (!process.env.IS_TEST) { win.webContents.openDevTools(); }
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
