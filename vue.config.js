const path = require('path');
const SentryWebpackPlugin = require('@sentry/webpack-plugin');
const glob = require('glob');
const {globSync} = require("glob");
const fs = require('fs');

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

module.exports = {
  publicPath: './',
  outputDir:
    process.env.TARGET_PLATFORM === 'overwolf'
      ? path.resolve(__dirname, './overwolf/dist/desktop')
      : path.resolve(__dirname, './dist'),
  productionSourceMap: process.env.NODE_ENV !== 'production',
  pages: {
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
        afterPack: (context) => {
          if(context.electronPlatformName === 'darwin') {
            const appPath = context.appOutDir;
            let appUpdatePath = path.join(appPath, context.packager.appInfo.productName + '.app', 'Contents', 'Resources', 'app-update.yml');
            const appUpdate = fs.readFileSync(appUpdatePath, 'utf8');
            
            // Replace the backblaze URL with the correct one
            const newAppUpdate = appUpdate.replace('s3.eu-central-003.backblazeb2.com', 'https://piston.feed-the-beast.com').replace("path: ftb-app-updates", "");
            fs.writeFileSync(appUpdatePath, newAppUpdate);
          }
          if(context.electronPlatformName === 'win32' || context.electronPlatformName === 'linux') {
            const appPath = context.appOutDir;
            const appUpdatePath = path.join(appPath, 'resources', 'app-update.yml');
            const appUpdate = fs.readFileSync(appUpdatePath, 'utf8');
            
            // Replace the backblaze URL with the correct one
            const newAppUpdate = appUpdate.replace('s3.eu-central-003.backblazeb2.com', 'https://piston.feed-the-beast.com').replace("path: ftb-app-updates", "");
            fs.writeFileSync(appUpdatePath, newAppUpdate);
          }
        },
        extraResources: [
          {from: "subprocess/build/libs/", to: "", filter: ["launcher-*.jar"]},
          {from: "subprocess/build/libs/java-licenses.json", to: ""},
          {from: "./licenses.json", to: ""},
          {from: "subprocess/build/libs/meta.json", to: ""},
        ],
        win: {
          target: ['nsis'],
          artifactName: '${productName}-${version}-win.${ext}',
        },
        mac: {
          hardenedRuntime: true,
          gatekeeperAssess: false,
          target: ['dmg'],
          artifactName: '${productName}-${version}-mac.${ext}',
          category: 'public.app-category.games',
          binaries: [
            "subprocess/build/libs/launcher-*.jar"
          ]
        },
        linux: {
          target: ['dir'],
          category: 'Game',
          artifactName: '${productName}-${version}-linux.${ext}',
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
