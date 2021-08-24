const fs = require('fs');
const childProcess = require('child_process');
let manifestFile = fs.readFileSync('manifest.json', 'utf-8');
let manifestData = JSON.parse(manifestFile);
let version = manifestData.meta.version;
function makeOPK() {
  return childProcess.execSync(
    'zip -9 -r "FTB App - ' +
      version +
      '.opk" dist/ jdk-11.0.8+10-jre/ FTBOverwolfShim.dll assets/icon_256.png *.jar assets/launchericon.ico manifest.json version.json -x windows/*',
  );
}
makeOPK();
