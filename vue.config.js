const path = require('path');
const SentryWebpackPlugin = require('@sentry/webpack-plugin');
const {globSync} = require("glob");
const fs = require('fs');
const yaml = require('yaml')
const {execSync} = require('child_process');
const {notarize} = require("@electron/notarize");

const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf-8'));

const webpackPlugins = [];

if (process.env.GITHUB_REF_NAME) {
  webpackPlugins.push(
    new SentryWebpackPlugin({
      authToken: process.env.SENTRY_AUTH_TOKEN,
      org: 'creeperhost',
      project: 'ftb-app',
      release: `${packageJson.version}`,
      dist: process.env.APP_TARGET_PLATFORM ?? "unknown",
      include: process.env.TARGET_PLATFORM === 'overwolf' ? './overwolf/dist/desktop/' : './dist_electron/bundled/',
      ignore: ['node_modules', 'webpack.config.js'],
      urlPrefix: process.env.TARGET_PLATFORM === 'overwolf' 
        ? `overwolf-extension://cmogmmciplgmocnhikmphehmeecmpaggknkjlbag/dist/desktop/` 
        : "ftb://./"
    }),
  );
}

// This is required, the amount of times the IDE will import this just for using the variable is insane
if (process.env.GITHUB_REF_NAME) {
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
  productionSourceMap: true,
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
        generateUpdatesFilesForAllChannels: true,
        publish: {
          provider: 'generic',
          url: "https://piston.feed-the-beast.com/app"
        },
        beforePack: async (context) => {          
          if (context.electronPlatformName === 'darwin' && !hasRepackedJar) {
            await signJnilibInJar(context);
            hasRepackedJar = true; // Don't do this more than once
          }
          
          // Remove the sourcemaps before packing as we only need them for sentry upload
          const sourceMapFiles = globSync(`${__dirname}/dist_electron/**/*.js.map`, {ignore: ['node_modules/**']});
          for (const sourceMapFile of sourceMapFiles) {
            fs.unlinkSync(sourceMapFile);
          }
          
          // Quickly patch all the .js files to remove their //# sourceMappingURL=
          const jsFiles = globSync(`${__dirname}/dist_electron/**/*.js`, {ignore: ['node_modules/**']});
          for (const jsFile of jsFiles) {
            let file = fs.readFileSync(jsFile, 'utf-8');
            file = file.replace(/^\/\/# sourceMappingURL=.*\.js\.map$/gm, '');
            fs.writeFileSync(jsFile, file);
          }
        },
        afterSign: async (context) => {
          const { electronPlatformName, appOutDir } = context;
          if (electronPlatformName !== 'darwin') {
            return;
          }
          
          if (!process.env.APPLE_API_KEY) {
            console.error("No apple api key found");
            return;
          }

          const appName = context.packager.appInfo.productFilename;
          return await notarize({
            appBundleId: 'dev.ftb.app',
            appPath: `${appOutDir}/${appName}.app`,
            appleApiKey: "./apple_api_key.p8", // Only exists at CI time
            appleApiKeyId: process.env.APPLE_API_KEY_ID,
            appleApiIssuer: process.env.APPLE_API_ISSUER,
          });
        },
        extraResources: [
          {from: "subprocess/build/libs/", to: "", filter: ["launcher-*.jar"]},
          {from: "subprocess/build/libs/java-licenses.json", to: ""},
          {from: "./licenses.json", to: ""},
          {from: "subprocess/build/libs/meta.json", to: ""},
        ],
        win: {
          target: ['nsis'],
          artifactName: 'ftb-app-${version}-${arch}.${ext}',
        },
        mac: {
          hardenedRuntime: true,
          gatekeeperAssess: false,
          target: ['default'], // We use default in order to allow for updates. DMG only allows installation.
          artifactName: 'ftb-app-${version}-${arch}.${ext}',
          category: 'public.app-category.games',
          binaries: [
            getPathToLauncher()
          ]
        },
        linux: {
          target: ['dir', 'AppImage', 'deb', 'rpm'],
          category: 'Game',
          artifactName: 'ftb-app-${version}-${arch}.${ext}',
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
  if (!process.env.GITHUB_REF_NAME) {
    return;
  }
  
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
    execSync(`codesign --sign ${signingIdentity} --force --timestamp --options runtime --entitlements ${entitlementsPath} ${toSignFile} --keychain ${keychainFile}`);
    
    // Move the file back to its original name
    fs.renameSync(toSignFile, file);

    // Update the JAR file with the signed file
    console.info(`Updating ${file} in ${jar}`);
    execSync(`jar --update --file=${absoluteJar} ${file}`);
  });
}