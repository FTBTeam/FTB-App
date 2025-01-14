// NodeJS
const path = require('path')
const fs = require('fs')
const { javaVersion, installJre } = require('./shared')

const javaPath = path.join(process.cwd(), "overwolf", "jdk-" + javaVersion);

async function runPreInit() {
  if (!fs.existsSync(javaPath)) {
    console.log(`Java missing, installing AdoptOpenJDK ${javaVersion}`)
    
    await installJre(javaVersion, javaPath); 
  } else {
    console.log("Java already installed")
  }
}

runPreInit().catch(e => {
  console.error(e)
  process.exit(1)
})