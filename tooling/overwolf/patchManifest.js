const path = require('path');
const fs = require('fs');

const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf-8'));
let manifestData = JSON.parse(fs.readFileSync('overwolf/manifest.json', 'utf-8'));

// Overwolf do not allow -beta, -alpha
const version = packageJson.version.split("-")[0];

manifestData = {
  ...manifestData,
  meta: {
    ...manifestData.meta,
    version: version,
    name: 'FTB App',
    icon: 'icon_256.png',
    icon_gray: 'icon_256.png',
  }
}

fs.writeFileSync('overwolf/manifest.json', JSON.stringify(manifestData));
