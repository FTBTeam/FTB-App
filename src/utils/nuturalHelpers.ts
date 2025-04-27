import { MetaData } from '@platform';
import dayjs from "dayjs";

type Joiner = (...paths: string[]) => string;

let metaData: MetaData | null = null;

// Lazy loading basically
export function loadApplicationMetaData(resourcesPath: string, joiner: Joiner, reader: (path: string) => string): MetaData {
  if (!import.meta.env.PROD) {
    return fallbackMetaData;
  }
  
  if (metaData) {
    return metaData;
  }
  
  const metaDataPath = joiner(resourcesPath, "meta.json");
  try {
    const data = reader(metaDataPath);
    const parsed = JSON.parse(data);
    if (parsed) {
      metaData = parsed as MetaData;
      return metaData;
    }
  } catch (e) {
    console.error("Failed to load meta data", e);
  }

  metaData = fallbackMetaData;
  return metaData;
}

export const fallbackMetaData: MetaData = {
  appVersion: "Unknown",
  commit: "Unknown",
  branch: "release",
  released: dayjs().unix(),
  runtime: {
    version: "21", // If we're using this, something has gone wrong
    jar: "app.jar", // If we're using this, something has gone wrong
    env: [],
    jvmArgs: []
  }
}

export function getAppHome(os: string, homeDir: string, joiner: Joiner) {
  if (os === "darwin") {
    return joiner(homeDir, 'Library', 'Application Support', '.ftba');
  } else if (os === "win32") {
    return joiner(homeDir, 'AppData', 'Local', '.ftba');
  } else {
    return joiner(homeDir, '.ftba');
  }
}

export function jreHome(appHome: string, joiner: Joiner, isWindows: boolean, isMac: boolean) {
  const root = joiner(appHome, 'runtime');
  
  let finalPath = joiner(root, 'bin', 'java');
  if (isMac) {
    finalPath = joiner(root, 'Contents', 'Home', 'bin', 'java');
  }
  
  return isWindows ? `${finalPath}.exe` : finalPath;
}

/**
 * Electron can return different values for os.platform() than what the Adoptium api
 * supports. The possible values can be found at {@link os#platform}
 * @param os
 */
export function computeOs(os: string) {
  switch (os) {
    case "aix": return "aix"
    case "sonos": return "solaris" // sunos is solaris
    case "darwin": return "mac"
    case "win32": return "windows"
    default: return "linux"
  }
}

/**
 * Same as {@link computeOs} but for arch instead of os
 * @see computeOs
 * @param arch
 *
 * Electron can be 'arm', 'arm64', 'ia32', 'mips','mipsel', 'ppc', 'ppc64', 's390', 's390x', and 'x64'.
 * API can be: x64, x86, x32, ppc64, ppc64le, s390x, aarch64, arm, sparcv9, riscv64
 */
export function computeArch(arch: string) {
  switch (arch) {
    case "x64":
      return "x64"
    case "x32":
    case "ia32":
      return "x86"
    case "arm64":
      return "aarch64"
    case "arm":
      return "arm"
    case "ppc64":
      return "ppc64"
    case "s390x":
      return "s390x"
    default:
      return "x64"
  }
}