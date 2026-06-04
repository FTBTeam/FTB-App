import path from "node:path";
import fs from "node:fs";

import {app, ipcMain} from "electron";
import log from "electron-log/main";
import {autoUpdater} from "electron-updater";

import {appHome} from "./main.ts";
import {logAndReturn} from "./helpers/utils.ts";
import {LogAndEmit} from "./helpers/loggingEmitter.ts";
import {AppData} from "./app";

export function setupUpdater(appData: AppData) {
  autoUpdater.allowDowngrade = true;
  autoUpdater.channel = logAndReturn(loadChannel(), "Loaded auto updater channel: ");

  autoUpdater.on('checking-for-update', () => LogAndEmit.create('updater:checking-for-update', appData).execute());
  autoUpdater.on("update-cancelled", (info) => LogAndEmit.create('updater:update-cancelled', appData).meta(info).execute());
  autoUpdater.on('update-available', (info) => LogAndEmit.create('updater:update-available', appData).meta(info).args(info.version).execute());
  autoUpdater.on('update-not-available', (info) => LogAndEmit.create('updater:update-not-available', appData).meta(info).execute());
  autoUpdater.on('error', (error, message) => LogAndEmit.create('updater:error', appData).meta({error: error, message: message}).execute());
  autoUpdater.on('download-progress', (progress) => LogAndEmit.create('updater:download-progress', appData)
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
    LogAndEmit.create('updater:update-downloaded', appData)
      .meta(event)
      .execute()
  });

  ipcMain.on("updater:download-update", () => {
    autoUpdater.downloadUpdate().catch((e) => log.error("Failed to download update", e));
  })
}

export function updateApp(source: string) {
  log.debug("Quitting and installing app from: ", source)

  // Get all known windows and close them
  log.debug("Removing all listeners and closing windows");
  app.removeAllListeners('window-all-closed');
  
  log.debug("Quitting app")
  autoUpdater.quitAndInstall();
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
      log.error("Failed to load channel data", e)
    }

  } catch (e) {
    log.error("Failed to load channel", e)
  }

  return 'stable';
}