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
    .execSync('git rev-parse HEAD')
    .toString()
    .trim()
    .substring(0, 8);
}

function buildFrontend() {
  return childProcess.execSync('cd ../ && npm install && npm run vue:build:overwolf');
}

let versionFile = fs.readFileSync('version.json', 'utf-8');
let versionData = JSON.parse(versionFile);
versionData.webVersion = getGitCommitHash();
versionData.timeStampBuilt = new Date().getTime();
fs.writeFileSync('version.json', JSON.stringify(versionData));
let manifestFile = fs.readFileSync('manifest.json', 'utf-8');
let manifestData = JSON.parse(manifestFile);
let version = manifestData.meta.version;
let date = new Date();
let yy = date
  .getFullYear()
  .toString()
  .substring(2);
version =
  version.split('.')[0] +
  '.' +
  yy +
  '.' +
  (date.getMonth() + 1) +
  date.getDate() +
  '.' +
  date.getHours() +
  date.getMinutes();
manifestData.meta.version = version;
if (isPreview) {
  manifestData.meta.name = 'FTB App Preview';
  manifestData.meta.icon = 'assets/icon_256_preview.png';
  manifestData.meta.icon_gray = 'assets/icon_256_preview.png';
} else {
  manifestData.meta.name = 'FTB App';
  manifestData.meta.icon = 'assets/icon_256.png';
  manifestData.meta.icon_gray = 'assets/icon_256.png';
}
fs.writeFileSync('manifest.json', JSON.stringify(manifestData));
buildFrontend();
