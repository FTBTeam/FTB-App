const path = require('path');
const SentryWebpackPlugin = require('@sentry/webpack-plugin');
const fs = require('fs');
const packageJson = require('./package.json');

const webpackPlugins = [];

//process.env.CI_COMMIT_BRANCH === 'develop' add back when we start using preview builds in production
if (
  process.env.SENTRY_AUTH_TOKEN &&
  (process.env.VERSION || process.env.VERSION_OVERRIDE) &&
  (process.env.CI_COMMIT_BRANCH === 'release' || process.env.CI_COMMIT_BRANCH === 'develop')
) {
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

/**
 * A simple plugin that allows for dynamic generation of assets
 */
const VersionDataPlugin = (options) => {
  return {
    apply: (compiler) => {
      compiler.hooks.emit.tapAsync('VersionDataPlugin', (compilation, callback) => {
        if (!options.predicate(compilation)) {
          return callback();
        }
        
        const { fileName, content } = options;

        compilation.assets[fileName] = {
          source: () => content,
          size: () => content.length
        }
        
        callback();
      })
    }
  }
}

webpackPlugins.push(VersionDataPlugin({
  predicate: () => process.env.TARGET_PLATFORM === 'overwolf' && process.env.NODE_ENV !== 'production',
  fileName: 'version.json',
  content: JSON.stringify({
    "jarVersion": "invalid-as-dev-does-not-use-the-jar",
    "webVersion": "no-a-version",
    "publicVersion": packageJson.version,
    "branch": "development",
    "timestampBuilt": Date.now(),
    "javaLicense": {}
  })
}));

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
        productName: 'FTBApp',
        appId: 'com.feedthebeast.Launcher',
        
        mac: {
          // #TODO https://kilianvalkhof.com/2019/electron/notarizing-your-electron-application/
          identity: 'null',
          hardenedRuntime: true,
          gatekeeperAssess: false,
          entitlements: 'build/entitlements.mac.plist',
          entitlementsInherit: 'build/entitlements.mac.plist',
          target: ['dir'],
          artifactName: '${productName}-mac.${ext}',
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
