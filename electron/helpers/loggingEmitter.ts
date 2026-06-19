import log from "electron-log/main";
import {AppData} from "../app";

export class LogAndEmit {
  private _args: any[] = [];
  private _meta: any | null = {};

  private constructor(
    private readonly eventName: string,
    private readonly appData: AppData
  ) {
  }

  public static create(name: string, appData: AppData) {
    return new LogAndEmit(name, appData);
  }

  public args(...args: any[]) {
    this._args = args;
    return this;
  }

  public meta(meta: any) {
    this._meta = meta;
    return this;
  }

  public execute() {
    log.debug("Emitting event", this.eventName, this._args, this._meta);
    if (!this.appData.prelaunchWindow && !this.appData.mainWindow) {
      log.warn("No window to send event to, skipping", this.eventName);
      return;
    }

    const target = this.appData.prelaunchWindow ? this.appData.prelaunchWindow : this.appData.mainWindow;
    if (!target) {
      log.warn("No target window to send event to, skipping", this.eventName);
      return;
    }

    target.webContents.send(this.eventName, ...this._args);
  }
}