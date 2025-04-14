import { contextBridge, ipcRenderer } from 'electron';
import path from 'node:path';
import fs from 'node:fs';
import os from 'node:os';
import MarkdownIt from 'markdown-it';
import { fallbackMetaData, loadApplicationMetaData } from '../src/nuturalHelpers.ts';

const markdownParser = new MarkdownIt();
markdownParser.renderer.rules.link_open = function (tokens, idx, options, _, self) {
  // Ensure the link is opened in a new tab
  tokens[idx].attrPush(['target', '_blank']);
  return self.renderToken(tokens, idx, options);
}

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
  markdown: {
    parse(text: string) {
      return markdownParser.render(text)
    }
  },
  app: {
    getMetaData() {
      if (process.env.NODE_ENV === "development") {
        return fallbackMetaData;
      }
      
      return loadApplicationMetaData(process.resourcesPath, path.join, (path) => fs.readFileSync(path, "utf-8"))
    },
    getLicenses() {
      let frontendLicenses: any = {};
      let javaLicenses: any = {};
      
      try {
        frontendLicenses = JSON.parse(fs.readFileSync(path.join(process.resourcesPath, "licenses.json"), 'utf-8'));
      } catch (e) {
        console.warn("Failed to load frontend licenses", e);
      }
      
      try {
        javaLicenses = JSON.parse(fs.readFileSync(path.join(process.resourcesPath, "java-licenses.json"), 'utf-8'));
      } catch (e) {
        console.warn("Failed to load java licenses", e);
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
