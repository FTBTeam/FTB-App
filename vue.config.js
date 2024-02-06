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
        extraResources: [
          {from: "subprocess/build/libs/", to: "", filter: ["launcher-*.jar"]},
          {from: "subprocess/build/libs/java-licenses.json", to: ""},
          {from: "./licenses.json", to: ""},
          {from: "subprocess/build/libs/meta.json", to: ""},
        ],
        win: {
          target: ['nsis'],
        },
        mac: {
          // #TODO https://kilianvalkhof.com/2019/electron/notarizing-your-electron-application/
          identity: 'null',
          hardenedRuntime: true,
          gatekeeperAssess: false,
          entitlements: 'build/entitlements.mac.plist',
          entitlementsInherit: 'build/entitlements.mac.plist',
          target: ['dir'],
          artifactName: '${productName}-mac.${ext}',
          category: 'public.app-category.games',
        },
        linux: {
          target: ['dir'],
          category: 'Game',
          artifactName: '${productName}-linux.${ext}',
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
