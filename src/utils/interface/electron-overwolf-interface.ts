export interface Util {
  openUrl: (e: string) => void;
}

export default interface ElectronOverwolfInterface {
  utils: Util;
}
