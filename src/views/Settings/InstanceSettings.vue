<template>
  <div class="instance-settings">
    <p class="block uppercase text-white-700 font-bold mb-4">Window Size</p>
    <div class="mb-4">
      <div class="flex items-center mb-2">
        <div class="block flex-1 mr-2">
          <b>Size presets</b>
          <p class="text-muted text-sm">Select a preset based on your system</p>
        </div>
        
        <selection
          v-if="loadedSettings"
          @selected="(e) => e && selectResolution(e)"
          :inheritedSelection="resolutionList.find((e) => e.text === `${localSettings.width} x ${localSettings.height}px`)"
          :style="{width: '240px'}"
          :options="resolutionList" 
        />
      </div>
      <div class="flex items-center mb-2">
        <div class="block flex-1 mr-2">
          <b>Width</b>
          <p class="text-muted text-sm">The Minecraft windows screen width</p>
        </div>
        <ftb-input class="mb-0" v-model="localSettings.width" :value="localSettings.width" @blur="saveMutated" />  
      </div>
      <div class="flex items-center">
        <div class="block flex-1 mr-2">
          <b>Height</b>
          <p class="text-muted text-sm">The Minecraft windows screen height</p>
        </div>
        <ftb-input class="mb-0" v-model="localSettings.height" :value="localSettings.height" @blur="saveMutated" />
      </div>
    </div>
    
    <!--          <ftb-slider label="Default Memory" v-model="localSettings.memory" :currentValue="localSettings.memory" minValue="512" :maxValue="settingsState.hardware.totalMemory" @change="doSave"-->
    <!--                      unit="MB" @blur="doSave" step="128"/>-->

    <ftb-input
      label="Custom Arguments"
      :value="localSettings.jvmargs"
      v-model="localSettings.jvmargs"
      @blur="saveMutated"
    />
    <small class="text-muted block mb-8 max-w-xl">
      These arguments are appended to your instances upon start, they are normal java arguments.
    </small>

    <ftb-input
      label="Instance Location"
      :value="localSettings.instanceLocation"
      :disabled="true"
      v-model="localSettings.instanceLocation"
      button="true"
      buttonText="Browse"
      buttonColor="primary"
      :buttonClick="browseForFolder"
    />
    <small class="text-muted block max-w-xl"
      >Changing your instance location with instances installed will cause your instances to be moved to the new
      location automatically.</small
    >
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';

import { Action, State } from 'vuex-class';
import { Settings, SettingsState } from '@/modules/settings/types';
import platform from '@/utils/interface/electron-overwolf';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import Selection from '@/components/atoms/input/Selection.vue';

@Component({
  components: {
    Selection,
    'ftb-toggle': FTBToggle,
  },
})
export default class InstanceSettings extends Vue {
  @State('settings') public settingsState!: SettingsState;
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @Action('loadSettings', { namespace: 'settings' }) public loadSettings: any;
  
  localSettings: Settings = {} as Settings;
  lastSettings: Settings = {} as Settings;

  loadedSettings = false;

  async created() {
    await this.loadSettings();
    this.loadedSettings = true;

    // Make a copy of the settings so we don't mutate the vuex state
    this.localSettings = { ...this.settingsState.settings };
    this.lastSettings = { ...this.localSettings };
  }

  keepLauncherOpen(value: boolean): void {
    this.localSettings.keepLauncherOpen = value;
    this.saveSettings(this.localSettings);
  }

  saveMutated() {
    // Compare the last settings to the current settings, if they are the same, don't save
    if (JSON.stringify(this.lastSettings) === JSON.stringify(this.localSettings)) {
      return;
    }
    
    this.saveSettings(this.localSettings);
    this.lastSettings = { ...this.localSettings };
  }

  browseForFolder() {
    platform.get.io.selectFolderDialog(this.localSettings.instanceLocation, (path) => {
      if (path == null) {
        return;
      }

      this.localSettings.instanceLocation = path;
      this.saveSettings(this.localSettings);
    });
  }

  selectResolution(id: number) {
    const selected = this.settingsState.hardware.supportedResolutions[id];
    if (!selected) {
      return;
    }
    
    this.localSettings.width = selected.width;
    this.localSettings.height = selected.height;
    this.saveMutated();
  }

  get resolutionList() {
    const resList = [];
    for (const [key, res] of Object.entries(this.settingsState.hardware.supportedResolutions)) {
      resList.push({
        value: key,
        text: `${res.width} x ${res.height}px`,
      })
    }
    return resList;
  }
}
</script>

<style scoped lang="scss">
.flex-1 {
  flex: 1;
}
</style>
