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
            mainProcessArgs: ['--ws', '18797:2dcef192-f33c-4a5f-8ecf-f6603c8dc87e']
        }
    }
};
