'use strict';
import fs from 'fs';

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

// let usingLegacySettings = false;
// const appSettingsPathLegacy = path.join(appHome, 'bin', 'settings.json');
// const appSettingsPath = path.join(appHome, "storage", 'settings.json');
// let appSettings = getAppSettings(appSettingsPath);
// if (appSettings === null) {
//   logger.debug("App settings not found, trying legacy settings")
//   usingLegacySettings = true;
//   appSettings = getAppSettings(appSettingsPathLegacy);
// }
//
// const appLogsParent = path.join(appHome, 'logs');
// try {
//   if (!fs.existsSync(appLogsParent)) {
//     fs.mkdirSync(appLogsParent, { recursive: true });
//   }
// } catch (e) {
//   logger.error("Failed to create app logs parent directory", e)
// }
//
// const electronLogFile = path.join(appLogsParent, 'ftb-app-electron.log');
// if (fs.existsSync(electronLogFile)) {
//   try {
//     fs.writeFileSync(electronLogFile, '')
//   } catch (e) {
//     logger.error("Failed to clear electron log file", e)
//   }
// }

// const isDevelopment = process.env.NODE_ENV !== 'production' || process.argv.indexOf('--dev') !== -1;


// async function createWindow(reset = false) {
//  
//   logger.debug("Creating main window")
//   let useSystemFrame = false;
//   if (usingLegacySettings) {
//     useSystemFrame = appSettings?.useSystemWindowStyle === "true" ?? false;
//   } else {
//     useSystemFrame = appSettings.appearance.useSystemWindowStyle
//   }
//  
//   win = new BrowserWindow({
//     title: 'FTB App',
//
//     // Size Settings
//     minWidth: 1220,
//     minHeight: 895,
//     width: 1545,
//     height: 900,
//     frame: useSystemFrame,
//     titleBarStyle: useSystemFrame ? 'default' : 'hidden',
//     autoHideMenuBar: true,
//     show: false,
//     backgroundColor: '#2a2a2a',
//     webPreferences: {
//       nodeIntegration: true,
//       contextIsolation: false,
//       disableBlinkFeatures: 'Auxclick',
//       webSecurity: false,
//     },
//   });
//  
//   win.webContents.setWindowOpenHandler((details) => {
//     shell.openExternal(details.url);
//     return { action: 'deny' }
//   })
//
//   win.webContents.session.webRequest.onBeforeSendHeaders((details, callback) => {
//     callback({ requestHeaders: { Origin: '*', ...details.requestHeaders } });
//   });
//
//   win.webContents.session.webRequest.onHeadersReceived((details, callback) => {
//     callback({
//       responseHeaders: {
//         'Access-Control-Allow-Origin': ['*'],
//         ...details.responseHeaders,
//       },
//     });
//   });
//  
//   if (process.env.WEBPACK_DEV_SERVER_URL) {
//     logger.debug("Loading dev server url")
//     await win.loadURL(process.env.WEBPACK_DEV_SERVER_URL as string);
//    
//     if (process.env.NODE_ENV !== "production") {
//       logger.debug("Opening dev tools for main window")
//       win.webContents.openDevTools({
//         mode: 'detach',
//       });
//     }
//   } else {
//     logger.debug("Loading app from built files")
//     await win.loadURL(`${protocolSpace}://./index.html`);
//   }
//
//   if (reset) {
//     preserveSubprocess = false;
//   }
//
//   win.on('closed', () => {
//     // Kill the subprocess if it's running
//     if (subprocess !== null && !preserveSubprocess) {
//       subprocess.kill();
//     }
//     win = null;
//   });
// }