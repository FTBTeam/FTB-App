const path = require('path');
const fs = require('fs');
const { execSync } = require('child_process');
const locateSubprocess = require('./locateSubprocess.cjs');

let hasRepackedJar = false;

module.exports = async function (context) {
  if (!process.env.GITHUB_REF_NAME) {
    return;
  }

  if (context.electronPlatformName !== 'darwin' || hasRepackedJar) {
    return;  
  }

  hasRepackedJar = true;
  const packer = context.packager;
  const keychainFile = (await packer.codeSigningInfo.value).keychainFile

  const signingIdentity = '5372643C69B1D499BDF6EA772082E9CE99E85029';
  const entitlementsPath = './resources/entitlements.mac.plist';

  const jar = locateSubprocess();
  const absoluteJar = path.resolve(jar);
  if (jar === "") {
    throw new Error("No launcher jar found");
  }

// Make a tmp directory to store the jnilib files
  if (!fs.existsSync('tmp')) {
    fs.mkdirSync('tmp');
  }

// Expand the jar 
  console.log("Expanding jar");
  execSync(`jar --extract --file=${absoluteJar}`, {cwd: 'tmp'});

// Find all the jnilib files
  const files = execSync(`find tmp -name '*.jnilib'`, {encoding: 'utf-8'})
    .split('\n')
    .filter(file => file); // filter out empty strings

  if (files.length === 0) {
    // This is almost definitely a mistake
    throw new Error("No jnilib files found");
  }

  console.log("Signing jnilib files in jar");
  files.forEach(file => {
    const toSignFile = `${file}-tosign`;

    // Move the file to a new name with -tosign appended
    fs.renameSync(file, toSignFile);

    // Sign the file
    execSync(`codesign --sign ${signingIdentity} --force --timestamp --options runtime --entitlements ${entitlementsPath} ${toSignFile} --keychain ${keychainFile}`);

    // Move the file back to its original name
    fs.renameSync(toSignFile, file);

    // Update the JAR file with the signed file
    console.info(`Updating ${file} in ${jar}`);
    execSync(`jar --update --file=${absoluteJar} ${file.replace("tmp/", "")}`, {cwd: 'tmp'});
  });
}