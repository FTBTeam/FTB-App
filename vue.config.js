module.exports = {
    pluginOptions: {
        electronBuilder: {
            builderOptions: {
                generateUpdatesFilesForAllChannels: true,
                productName: "ftbapp",
                appId: "com.feedthebeast.Launcher",
                mac: {
                    // #TODO https://kilianvalkhof.com/2019/electron/notarizing-your-electron-application/
                    identity: 'null',
                    hardenedRuntime : true,
                    gatekeeperAssess: false,
                    entitlements: "build/entitlements.mac.plist",
                    entitlementsInherit: "build/entitlements.mac.plist",
                    target: [
                        "dir"
                    ]
                },
                win: {
                    target: [
                        "portable"
                    ],
                    artifactName: "${productName}.${ext}"
                },
                linux: {
                    target: [
                        "AppImage"
                    ],
                    category: "Game",
                    artifactName: "${productName}.${ext}"
                },
                directories: {
                    output: "release"
                }
            },
            mainProcessArgs: ['--ws', '18623:b3d2a72d-9876-483f-add7-2c3c3ce85b44']
        }
    }
};
