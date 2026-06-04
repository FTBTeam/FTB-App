import log from "electron-log/main";
import fs from "node:fs";
import path from "node:path";
import {screen} from "electron";
import {spawn} from "node:child_process";
import {appHome} from "./main.ts";
import {AppData} from "./app";

export function startSubprocess(appData: AppData, args: any) {
  log.debug("Starting subprocess")
  if (!import.meta.env.PROD) {
    log.debug("Not starting subprocess in dev mode")
    return;
  }

  let userDefinedJvmArgs = [] as string[];
  try {
    if (fs.existsSync(path.join(appHome, ".subprocess-jvm-args"))) {
      const jvmArgsData = fs.readFileSync(path.join(appHome, ".subprocess-jvm-args"), 'utf-8');
      // Each line its own arg
      userDefinedJvmArgs = jvmArgsData.split("\n")
        .map(arg => arg.trim())
        .filter(arg => arg.length > 0);
      log.info("Found .subprocess-jvm-args file, forcing IPv4 for subprocess");
    }
  } catch (e) {
    log.error("Failed to read .subprocess-jvm-args file", e)
  }

  log.log("Starting subprocess")

  const javaPath = args.javaPath;
  const argsList = args.args as string[]
  const env = args.env as string[]

  if (appData.subprocess !== null && appData.state.ws === null) {
    // Check if the process is still running
    let pid = appData.subprocess.pid;
    appData.subprocess.unref();
    appData.subprocess = null;
    appData.state.ws = null;

    // Kill the subprocess
    if (pid) {
      try {
        process.kill(pid, 'SIGINT');
      } catch (e) {
        log.error("Failed to kill subprocess", e)
      }
    }

    log.debug("Subprocess is already running, killing it")
  }

  if (appData.subprocess && appData.state.ws !== null) {
    log.debug("Subprocess is already running, returning existing connection data", appData.state.ws)
    return {
      port: appData.state.ws.port,
      secret: appData.state.ws.token
    }
  }

  const correctedPath = javaPath.replace(/\\/g, "/");
  log.debug("Starting subprocess", correctedPath, argsList)

  const electronPid = process.pid;
  argsList.push(...["--pid", "" + electronPid, "--electron"])
  if (process.argv.includes("ignore-pid-checks")) {
    argsList.push("--ignore-pid-checks")
  }

  const primaryDisplay = screen.getPrimaryDisplay();
  const { width, height } = primaryDisplay.workAreaSize;

  argsList.push(
    "--screenWidth", "" + width,
    "--screenHeight", "" + height
  )

  // Inject java args before the -jar argument
  if (userDefinedJvmArgs.length > 0) {
    const jarIndex = argsList.findIndex(arg => arg === '-jar');
    if (jarIndex !== -1) {
      argsList.splice(jarIndex, 0, ...userDefinedJvmArgs);
      log.debug("Injected user defined JVM args into subprocess args", userDefinedJvmArgs);
    }
  }

  // Spawn the process so it can run in the background and capture the output
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => {
      reject("Subprocess timed out")
    }, 30_000) // 30 seconds

    const mappedEnv = env.reduce((curr, itt) => {
      const [key, value] = itt.split("=");
      curr[key] = value;
      return curr;
    }, {} as Record<string, string>)

    log.debug("Mapped env", mappedEnv)
    log.debug("Args list", argsList)

    appData.state.isSubprocessSetup = false;
    const subprocess = spawn(correctedPath, argsList, {
      detached: true,
      stdio: ['ignore', 'pipe', 'pipe'], // ignore stdin, pipe stdout and stderr
      env: {
        ...process.env,
        ...mappedEnv,
      },
      cwd: appHome
    });
    
    appData.subprocess = subprocess;

    subprocess.stderr?.on('data', (data) => {
      let outputData = data.toString();
      if (outputData.endsWith("\n")) {
        outputData = outputData.slice(0, -1);
      }
      log.debug("Subprocess stderr", outputData)
    });

    subprocess.stdout?.on('data', (data) => {
      let outputData = data.toString();
      if (outputData.endsWith("\n")) {
        outputData = outputData.slice(0, -1);
      }

      log.debug("Subprocess stdout", outputData)
      if (!appData.state.isSubprocessSetup) {
        if (data.includes("Backend Ready! Port=")) {
          const port = parseInt(outputData.match(/Port=(\d+)/)![1]);
          const secret = outputData.match(/OneTimeToken=(\w+-\w+-\w+-\w+-\w+)/)![1];

          if (!(!port || !secret || isNaN(port))) {
            log.debug('Found port and secret', port, secret);
            appData.state.isSubprocessSetup = true;
            clearTimeout(timeout);

            if (subprocess?.pid) {
              appData.state.ws = {
                pid: subprocess.pid,
                port: port,
                token: secret,
              }

              log.debug('Cached process data', appData.state.ws);
            }

            resolve({
              port,
              secret,
            });
          }
        }
      }
    });

    subprocess?.on('error', (err) => {
      log.error("Subprocess error", err)
    });

    subprocess.on('exit', (code, signal) => {
      log.debug("Subprocess exited", code, signal)
    });

    subprocess?.on('close', (code) => {
      log.debug("Subprocess closed", code)
    });
  })
}