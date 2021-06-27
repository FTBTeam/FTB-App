import ElectronOverwolfInterface from "./electron-overwolf-interface";

const isElectron = true;

/**
 * Creates a simple way of abstracting the logic between overwolf and electron into a
 * unified interface.
 */
class Platform {
  protected inter: ElectronOverwolfInterface = {} as ElectronOverwolfInterface;

  constructor() {
    this.setup().catch(e => console.log("Platform failed resolve deps"));
  }

  async setup() {
    this.inter = isElectron
      ? (((await import("./electron"))
          .default as unknown) as ElectronOverwolfInterface)
      : (((await import("./overwolf"))
          .default as unknown) as ElectronOverwolfInterface);
  }

  get get() {
    return this.inter;
  }
}

export default new Platform();
