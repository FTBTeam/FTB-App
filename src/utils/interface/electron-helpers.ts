import {Arg} from '@/utils/interface/impl/electron.ts';
import { computeArch, computeOs } from '@/nuturalHelpers';

const { os } = window.nodeUtils;

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