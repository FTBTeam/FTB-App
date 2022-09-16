let versionData = {};

async function blah() {
  let raw = await fetch('../../version.json');
  let versionObj = await raw.json();
  versionData = versionObj;
  const version = versionObj.jarVersion;
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

const dev = false; // todo: get from overwolf, but only used for launch java currently anyway
blah().catch(e => console.log(e));

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
