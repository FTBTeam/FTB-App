const path = require('path');

module.exports = {
  publicPath: './',
  outputDir:
    process.env.TARGET_PLATFORM === 'overwolf'
      ? path.resolve(__dirname, './overwolf/dist/desktop')
      : path.resolve(__dirname, './dist'),
  chainWebpack: config => {
    if (process.env.TARGET_PLATFORM === 'overwolf') {
      config.plugin('copy').tap(options => {
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
      title: 'FTBApp',
      chunks: ['chunk-vendors', 'chunk-common', 'index'],
    },
    chatPage: 'src/main.ts',
  },
  pluginOptions: {
    electronBuilder: {
      nodeIntegration: true,
      builderOptions: {
        electronBuilder: true,
        generateUpdatesFilesForAllChannels: true,
        productName: 'ftbapp',
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
};
