import ElectronOverwolfInterface from './electron-overwolf-interface';

/**
 * Creates a simple way of abstracting the logic between overwolf and electron into a
 * unified interface.
 */
class Platform {
  protected inter: ElectronOverwolfInterface = {} as ElectronOverwolfInterface;

  constructor() {}

  public async setup() {
    this.inter = ((await import(`./${process.env.VUE_APP_PLATFORM}`)).default as unknown) as ElectronOverwolfInterface;
  }

  get get() {
    return this.inter;
  }

  public isOverwolf() {
    return process.env.VUE_APP_PLATFORM !== 'electron';
  }

  public isElectron() {
    return process.env.VUE_APP_PLATFORM === 'electron';
  }
}

export type PlatformType = Platform;

export default new Platform();
