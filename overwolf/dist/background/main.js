const checkIfAdmin = async () => {  
  // Check if running as admin
  let isRunningAsAdmin = plugin.get().IsRunningAsAdministrator();
  if (isRunningAsAdmin && !hasIgnoredWarning) {
    console.warn("APP IS RUNNING AS ADMIN!")

    // Show error and on confirm close the app
    const windowRet = await p(overwolf.windows.getCurrentWindow);
    const windowId = windowRet.window.id;

    const result = await p(overwolf.windows.displayMessageBox, {
      message_title: "Running as Admin!",
      message_body: "We do not recommend running this app as admin, please close the app and run it normally. If you continue, you may experience issues with the app.",
      confirm_button_text: "Continue regardless",
      cancel_button_text: "Close app!",
      message_box_icon:  overwolf.windows.enums.MessagePromptIcon.ExclamationMark
    })

    if (!result || result.success === false) {
      console.error("Failed to display message box");
      return;
    }

    if (!result.confirmed) {
      await p(overwolf.windows.close, windowId);
    } else {
      localStorage.setItem("hasIgnoredAdminWarning", "true");
    }
  } else {
    console.log("App is not running as admin");
    console.log(`User has ignored warning: ${hasIgnoredWarning === "true" ? "yes" : "no"}`);
  }
}

const ensureBackendIsStopped = async (plugin) => {
  let backgroundProcess = plugin.IsJavaStillRunning();
  if (backgroundProcess) {
    console.log('Yeeting old java process');
    plugin.YeetOldJavaProcess();
  } else {
    console.log('No old java process');
  }
}

let versionData = null;
const setup = async () => {
  // Version data
  versionData = await fetch('../../version.json').then(e => e.json());
  
  const plugin = new OverwolfPlugin('OverwolfShim', true);

  let status = await p(plugin.initialize).catch(e => console.log(e));
  if (status == false) {
    // TODO: Warn user
    console.error("Plugin couldn't be loaded??");
    return;
  }
  
  plugin.get().onData.addListener(({ error, output }) => {
    if (error) console.error(error);
    if (output) console.log(output);
  });
  
  await checkIfAdmin();
  await ensureBackendIsStopped(plugin);
  
  // Launch the backend
  const startupResponse = await p(plugin.get().LaunchJava, version, false);
  console.debug(JSON.stringify(startupResponse));

  const {pid: processId} = startupResponse;

  console.log(`Java process launched - pid=${processId}`);
  
  // Start the app window
  const windowRet = await p(overwolf.windows.obtainDeclaredWindow, 'index');
  await p(overwolf.windows.restore, windowRet.window.id);
  
  console.log('Showing window');
  overwolf.windows.onStateChanged.addListener(async state => {
    console.log('State changed');
    console.log(state);
    if (state.window_state_ex == 'closed' && state.window_name == 'index') {
      // Close the webserver and background if the main app window closes
      console.log('Closing');
      if (server !== undefined) {
        server.close();
      }
      overwolf.windows.close('background');
    }
  });
  
  return {
    plugin
  }
}

let app = null;
setup()
  .then(e => app = e)
  .catch(e => console.error("Failed to setup app", e));

const funcs = {
  randomUUID: () => app.plugin.get().randomUUID(),
  wsData: () => app.wsData,
  async restartApp() {
    // Get the current index window and close it
    const window = await p(overwolf.windows.obtainDeclaredWindow, 'index')
    await p(overwolf.windows.close, window.window.id);

    let windowRet = await p(overwolf.windows.obtainDeclaredWindow, 'index');
    await p(overwolf.windows.restore, windowRet.window.id);
  },
  async openWebserver(authDataCallback) {
    if (app.webserver !== undefined) {
      return app.webserver;
    }
    let response = await p(overwolf.web.createServer, 7755);
    if (response.status === 'success') {
      app.webserver = response.server;
      
      const onRequest = info => {
        let data = JSON.parse(info.content);
        if ((data.token && data.token.length > 0) || (data.key && data.iv && data.password)) {
          app.webserver.close();
          app.webserver = undefined;
          authDataCallback(data);
        } else {
          app.webserver.close();
          app.webserver = undefined;
        }
      }
      
      app.webserver.onRequest.removeListener(onRequest);
      app.webserver.onRequest.addListener(onRequest);
      app.webserver.listen(info => {
        console.log('Server listening status on port ' + 7755 + ' : ' + info);
      });
      return app.webserver;
    } else {
      return null;
    }
  }
}

let protocolURL = undefined;
if (location.href.indexOf('source') !== -1) {
  let queryString = location.href.substring(location.href.indexOf('.html') + 6);
  let source = queryString.substring(7, queryString.indexOf('&'));
  if (source === 'urlscheme') {
    let url = queryString.substring(queryString.indexOf('&') + 1);
    protocolURL = decodeURIComponent(url);
  }
}

function getProtocolURL() {
  return protocolURL;
}

function getVersionData() {
  return versionData;
}

async function p(func, ...args) {
  return new Promise((acc, rej) => {
    let realArgs = [
      ...args,
      result => {
        if (typeof result === 'boolean') {
          if (result) {
            acc(result);
          } else {
            rej(result);
          }
        } else if ('success' in result) {
          if (result.success) {
            acc(result);
          } else {
            rej(result);
          }
        } else if ('status' in result) {
          if (result.status == 'success') {
            acc(result);
          } else {
            rej(result);
          }
        } else {
          acc(result); // assume valid as API isn't consistent
        }
      },
    ];
    func.apply(null, realArgs);
  });
}
