const path = require('path');
const SentryWebpackPlugin = require('@sentry/webpack-plugin');
const {globSync} = require("glob");
const fs = require('fs');
const yaml = require('yaml')
const {execSync} = require('child_process');

const webpackPlugins = [];

if (process.env.CI_COMMIT_TAG) {
  webpackPlugins.push(
    new SentryWebpackPlugin({
      authToken: process.env.SENTRY_AUTH_TOKEN,
      org: 'creeperhost',
      project: 'ftb-app',
      release: `${process.env.VERSION || process.env.VERSION_OVERRIDE}-${process.env.VUE_APP_PLATFORM}`,
      include: process.env.TARGET_PLATFORM === 'overwolf' ? './overwolf/dist/desktop/' : './dist_electron/bundled/',
      ignore: ['node_modules', 'webpack.config.js'],
      urlPrefix: process.env.TARGET_PLATFORM === 'overwolf' 
        ? `overwolf-extension://${process.env.CI_COMMIT_BRANCH === 'release' ? "cmogmmciplgmocnhikmphehmeecmpaggknkjlbag" : "nelapelmednbnaigieobbdgbinpgcgkfmmdjembg"}/dist/desktop/` 
        : ""
    }),
  );
}

// This is required, the amount of times the IDE will import this just for using the variable is insane
if (process.env.NODE_ENV === 'production') {
  console.log("Checking for bad imports")
  const start = Date.now();
  const badStrings = [
    "import * as process from 'process'",
    "import * as process from \"process\"",
    "import process from 'process'",
    "import process from \"process\"",
  ]
  
  // Don't allow a build if "import * as process from 'process'" exists in any file
  const sourceFiles = globSync('src/**/*.{ts,js,vue}', {ignore: ['node_modules/**']});
  for (const file of sourceFiles) {
    // Stream the file for performance
    const stream = fs.createReadStream(file);
    let found = false;
    let buffer = Buffer.alloc(0);
    stream.on('data', (chunk) => {
      buffer = Buffer.concat([buffer, chunk]);
      for (const badString of badStrings) {
        if (buffer.includes(badString)) {
          found = true;
          stream.close();
          break;
        }
      }
    });
    
    stream.on('close', () => {
      if (found) {
        console.error(`Found "import * as process from 'process'" in ${file}`);
        process.exit(1);
      }
    });
  }
  
  console.log(`Checked for bad imports in ${Date.now() - start}ms`);
}

let hasRepackedJar = false;

