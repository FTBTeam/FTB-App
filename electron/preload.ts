import { contextBridge, ipcRenderer } from 'electron';
import path from 'node:path';
import fs from 'node:fs';
import os from 'node:os';
import { fallbackMetaData, loadApplicationMetaData } from '../src/utils/nuturalHelpers.ts';
import log from 'electron-log/main'

// --------- Expose some API to the Renderer process ---------
contextBridge.exposeInMainWorld('ipcRenderer', {
  on(...args: Parameters<typeof ipcRenderer.on>) {
    const [channel, listener] = args
    return ipcRenderer.on(channel, (event, ...args) => listener(event, ...args))
  },
  off(...args: Parameters<typeof ipcRenderer.off>) {
    const [channel, ...omit] = args
    return ipcRenderer.off(channel, ...omit)
  },
  send(...args: Parameters<typeof ipcRenderer.send>) {
    const [channel, ...omit] = args
    return ipcRenderer.send(channel, ...omit)
  },
  invoke(...args: Parameters<typeof ipcRenderer.invoke>) {
    const [channel, ...omit] = args
    return ipcRenderer.invoke(channel, ...omit)
  },
})

const nodeUtils = {
  path: {
    join: path.join, // Expose the `join` function from `path` module.
    resourcesPath: process.resourcesPath
  },
  // @deprecated we shouldn't give fs to renderer process
  fs: {
    exists: fs.existsSync,
    readFile: fs.readFileSync,
    writeFile: fs.writeFileSync,
    mkdir: fs.mkdirSync,
    readdir: fs.readdirSync,
    stat: fs.statSync,
    rm: fs.rmSync,
    rename: fs.renameSync,
  },
  os: {
    platform: os.platform,
    homedir: os.homedir,
    arch: os.arch
  },
  buffer: {
    from: Buffer.from,
  },
  app: {
    getMetaData() {
      if (!import.meta.env.PROD) {
        return fallbackMetaData;
      }
      
      return loadApplicationMetaData(process.resourcesPath, path.join, (path) => fs.readFileSync(path, "utf-8"))
    },
    getLicenses() {
      let frontendLicenses: any = {};
      let javaLicenses: any = {};
      
      if (!import.meta.env.PROD) {
        return {frontend: {}, java: {}}
      }
      
      try {
        frontendLicenses = JSON.parse(fs.readFileSync(path.join(process.resourcesPath, "licenses.json"), 'utf-8'));
      } catch (e) {
        log.warn("Failed to load frontend licenses", e);
      }
      
      try {
        javaLicenses = JSON.parse(fs.readFileSync(path.join(process.resourcesPath, "java-licenses.json"), 'utf-8'));
      } catch (e) {
        log.warn("Failed to load java licenses", e);
      }
      
      return {
        frontend: frontendLicenses,
        java: javaLicenses
      }
    }
  }
}

export type NodeUtils = typeof nodeUtils

contextBridge.exposeInMainWorld("nodeUtils", nodeUtils)
