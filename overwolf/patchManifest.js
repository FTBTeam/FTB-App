const path = require('path');
const fs = require('fs');

console.log(path.resolve('public/version.json'));

let versionData = JSON.parse(fs.readFileSync('public/version.json', 'utf-8'));
let manifestData = JSON.parse(fs.readFileSync('overwolf/manifest.json', 'utf-8'));

let version = manifestData.meta.version;
let versionTime = versionData.jarVersion.split('-')[0].substring(2);

version =
  version.split('.')[0] +
  '.' +
  versionTime.substring(0, 2) +
  '.' +
  parseInt(versionTime.substring(2, 6)) +
  '.' +
  parseInt(versionTime.substring(6));

manifestData = {
  ...manifestData,
  meta: {
    ...manifestData.meta,
    version,
    name: 'FTB App',
    icon: 'icon_256.png',
    icon_gray: 'icon_256.png',
  }
}

fs.writeFileSync('overwolf/manifest.json', JSON.stringify(manifestData));
