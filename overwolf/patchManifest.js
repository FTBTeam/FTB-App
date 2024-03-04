const path = require('path');
const fs = require('fs');

const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf-8'));
let manifestData = JSON.parse(fs.readFileSync('overwolf/manifest.json', 'utf-8'));

manifestData = {
  ...manifestData,
  meta: {
    ...manifestData.meta,
    version: packageJson.version,
    name: 'FTB App',
    icon: 'icon_256.png',
    icon_gray: 'icon_256.png',
  }
}

fs.writeFileSync('overwolf/manifest.json', JSON.stringify(manifestData));
