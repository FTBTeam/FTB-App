import ElectronOverwolfInterface from './electron-overwolf-interface';
import {createLogger} from '@/core/logger';
import {constants} from '@/core/constants';

/**
 * Creates a simple way of abstracting the logic between overwolf and electron into a
 * unified interface.
 */
class Platform {
  protected inter: ElectronOverwolfInterface = {} as ElectronOverwolfInterface;
  private logger = createLogger("Platform.ts")

  private platformProvider = constants.platform
  
  constructor() {}

  public async setup() {
    this.logger.info('Setting up platform');
    this.inter = ((await import(`./${this.platformProvider}`)).default as unknown) as ElectronOverwolfInterface;
    this.logger.info(`Platform setup complete for ${this.platformProvider ?? "electron"}`);
  }

  get get() {
    return this.inter;
  }

  public isOverwolf() {
    return this.platformProvider === 'overwolf';
  }

  public isElectron() {
    return this.platformProvider === 'electron';
  }
}

export type PlatformType = Platform;

export default new Platform();
