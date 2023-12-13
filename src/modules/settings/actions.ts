import {ActionTree} from 'vuex';
import {Settings, SettingsState} from './types';
import {RootState} from '@/types';
import platform from '@/utils/interface/electron-overwolf';
import {sendMessage} from '@/core/websockets/websocketsApi';

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
      
      // Convert if it's a boolean
      if (value === 'true' || value === 'false' || value === "True" || value === "False") {
        parsedSettings[key] = value === 'true' || value === "True";
      // Convert if it's a number based on a regex but don't parse int on anything over a 64 bit number
      } else if (value === 'null') {
        parsedSettings[key] = null;
      } else if (value === "undefined") {
        parsedSettings[key] = undefined;
      } else if (value.startsWith("{") && value.endsWith("}") || value.startsWith("[") && value.endsWith("]")) {
        // Convert if it's a json object
        try {
          parsedSettings[key] = JSON.parse(value);
        } catch (e) {
          parsedSettings[key] = value;
        }                
      } else if (!value.includes(".") && value.match(/^[0-9]{1,16}$/)) {
        parsedSettings[key] = parseInt(value, 10);
      // Convert if the value is a float 
      } else if (value.includes(".") && value.match(/^[0-9]{1,16}\.[0-9]{1,16}$/)) {
        parsedSettings[key] = parseFloat(value);
      } else if (Number.isNaN(value) || value === "NaN") {
        parsedSettings[key] = NaN;
      } else if (value === "Infinity" || value === "+Infinity" || value === "-Infinity") {
        parsedSettings[key] = value.startsWith("-") ? -Infinity : Infinity;
      } else if (value.endsWith("n") && value.match(/^[0-9]{1,16}n$/)) {
        parsedSettings[key] = BigInt(value);
      } else {
        parsedSettings[key] = settings[key];
      }
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
      if (value === null) {
        newSettings[key] = "null";
      } else if (value === undefined) {
        newSettings[key] = "undefined";
      } else if (typeof value === "boolean") {
        newSettings[key] = value ? "true" : "false";
      } else if (typeof value === "number") {
        newSettings[key] = value.toString();
      } else if (typeof value === "bigint") {
        newSettings[key] = value.toString() + "n";
      } else if (Array.isArray(value) || typeof value === "object") {
        newSettings[key] = JSON.stringify(value);
      } else {
        newSettings[key] = value;
      }
    }

    await sendMessage('saveSettings', {
      settingsInfo: newSettings
    })
    
    dispatch('loadSettings');
  },
};
