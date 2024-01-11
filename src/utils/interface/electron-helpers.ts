import path from 'path';
import os from 'os';

export function computeOs(os: string) {
  switch (os) {
    case "darwin": return "mac"
    case "win32": return "windows"
    default: return "linux"
  }
}

export function computeArch(arch: string) {
  switch (arch) {
    case "x64":
      return "x64"
    case "x32":
      return "x86"
    case "arm64":
      return "aarch64"
    default:
      return "x64"
  }
}

export function jreLocation(appPath: string) {
  const runtimePath = `${appPath}/runtime`;

  let javaExecPath = path.join(runtimePath, 'bin', 'java');
  if (os.platform() === "darwin") {
    javaExecPath = path.join(runtimePath, 'Contents', 'Home', 'bin', 'java');
  }
  
  return javaExecPath;
}