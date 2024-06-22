import path from 'path';
import os from 'os';
import {Arg} from '@/utils/interface/electron';
import {getAppHome} from '@/nuturalHelpers';

export function electronAppHome() {
  return getAppHome(os.platform(), os.homedir(), path.join);
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

export function jreLocation(appPath: string) {
  const runtimePath = path.join(appPath, "runtime");

  let javaExecPath = path.join(runtimePath, 'bin', 'java');
  if (os.platform() === "darwin") {
    javaExecPath = path.join(runtimePath, 'Contents', 'Home', 'bin', 'java');
  }
  
  const isWindows = os.platform() === "win32";
  
  return isWindows ? `${javaExecPath}.exe` : javaExecPath;
}

/**
 * Takes in an array of arguments and returns a string based on the provided filters, optional
 * keys, etc.
 * 
 * @param args
 */
export function parseArgs(args: Arg[]): string[] {
  const ourOs = computeOs(os.platform());
  const ourArch = computeArch(os.arch());
  
  const finalOutput: string[] = [];
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

function addArg(arg: Arg): string {
  // This shouldn't happen, but just in case
  if (typeof arg === "string") {
    return arg;
  }
  
  if (arg.key) {
    return `${arg.key}=${arg.value}`;
  }
  
  return arg.value;
}