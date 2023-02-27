const fs = require('fs');
const childProcess = require('child_process');
const semver = require('semver');

let versionTypeIncrease = process.argv[2];
let isPreview = false;
if (process.argv.length > 3) {
  isPreview = true;
}

function getGitCommitHash() {
  return childProcess
    .execSync('git rev-parse HEAD', { stdio: 'inherit' })
    .toString()
    .trim()
    .substring(0, 8);
}

function buildFrontend() {
  return childProcess.execSync('cd ../ && yarn install && yarn run vue:build:overwolf', { stdio: 'inherit' });
}

let versionFile = fs.readFileSync('version.json', 'utf-8');
let versionData = JSON.parse(versionFile);
let manifestFile = fs.readFileSync('manifest.json', 'utf-8');
let manifestData = JSON.parse(manifestFile);
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
if (isPreview) {
  manifestData.meta.name = 'FTB App Preview';
  manifestData.meta.icon = 'icon_256_preview.png';
  manifestData.meta.icon_gray = 'icon_256_preview.png';
} else {
  manifestData.meta.name = 'FTB App';
  manifestData.meta.icon = 'icon_256.png';
  manifestData.meta.icon_gray = 'icon_256.png';
}
fs.writeFileSync('manifest.json', JSON.stringify(manifestData));
buildFrontend();
