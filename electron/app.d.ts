import {ChildProcess} from "child_process";
import {BrowserWindow} from "electron";

export type AppData = {
  protocolSpace: string;
  
  state: {
    appStarted: boolean;
    initialProtocolUrl: string | null;
    ws: {
      pid: number;
      port: number;
      token: string;
    } | null;
    isSubprocessSetup: boolean;
    preserveSubprocess: boolean;
  },

  subprocess: ChildProcess | null;
  mainWindow: BrowserWindow | null;
  prelaunchWindow: BrowserWindow | null;

  options: {
    startInFullscreen: boolean;
    openDebugTools: boolean;
  },
  
  paths: {
    home: string;
    logFile: string;
  }
}