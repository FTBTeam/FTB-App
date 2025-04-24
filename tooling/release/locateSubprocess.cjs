const fs = require('fs');
const path = require('path');

function locateSubprocess() {
  try {
    const subprocessPath = path.resolve('./subprocess');
    const buildPath = path.resolve(subprocessPath, 'build', "libs");

    const files = fs.readdirSync(buildPath);
    console.log("Possible files in subprocess build path: ", files);
    const jarFiles = files.filter(file => file.startsWith('app') && file.endsWith('.jar'));

    if (jarFiles.length === 0) {
      console.error("No launcher jar found");
      return "";
    }

    return path.relative('./', path.join(buildPath, jarFiles[0]));
  } catch (e) {

    return "";
  }
}

module.exports = locateSubprocess;