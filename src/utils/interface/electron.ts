import { ipcRenderer } from "electron";
import ElectronOverwolfInterface from "./electron-overwolf-interface";

const Electron: ElectronOverwolfInterface = {
  utils: {
    openUrl(url: string) {
      ipcRenderer.send("openLink", url);
    },
  },
};

export default Electron;
