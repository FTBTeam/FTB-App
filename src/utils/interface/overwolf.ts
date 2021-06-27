import ElectronOverwolfInterface from "./electron-overwolf-interface";

declare global {
  var overwolf: any;
}

const Overwolf: ElectronOverwolfInterface = {
  utils: {
    openUrl(url: string) {
      overwolf.openUrlInDefaultBrowser(url);
    },
  },
};

export default Overwolf;
