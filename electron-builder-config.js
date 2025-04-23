import { globSync } from 'glob';
import fs from 'fs';
import path from 'path';
import { execSync } from 'child_process';

let hasRepackedJar = false;

/**
 * @type {import("electron-builder").Configuration}
 */
const config = {
  productName: 'FTB Electron App',
  asar: true,
  appId: 'dev.ftb.app',
  copyright: `Copyright © ${new Date().getFullYear()} - Feed The Beast Ltd`,
  generateUpdatesFilesForAllChannels: true,
  publish: {
    provider: 'generic',
    url: "https://piston.feed-the-beast.com/app",
  },
  protocols: [
    {
      name: "FTB App Protocol",
      schemes: [
        "ftb"
      ]
    }
  ],
  beforePack: async (context) => {
    if (context.electronPlatformName === 'darwin' && !hasRepackedJar) {
      await signJnilibInJar(context);
      hasRepackedJar = true; // Don't do this more than once
    }

    // Remove the sourcemaps before packing as we only need them for sentry upload
    const sourceMapFiles = globSync(`./dist_electron/**/*.js.map`, {ignore: ['node_modules/**']});
    for (const sourceMapFile of sourceMapFiles) {
      fs.unlinkSync(sourceMapFile);
    }

    // Quickly patch all the .js files to remove their //# sourceMappingURL=
    const jsFiles = globSync(`./dist_electron/**/*.js`, {ignore: ['node_modules/**']});
    for (const jsFile of jsFiles) {
      let file = fs.readFileSync(jsFile, 'utf-8');
      file = file.replace(/^\/\/# sourceMappingURL=.*\.js\.map$/gm, '');
      fs.writeFileSync(jsFile, file);
    }
  },
  extraResources: [
    {from: "subprocess/build/libs/", to: "", filter: ["app-*.jar"]},
    {from: "subprocess/build/libs/java-licenses.json", to: ""},
    {from: "./licenses.json", to: ""},
    {from: "subprocess/build/libs/meta.json", to: ""},
  ],
  win: {
    target: ['nsis'],
    artifactName: 'ftb-app-win-${version}-${arch}.${ext}',
    sign: './tooling/signing/windows-signing.js',
    signingHashAlgorithms: ["sha256"],
    icon: "./resources/icons/app-icon-windows.ico",
    publisherName: "Feed The Beast Ltd",
  },
  mac: {
    hardenedRuntime: true,
    gatekeeperAssess: false,
    target: ['default'], // We use default in order to allow for updates. DMG only allows installation.
    artifactName: 'ftb-app-macos-${version}-${arch}.${ext}',
    category: 'public.app-category.games',
    binaries: [
      getPathToLauncher()
    ]
  },
  linux: {
    target: ['dir', 'AppImage', 'deb', 'rpm'],
    category: 'Game',
    artifactName: 'ftb-app-linux-${version}-${arch}.${ext}',
    synopsis: 'FTB Desktop App for downloading and managing Modpacks',
  },
  directories: {
    output: 'release',
    buildResources: 'resources',
  },
  dmg: {
    window: {
      width: 540,
      height: 380,
    },
    contents: [
      {
        x: 154,
        y: 175,
        type: 'file',
      },
      {
        x: 385,
        y: 175,
        type: 'link',
        path: '/Applications',
      },
    ]
  }
}

function getPathToLauncher() {
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

async function signJnilibInJar(context) {
  if (!process.env.GITHUB_REF_NAME) {
    return;
  }

  const packer = context.packager;
  const keychainFile = (await packer.codeSigningInfo.value).keychainFile

  const signingIdentity = '5372643C69B1D499BDF6EA772082E9CE99E85029';
  const entitlementsPath = './resources/entitlements.mac.plist';

  const jar = getPathToLauncher();
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

export default config;