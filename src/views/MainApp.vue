<script lang="ts" setup>
// TODO: [port] Add back 
// async function startApp() {
  // appStarting.value = false;
  // logger.info("Starting app");
  //
  // if (!constants.isProduction) {
  //   logger.info("Starting production app");
  //
  //   // If we're in dev, we just default to localhost
  //   await connectToWebsockets();
  //   return;
  // }
  //
  // logger.info("Sending start subprocess message");
  // try {
  //   let appData: any = await appPlatform.app.startSubprocess();
  //
  //   // False means update
  //   if (!appData) {
  //     try {
  //       await appPlatform.app.updateApp(
  //         (stage: any) => appInstallStage.value = stage,
  //         () => logger.info("Update complete")
  //       );
  //     } catch (e: any) {
  //       appInstallFailed.value = true;
  //       appInstallError.value = e?.customMessage || "The app has failed to update, please try again or contact support...";
  //       logger.error("Failed to update app", e);
  //     }
  //
  //     appData = await appPlatform.app.startSubprocess();
  //   }
  //
  //   logger.info("Started app subprocess", appData);
  //   if (!appData.port || !appData.secret) {
  //     appStartupFailed.value = true;
  //     appInstallError.value = "The app has failed to start due to connection issues, please try again or contact support...";
  //     logger.error("Failed to start subprocess due to connection issues", appData);
  //     return;
  //   }
  //
  //   // Finally we need to get the app to connect to the subprocess
  //   await connectToWebsockets(appData.port, appData.secret);
  // } catch (e: any) {
  //   // Same as the install, grab the custom error if possible
  //   appStartupFailed.value = true;
  //   appInstallError.value = e?.customMessage || "The app has failed to start, please try again or contact support...";
  //   logger.error("Failed to start subprocess", e);
  // }
// }

  // TODO: [port] Add back 
// async function setupApp() {
  // if (!hasInitialized.value) {
  //
  //   logger.debug("Starting ping poll");
  //
  //   hasInitialized.value = true;
  //
  //   checkForFirstRun()
  //     .then(() => logger.info("Finished checking for first run"))
  //     .catch(error => logger.error("Failed to check for first run", error));
  // }
  //
  // appPlatform.actions.onAppReady();
  //
  // logger.debug("Notifying all controllers of connected status")
// }
</script>

<template>
</template>