const path = require('path');
const SentryWebpackPlugin = require('@sentry/webpack-plugin');

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
} else {
  console.warn("Can't run Sentry source map uploader");
}

module.exports = {
  publicPath: './',
  outputDir:
    process.env.TARGET_PLATFORM === 'overwolf'
      ? path.resolve(__dirname, './overwolf/dist/desktop')
      : path.resolve(__dirname, './dist'),
  chainWebpack: (config) => {
    if (process.env.TARGET_PLATFORM === 'overwolf') {
      config.plugin('copy').tap((options) => {
        options[0][0].ignore.push('./public/css/**/*');
        options[0][0].ignore.push('/public/css/**/*');
        options[0][0].ignore.push('public/css/**/*');
        options[0][0].ignore.push('/css/**/*');
        options[0][0].ignore.push('css/**/*');
        options[0][0].ignore.push('./public/img/**/*');
        options[0][0].ignore.push('/public/img/**/*');
        options[0][0].ignore.push('public/img/**/*');
        options[0][0].ignore.push('/img/**/*');
        options[0][0].ignore.push('img/**/*');
        return options;
      });
    }
  },
  pages: {
    index: {
      // entry for the page
      entry: 'src/main.ts',
      template: process.env.TARGET_PLATFORM === 'overwolf' ? 'public/index-overwolf.html' : 'public/index.html',
      filename: 'index.html',
      title: 'FTB App',
      chunks: ['chunk-vendors', 'chunk-common', 'index'],
    },
    chatPage: 'src/main.ts',
  },
  pluginOptions: {
    electronBuilder: {
      nodeIntegration: true,
      builderOptions: {
        generateUpdatesFilesForAllChannels: true,
        productName: 'FTB App',
        appId: 'com.feedthebeast.Launcher',
        mac: {
          // #TODO https://kilianvalkhof.com/2019/electron/notarizing-your-electron-application/
          identity: 'null',
          hardenedRuntime: true,
          gatekeeperAssess: false,
          entitlements: 'build/entitlements.mac.plist',
          entitlementsInherit: 'build/entitlements.mac.plist',
          target: ['dir'],
        },
        win: {
          target: ['portable'],
          artifactName: '${productName}.${ext}',
        },
        linux: {
          target: ['AppImage'],
          category: 'Game',
          artifactName: '${productName}.${ext}',
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
