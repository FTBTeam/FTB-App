<template>
  <div class="instance-settings">
    <p class="block text-white-700 text-lg font-bold mb-4">Updates</p>
    <div class="flex items-center mb-6">
      <div class="block flex-1 mr-2">
        <b>Release Channel</b>
        <small class="block text-muted mr-6 mt-2">
          The selected release channel will determine when we show that a supported modpack has an update.<span class="mb-2 block" /> Release is the most stable, then Beta should be playable and Alpha could introduce game breaking bugs.</small>
      </div>

      <selection2
        v-if="loadedSettings"
        :options="channelOptions"
        v-model="localSettings.updateChannel"
        :style="{width: '192px'}"
        @change="v => saveMutated()"
      />
    </div>
    <p class="block text-white-700 text-lg font-bold mb-4">Window Size</p>
    <div class="mb-6">
      <ftb-toggle
        label="Fullscreen"
        :value="localSettings.fullScreen"
        @change="v => {
          localSettings.fullScreen = v;
          saveMutated();
        }"
        class="mb-4"
        small="Always open Minecraft in Fullscreen mode"
      />
      
      <div :class="{'cursor-not-allowed opacity-50 pointer-events-none': localSettings.fullScreen}">
        <div class="flex items-center mb-4">
          <div class="block flex-1 mr-2">
            <b>Size presets</b>
            <small class="text-muted block mt-2">Select a preset based on your system</small>
          </div>
          
          <selection
            v-if="loadedSettings"
            @selected="(e) => e && selectResolution(e)"
            :inheritedSelection="resolutionList.find((e) => e.text === `${localSettings.width} x ${localSettings.height}px`)"
            :style="{width: '192px'}"
            :options="resolutionList" 
          />
        </div>
        <div class="flex items-center mb-4">
          <div class="block flex-1 mr-2">
            <b>Width</b>
            <small class="text-muted block mt-2">The Minecraft windows screen width</small>
          </div>
          <ftb-input class="mb-0" v-model="localSettings.width" :value="localSettings.width" @blur="saveMutated" />  
        </div>
        <div class="flex items-center">
          <div class="block flex-1 mr-2">
            <b>Height</b>
            <small class="text-muted block mt-2">The Minecraft windows screen height</small>
          </div>
          <ftb-input class="mb-0" v-model="localSettings.height" :value="localSettings.height" @blur="saveMutated" />
        </div>
      </div>
    </div>

    <p class="block text-white-700 text-lg font-bold mb-4">Java</p>
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

    <p class="block text-white-700 text-lg font-bold mb-4">Misc</p>
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
import { alertController } from '@/core/controllers/alertController';
import Selection2 from '@/components/atoms/input/Selection2.vue';
import {ReleaseChannelOptions} from '@/utils/commonOptions';

@Component({
  components: {
    Selection,
    'ftb-toggle': FTBToggle,
    Selection2
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
    
    alertController.success("Settings saved")
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
  
  get channelOptions() {
    return ReleaseChannelOptions();
  }
}
</script>

<style scoped lang="scss">
.flex-1 {
  flex: 1;
}
</style>
