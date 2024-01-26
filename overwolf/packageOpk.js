const fs = require('fs');
const archiver = require('archiver')

// Check if we're given an output path
let outputPath = "overwolf/"
let createZip = false
console.log(process.argv)
if (process.argv.length > 3) {
  // Try and find it
  let foundPath = undefined;
  for (let i = 0; i < process.argv.length; i++) {
    if (process.argv[i] === "--output") {
      foundPath = process.argv[i + 1]
      break
    }
    
    if (process.argv[i] === "--zip") {
      createZip = true
    }
  }
  
  if (!outputPath || !fs.existsSync(foundPath)) {
    console.error("Invalid output path")
    process.exit(1)
  }
  
  outputPath = foundPath
}

console.log("Using output path: " + outputPath)

// Only available at buildtime
const metaData = JSON.parse(fs.readFileSync('./meta.json', 'utf-8'));

const manifestData = JSON.parse(fs.readFileSync('overwolf/manifest.json', 'utf-8'));
const archive = archiver('zip', {zlib: {level: 9}})

const output = fs.createWriteStream(`${outputPath}/${manifestData.meta.name} - ${manifestData.meta.version}.opk`);
let zipOutput = undefined
if (createZip) {
  zipOutput = fs.createWriteStream(`${outputPath}/${manifestData.meta.name} - ${manifestData.meta.version}.zip`);
}

output.on('close', function () {
    console.log(archive.pointer() + ' total bytes');
});

archive.on('entry', function(e) {
    console.log(" " + e.name)
})

archive.pipe(output)

if (zipOutput) {
  archive.pipe(zipOutput)
}

console.log("Building opk..")
archive.directory('overwolf/dist/', 'dist')
archive.directory('overwolf/jdk-17.0.1+12-minimal/', 'jdk-17.0.1+12-minimal')
archive.file("overwolf/OverwolfShim.dll", {name: 'OverwolfShim.dll'})
archive.file("overwolf/launchericon.ico", {name: 'launchericon.ico'})
archive.file(`overwolf/${manifestData.meta.icon}`, {name: manifestData.meta.icon})
archive.file("overwolf/manifest.json", {name: 'manifest.json'})
archive.file("overwolf/meta.json", {name: 'meta.json'})
archive.file("overwolf/java-licenses.json", {name: 'java-licenses.json'})
archive.file("./licenses.json", {name: './licenses.json'})
archive.file(`subprocess/build/libs/${metaData.runtime.jar}`, {name: metaData.runtime.jar})

archive.finalize()
