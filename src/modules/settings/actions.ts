import {ActionTree} from 'vuex';
import {Settings, SettingsState} from './types';
import {RootState} from '@/types';
import platform from '@/utils/interface/electron-overwolf';
import {sendMessage} from '@/core/websockets/websocketsApi';

export function parseValueToType(value: string): any {
  // Convert if it's a boolean
  if (value === 'true' || value === 'false' || value === "True" || value === "False") {
    return value === 'true' || value === "True";
    // Convert if it's a number based on a regex but don't parse int on anything over a 64 bit number
  } else if (value === 'null') {
    return null;
  } else if (value === "undefined") {
    return undefined;
  } else if (value.startsWith("{") && value.endsWith("}") || value.startsWith("[") && value.endsWith("]")) {
    // Convert if it's a json object
    try {
      return JSON.parse(value);
    } catch (e) {
      return value;
    }
  } else if (!value.includes(".") && value.match(/^[0-9]{1,16}$/)) {
    return parseInt(value, 10);
    // Convert if the value is a float 
  } else if (value.includes(".") && value.match(/^[0-9]{1,16}\.[0-9]{1,16}$/)) {
    return parseFloat(value);
  } else if (Number.isNaN(value) || value === "NaN") {
    return NaN;
  } else if (value === "Infinity" || value === "+Infinity" || value === "-Infinity") {
    return value.startsWith("-") ? -Infinity : Infinity;
  } else if (value.endsWith("n") && value.match(/^[0-9]{1,16}n$/)) {
    return BigInt(value);
  }
  
  // It's a string,
  return value;
}

export function writeValueFromType(value: any): string {
  // The inverse of what we do when we parse this stuffs
  if (value === null) {
    return "null";
  } else if (value === undefined) {
    return "undefined";
  } else if (typeof value === "boolean") {
    return value ? "true" : "false";
  } else if (typeof value === "number") {
    return value.toString();
  } else if (typeof value === "bigint") {
    return value.toString() + "n";
  } else if (Array.isArray(value) || typeof value === "object") {
    return JSON.stringify(value);
  } else {
    return value;
  }
}

export const actions: ActionTree<SettingsState, RootState> = {
  async loadSettings({ dispatch, commit }) {
    const result = await sendMessage("getSettings", {});

    if (result.settingsInfo.blockedUesrs !== undefined && !Array.isArray(result.settingsInfo.blockedUesrs)) {
      result.settingsInfo.blockedUsers = JSON.parse(result.settingsInfo.blockedUsers);
    }

    const settings = result.settingsInfo;
    
    let parsedSettings: Record<string, any> = {};
    for (const key of Object.keys(settings)) {
      const value = settings[key] as string;
      parsedSettings[key] = parseValueToType(value);
    }
    
    commit('loadSettings', parsedSettings);
    platform.get.actions.updateSettings(parsedSettings);
    commit('loadHardware', result);
  },
  async saveSettings({ dispatch, commit }, settings: Settings) {
    if (Array.isArray(settings.blockedUsers)) {
      settings.blockedUsers = JSON.stringify(settings.blockedUsers);
    }
    
    // Hack to ensure it complies with the backend datatype and without breaking all the existing configs.
    const newSettings: Record<string, string> = {}
    // Use json stringify to convert the settings to a string
    for (const key of Object.keys(settings)) {
      
      // The inverse of what we do when we parse this stuffs
      const value = (settings as any)[key];
      newSettings[key] = writeValueFromType(value);
    }

    await sendMessage('saveSettings', {
      settingsInfo: newSettings
    })
    
    dispatch('loadSettings');
  },
};
