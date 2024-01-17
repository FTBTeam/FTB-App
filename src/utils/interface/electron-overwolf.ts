import ElectronOverwolfInterface from './electron-overwolf-interface';
import {createLogger} from '@/core/logger';

/**
 * Creates a simple way of abstracting the logic between overwolf and electron into a
 * unified interface.
 */
class Platform {
  protected inter: ElectronOverwolfInterface = {} as ElectronOverwolfInterface;
  private logger = createLogger("Platform.ts")

  constructor() {}

  public async setup() {
    this.logger.info('Setting up platform');
    this.inter = ((await import(`./${process.env.VUE_APP_PLATFORM}`)).default as unknown) as ElectronOverwolfInterface;
    this.logger.info(`Platform setup complete for ${process.env.VUE_APP_PLATFORM}`);
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
