let versionData = {};

async function blah() {
  let raw = await fetch('../desktop/version.json');
  let versionObj = await raw.json();
  versionData = versionObj;
  const version = versionObj.jarVersion;
  
  dev = versionObj.branch !== 'release';
  var plugin = new OverwolfPlugin('OverwolfShim', true);
  let status = await p(plugin.initialize).catch(e => console.log(e));
  if (status == false) {
    console.error("Plugin couldn't be loaded??");
    return;
  }

  plugin.get().onData.addListener(({ error, output }) => {
    if (error) {
      if (error.indexOf('New Port:') !== -1) {
        let port = error.substring(error.indexOf('New Port:') + 'New Port:'.length).trim();
        websocketPort = Number(port);
        console.log('Set new port', port, websocketPort);
        if (newWebsocketCallback) {
          newWebsocketCallback(websocketPort, secret === undefined, secret, dev);
        }
      }
      console.log(error);
    }

    if (output) {
      console.log(output);
    }
  });
  
  // Has the user ignored the warning?
  let hasIgnoredWarning = localStorage.getItem("hasIgnoredAdminWarning");
  
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
  
  // If the background process is already running, close it
  let backgroundProcess = plugin.get().IsJavaStillRunning();
  if (backgroundProcess) {
    console.log('Yeeting old java process');
    plugin.get().YeetOldJavaProcess();
  } else {
    console.log('No old java process');
  }
  
  let javaResp = await p(plugin.get().LaunchJava, version, dev);

  console.log(JSON.stringify(javaResp));

  let pid = javaResp.pid;

  console.log(`Java process launched - pid=${pid}`);
  let windowRet = await p(overwolf.windows.obtainDeclaredWindow, 'index');
  await p(overwolf.windows.restore, windowRet.window.id);
  console.log('Showing window');
  overwolf.windows.onStateChanged.addListener(async state => {
    console.log('State changed');
    console.log(state);
    if (state.window_state_ex == 'closed' && state.window_name == 'index') {
      console.log('Closing');
      if (server !== undefined) {
        server.close();
      }
      overwolf.windows.close('background');
    }
  });
  
  return plugin;
}

let server;
let websocketPort = 13377;
let authDataCallbacks = [];
let authData;
let protocolURL = undefined;
let newWebsocketCallback;
let secret;
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

function setNewWebsocketCallback(callback) {
  newWebsocketCallback = callback;
}

function setWSData(data) {
  console.log('Setting data', data);
  secret = data.secret;
  websocketPort = Number(data.port);
}

function getWebsocketData() {
  return { port: websocketPort, secret, dev };
}

async function closeEverythingAndLeave() {}

function setAuthData(authdata) {
  authData = authdata;
}

async function restartApp() {
  // Get the current index window and close it
  const window = await p(overwolf.windows.obtainDeclaredWindow, 'index')
  await p(overwolf.windows.close, window.window.id);
  
  let windowRet = await p(overwolf.windows.obtainDeclaredWindow, 'index');
  await p(overwolf.windows.restore, windowRet.window.id);
}

function onRequest(info) {
  let data = JSON.parse(info.content);
  if ((data.token && data.token.length > 0) || (data.key && data.iv && data.password)) {
    server.close();
    server = undefined;
    authDataCallbacks.forEach(cb => cb(data));
    authDataCallbacks = [];
    authData = data;
  } else {
    server.close();
    server = undefined;
  }
}

function getVersionData() {
  return versionData;
}

function getAuthData() {
  return authData;
}

function addCallback(authDataCallback) {
  authDataCallbacks.push(authDataCallback);
}

async function openWebserver(authDataCallback) {
  authDataCallbacks.push(authDataCallback);
  if (server !== undefined) {
    return server;
  }
  let response = await p(overwolf.web.createServer, 7755);
  if (response.status === 'success') {
    server = response.server;
    server.onRequest.removeListener(onRequest);
    server.onRequest.addListener(onRequest);
    server.listen(info => {
      console.log('Server listening status on port ' + 7755 + ' : ' + info);
    });
    return server;
  } else {
    return null;
  }
}

let dev = false; // todo: get from overwolf, but only used for launch java currently anyway
let plugin = null; // TODO: all of this code is awful

blah()
  .then(e => plugin = e)
  .catch(e => console.log(e));

function randomUUID() {
  return plugin.get().RandomUUID()
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
