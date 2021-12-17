const fs = require('fs');
const childProcess = require('child_process');
let manifestFile = fs.readFileSync('manifest.json', 'utf-8');
let manifestData = JSON.parse(manifestFile);
let version = manifestData.meta.version;

function makeOPK() {
  try {
    childProcess.execSync(
      'zip -9 -r "FTB App - ' +
        version +
        '.opk" dist/ jdk-17.0.1+12-minimal/ FTBOverwolfShim.dll icon_256.png *.jar launchericon.ico manifest.json version.json -x windows/*',
    );
  } catch (e) {
    console.log(e);
  }
}

makeOPK();
