const fs = require('fs');
const archiver = require('archiver')

const versionData = JSON.parse(fs.readFileSync('public/version.json', 'utf-8'));
const manifestData = JSON.parse(fs.readFileSync('overwolf/manifest.json', 'utf-8'));
const archive = archiver('zip', {zlib: {level: 9}})

const output = fs.createWriteStream(`overwolf/${manifestData.meta.name} - ${manifestData.meta.version}.opk`);
output.on('close', function () {
    console.log(archive.pointer() + ' total bytes');
});
archive.on('entry', function(e) {
    console.log(" " + e.name)
})
archive.pipe(output)
console.log("Building opk..")
archive.directory('overwolf/dist/', 'dist')
archive.directory('overwolf/jdk-17.0.1+12-minimal/', 'jdk-17.0.1+12-minimal')
archive.file("overwolf/OverwolfShim.dll", {name: 'OverwolfShim.dll'})
archive.file("overwolf/launchericon.ico", {name: 'launchericon.ico'})
archive.file(`overwolf/${manifestData.meta.icon}`, {name: manifestData.meta.icon})
archive.file("overwolf/manifest.json", {name: 'manifest.json'})
archive.file("overwolf/version.json", {name: 'version.json'})
archive.file(`subprocess/build/libs/launcher-${versionData.jarVersion}-all.jar`, {name: `launcher-${versionData.jarVersion}-all.jar`})

archive.finalize()
