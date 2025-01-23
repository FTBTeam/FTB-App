const fs = require('fs');
const archiver = require('archiver')
const path = require('path')
const {installJre, javaVersion} = require("./shared");

// Relative to the project root
const projectRoot = path.join(process.cwd(), "overwolf")

// Check if we're given an output path
let outputPath = projectRoot
let createZip = false

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

async function packageOpk() {
  console.log("Using output path: " + outputPath)

  // Only available at buildtime
  const metaData = JSON.parse(fs.readFileSync(path.join(projectRoot, 'meta.json'), 'utf-8'));

  const manifestData = JSON.parse(fs.readFileSync(path.join(projectRoot, 'manifest.json'), 'utf-8'));
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

  console.log("Download JRE")
  await installJre(javaVersion, path.join(projectRoot, "jdk-" + javaVersion))

  console.log("Building opk..")
  archive.directory('overwolf/dist/', 'dist')
  archive.directory('overwolf/jdk-21.0.3+9-jre/', 'jdk-21.0.3+9-jre')
  archive.file("overwolf/OverwolfShim.dll", {name: 'OverwolfShim.dll'})
  archive.file("overwolf/launchericon.ico", {name: 'launchericon.ico'})
  archive.file(`overwolf/${manifestData.meta.icon}`, {name: manifestData.meta.icon})
  archive.file("overwolf/manifest.json", {name: 'manifest.json'})
  archive.file("overwolf/meta.json", {name: 'meta.json'})
  archive.file("overwolf/java-licenses.json", {name: 'java-licenses.json'})
  archive.file("./licenses.json", {name: './licenses.json'})
  archive.file(`subprocess/build/libs/${metaData.runtime.jar}`, {name: metaData.runtime.jar})

  await archive.finalize()
}

packageOpk().catch(e => {
  console.error(e)
  process.exit(1)
})