module.exports = {
  publicPath: './',
  outputDir:
    process.env.TARGET_PLATFORM === 'overwolf'
      ? path.resolve(__dirname, './overwolf/dist/desktop')
      : path.resolve(__dirname, './dist'),
  productionSourceMap: process.env.NODE_ENV !== 'production',
  pages: {
    prelaunch: {
      entry: 'src/prelaunch.ts',
      template: 'public/prelaunch.html',
      filename: 'prelaunch.html',
      title: 'FTB App',
      chunks: ['chunk-vendors', 'chunk-common', 'prelaunch'],
      sourceMap: false,
    },
    index: {
      // entry for the page
      entry: 'src/main.ts',
      template: process.env.TARGET_PLATFORM === 'overwolf' ? 'public/index-overwolf.html' : 'public/index.html',
      filename: 'index.html',
      title: 'FTB App',
      chunks: ['chunk-vendors', 'chunk-common', 'index'],
      sourceMap: false,
    },
    chatPage: {
      entry: 'src/main.ts',
      sourceMap: false,
    }
  },
  pluginOptions: {
    electronBuilder: {
      nodeIntegration: true,
      customFileProtocol: `ftb://./`,
      builderOptions: {
        productName: 'FTB App',
        appId: 'dev.ftb.app',
        copyright: `Copyright © ${new Date().getFullYear()} - Feed The Beast Ltd`,
        publish: {
          provider: 's3',
          bucket: 'ftb-app-updates',
          endpoint: 's3.eu-central-003.backblazeb2.com',
          path: 'ftb-app-updates'
        },
        beforePack: async (context) => {
          if (context.electronPlatformName === 'darwin' && !hasRepackedJar) {
            await signJnilibInJar(context);
            hasRepackedJar = true; // Don't do this more than once
          }
        },
        afterPack: (context) => {
          const appPath = context.appOutDir;
          let appUpdatePath = path.join(appPath, 'resources', 'app-update.yml');
          if(context.electronPlatformName === 'darwin') {
            appUpdatePath = path.join(appPath, context.packager.appInfo.productName + '.app', 'Contents', 'Resources', 'app-update.yml');
          }
   
          const appUpdate = fs.readFileSync(appUpdatePath, 'utf8');
          const parsedData = yaml.parse(appUpdate);

          parsedData.endpoint = 'https://piston.feed-the-beast.com';
          delete parsedData.path;
          
          // Replace the backblaze URL with the correct one
          fs.writeFileSync(appUpdatePath, yaml.stringify(parsedData));
        },
        extraResources: [
          {from: "subprocess/build/libs/", to: "", filter: ["launcher-*.jar"]},
          {from: "subprocess/build/libs/java-licenses.json", to: ""},
          {from: "./licenses.json", to: ""},
          {from: "subprocess/build/libs/meta.json", to: ""},
        ],
        win: {
          target: ['nsis'],
          artifactName: '${productName}-${version}-${arch}.${ext}',
        },
        mac: {
          hardenedRuntime: true,
          gatekeeperAssess: false,
          target: ['default'], // We use default in order to allow for updates. DMG only allows installation.
          artifactName: '${productName}-${version}-${arch}.${ext}',
          category: 'public.app-category.games',
          binaries: [
            getPathToLauncher()
          ]
        },
        linux: {
          target: ['dir', 'AppImage', 'deb', 'rpm'],
          category: 'Game',
          artifactName: '${productName}-${version}-${arch}.${ext}',
          synopsis: 'FTB Desktop App for downloading and managing Modpacks',
        },
        directories: {
          output: 'release',
          buildResources: 'resources',
        },
      },
    },
  },
  configureWebpack: {
    plugins: webpackPlugins,
  },
};

function getPathToLauncher() {
  try {
    const subprocessPath = path.resolve(__dirname, 'subprocess');
    const buildPath = path.resolve(subprocessPath, 'build', "libs");

    const files = fs.readdirSync(buildPath);
    const jarFiles = files.filter(file => file.startsWith('launcher') && file.endsWith('.jar'));

    if (jarFiles.length === 0) {
      console.error("No launcher jar found");
      return "";
    }

    return path.relative(__dirname, path.join(buildPath, jarFiles[0]));
  } catch (e) {
    
    return "";
  }
}

async function signJnilibInJar(context) {
  const packer = context.packager;
  const keychainFile = (await packer.codeSigningInfo.value).keychainFile
  
  const signingIdentity = '5372643C69B1D499BDF6EA772082E9CE99E85029';
  const entitlementsPath = './node_modules/.pnpm/@overwolf+ow-app-builder-lib@24.7.0/node_modules/@overwolf/ow-app-builder-lib/templates/entitlements.mac.plist';
  
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
    console.log(`codesign --sign ${signingIdentity} --force --timestamp --options runtime --entitlements ${entitlementsPath} ${toSignFile} --keychain ${keychainFile}`)
    execSync(`codesign --sign ${signingIdentity} --force --timestamp --options runtime --entitlements ${entitlementsPath} ${toSignFile} --keychain ${keychainFile}`);
    
    // Move the file back to its original name
    fs.renameSync(toSignFile, file);

    // Update the JAR file with the signed file
    console.info(`Updating ${file} in ${jar}`);
    execSync(`jar --update --file=${absoluteJar} ${file}`);
  });
}