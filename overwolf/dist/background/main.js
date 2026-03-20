const checkIfAdmin = async (plugin) => {  
  // Check if running as admin
  // Has the user ignored the warning?
  let hasIgnoredWarning = localStorage.getItem("hasIgnoredAdminWarning");
  
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
  let backgroundProcess = plugin.get().IsJavaStillRunning();
  if (backgroundProcess) {
    console.log('Yeeting old java process');
    plugin.get().YeetOldJavaProcess();
  } else {
    console.log('No old java process');
  }
}

let versionData = null;
let wsData = null;

let licenseData = null;
let javaLicenseData = null;

const setup = async () => {
  // Version data
  versionData = await fetch('../../meta.json').then(e => e.json());
  licenseData = await fetch('../../licenses.json').then(e => e.json()).catch(e => console.error("Failed to load licenses", e));
  javaLicenseData = await fetch('../../java-licenses.json').then(e => e.json()).catch(e => console.error("Failed to load java licenses", e));
  
  const plugin = new OverwolfPlugin('OverwolfShim', true);

  let status = await p(plugin.initialize).catch(e => console.log(e));
  if (status === false) {
    console.error("Plugin couldn't be loaded??");
    return;
  }
  
  let finishedSetup = false;
  
  plugin.get().onData.addListener(({ error, output }) => {
    if (error) console.error(error);
    if (output) {
      console.log(output);
      
      if (!finishedSetup) {
        if (output.includes("Backend Ready! Port=")) {
          const port = parseInt(output.match(/Port=(\d+)/)[1]);
          const secret = output.match(/OneTimeToken=(\w+-\w+-\w+-\w+-\w+)/)[1];
          
          if (!port || !secret || isNaN(port)) {
            console.error("Failed to find port or secret", output)
            return
          }
          
          console.log("Found port and secret", output)
          
          wsData = {
            port: port,
            secret
          }
          
          finishedSetup = true;
        }
      }
    }
  });

  const screenSize = await new Promise(() => overwolf.utils.getMonitorsList((result) => {
    if (!result.displays || result.displays.length === 0) {
      resolve({ width: 1920, height: 1080 });
    }

    // Get the primary monitor
    const primaryMonitor = result.displays.find((display) => display.is_primary);
    const dpiX = primaryMonitor.dpiX || 96; // Default to 96 DPI if not available
    const dpiY = primaryMonitor.dpiY || 96;
    const scaleX = dpiX / 96;
    const scaleY = dpiY / 96;
    const width = Math.floor(primaryMonitor.width / scaleX);
    const height = Math.floor(primaryMonitor.height / scaleY);
    resolve({ width, height }); // If the DPI is normal, a 1080p monitor will return 1920x1080, if it's a 4k monitor with 200% scaling it will return 1920x1080 as well
  })).catch(error => {
    console.error("Failed to get monitor list, defaulting to 1920x1080", error);
    return { width: 1920, height: 1080 };
  });
  
  // Launch the backend
  const owDir = plugin.get().GetOverwolfDir();
  const javaPath = owDir + "\\jdk-21.0.3+9-jre\\bin\\java.exe";
  const pid = plugin.get().GetOverwolfPID();
  const args = serializeToStringList([
    "--pid",
    pid,
    "--overwolf",
    "--screenWidth", screenSize.width,
    "--screenHeight", screenSize.height,
  ])
  
  const jvmArgs = serializeToStringList(parseArgs(versionData.runtime.jvmArgs));
  
  const ftbaDir = plugin.get().GetDotFTBADir();
  const startupResponse = await p(plugin.get().LaunchJava, ftbaDir, javaPath, owDir + "\\" + versionData.runtime.jar, args, jvmArgs);
  console.debug(JSON.stringify(startupResponse));
  
  await checkIfAdmin(plugin);
  await ensureBackendIsStopped(plugin);

  const {pid: processId} = startupResponse;

  console.log(`Java process launched - pid=${processId}`);
  
  // Start the app window
  const windowRet = await p(overwolf.windows.obtainDeclaredWindow, 'index');
  await p(overwolf.windows.restore, windowRet.window.id);
  
  console.log('Showing window');
  overwolf.windows.onStateChanged.addListener(async state => {
    console.log('State changed');
    console.log(state);
    if (state.window_state_ex === 'closed' && state.window_name === 'index') {
      console.log('Closing');
      overwolf.windows.close('background');
    }
  });
  
  return {
    plugin
  }
}

let app = null;
setup()
  .then(e => {
    console.log("Finished setup");
    app = e;
    console.log(app)
  })
  .catch(e => console.error("Failed to setup app", e));

const appFunctions = {
  /**
   * Crypto module is not available in OW so we use C# to do it
   */
  randomUUID: () => {
    return app.plugin.get().RandomUUID();
  },
  wsData: () => wsData,
  async restartApp() {
    // Get the current index window and close it
    const window = await p(overwolf.windows.obtainDeclaredWindow, 'index')
    await p(overwolf.windows.close, window.window.id);

    let windowRet = await p(overwolf.windows.obtainDeclaredWindow, 'index');
    await p(overwolf.windows.restore, windowRet.window.id);
  },
}

function funcs() {
  return appFunctions;
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

function getLicenseData() {
  return licenseData;
}

function getJavaLicenseData() {
  return javaLicenseData;
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
          if (result.status === 'success') {
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

function serializeToStringList(list) {
  return list.join(';');
}

function parseArgs(args) {
  const ourOs = "windows";
  const ourArch = "x64";

  const finalOutput = [];
  for (const arg of args) {
    if (typeof arg === "string") {
      finalOutput.push(arg)
      continue;
    }

    if (arg.filter && arg.filter.os && !arg.filter.os.includes(ourOs)) {
      continue;
    }

    if (arg.filter && arg.filter.arch && !arg.filter.arch.includes(ourArch)) {
      continue;
    }

    finalOutput.push(addArg(arg));
  }

  return finalOutput;
}

function addArg(arg) {
  // This shouldn't happen, but just in case
  if (typeof arg === "string") {
    return arg;
  }

  if (arg.key) {
    return `${arg.key}=${arg.value}`;
  }

  return arg.value;
}