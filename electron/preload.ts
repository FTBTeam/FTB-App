import { ipcRenderer, contextBridge } from 'electron'
import path from 'node:path';
import fs from 'node:fs';
import os from 'node:os';
import MarkdownIt from 'markdown-it';

const markdownParser = new MarkdownIt();
markdownParser.renderer.rules.link_open = function (tokens, idx, options, _, self) {
  if (!(window as any).helperOpenLink) {
    (window as any).helperOpenLink = function (event: any) {
      event.preventDefault();
      let urlTarget = event.target;

      if (event.target?.tagName !== 'A') {
        // Get the closest parent link
        urlTarget = event.target?.closest('a');
      }
      
      ipcRenderer.send('action/open-link', urlTarget.href);
    }
  }

  tokens[idx].attrSet('onclick', 'event.preventDefault(); window.helperOpenLink(this.href);');
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
    rename: fs.renameSync
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
  }
}

export type NodeUtils = typeof nodeUtils

contextBridge.exposeInMainWorld("nodeUtils", nodeUtils)
