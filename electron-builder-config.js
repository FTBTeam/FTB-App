import locateSubprocess from './tooling/release/locateSubprocess.cjs';

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
  beforePack: './tooling/release/signJniLibs.cjs',
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
      locateSubprocess(),
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

export default config;