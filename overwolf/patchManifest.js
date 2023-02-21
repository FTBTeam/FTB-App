const fs = require('fs');

let versionData = JSON.parse(fs.readFileSync('public/version.json', 'utf-8'));
let manifestData = JSON.parse(fs.readFileSync('overwolf/manifest.json', 'utf-8'));

let version = manifestData.meta.version;
let versionTime = versionData.jarVersion.split('-')[0].substring(2);
version =
    version.split('.')[0] +
    '.' +
    versionTime.substring(0, 2) +
    '.' +
    versionTime.substring(2, 6) +
    '.' +
    versionTime.substring(6);
manifestData.meta.version = version;
if (process.env.CI_RELEASE) {
    manifestData.meta.name = 'FTB App';
    manifestData.meta.icon = 'icon_256.png';
    manifestData.meta.icon_gray = 'icon_256.png';
} else {
    manifestData.meta.name = 'FTB App Preview';
    manifestData.meta.icon = 'icon_256_preview.png';
    manifestData.meta.icon_gray = 'icon_256_preview.png';
}
fs.writeFileSync('overwolf/manifest.json', JSON.stringify(manifestData));
