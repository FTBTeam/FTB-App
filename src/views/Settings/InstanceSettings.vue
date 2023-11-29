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
      <ui-toggle label="Fullscreen" desc="Always open Minecraft in Fullscreen mode" v-model="localSettings.fullScreen" class="mb-4" @input="() => {
        saveMutated()
      }" />
      
      <div :class="{'cursor-not-allowed opacity-50 pointer-events-none': localSettings.fullScreen}">
        <div class="flex items-center mb-4">
          <div class="block flex-1 mr-2">
            <b>Size presets</b>
            <small class="text-muted block mt-2">Select a preset based on your system</small>
          </div>

          <selection2
            v-if="resolutionList.length"
            v-model="resolutionId"
            @change="selectResolution"
            :style="{width: '220px'}"
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
    <ram-slider class="mb-6" v-model="localSettings.memory" @change="saveMutated" />

    <ftb-input
      label="Custom Arguments"
      :value="localSettings.jvmargs"
      v-model="localSettings.jvmargs"
      @blur="saveMutated"
    />
    <small class="text-muted block mb-6 max-w-xl">
      These arguments are appended to your instances upon start, they are normal java arguments.
    </small>

    <ftb-input
      label="Shell arguments"
      :value="localSettings.shellArgs"
      v-model="localSettings.shellArgs"
      placeholder="/usr/local/application-wrapper"
      @blur="saveMutated"
    />
    <small class="text-muted block mb-6 max-w-xl">
      These arguments will be inserted before java is run, see the example below. It's recommended to not change these unless you know what you are doing.
    </small>
    
    <p class="mb-2">Startup preview</p>
    <small class="mb-4 block">This is for illustrative purposes only, this is not a complete example.</small>
    
    <code class="block bg-black rounded mb-6 px-2 py-2 overflow-x-auto" v-if="localSettings && localSettings.memory">
      {{localSettings.shellArgs}} java -jar minecraft.jar -Xmx{{prettyByteFormat(Math.floor(parseInt(localSettings.memory.toString()) * 1024 * 1000))}} {{localSettings.jvmargs}}
    </code>

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
import {Component, Vue} from 'vue-property-decorator';

import {Action, State} from 'vuex-class';
import {Settings, SettingsState} from '@/modules/settings/types';
import platform from '@/utils/interface/electron-overwolf';
import {alertController} from '@/core/controllers/alertController';
import Selection2 from '@/components/core/ui/Selection2.vue';
import {ReleaseChannelOptions} from '@/utils/commonOptions';
import {computeAspectRatio, prettyByteFormat} from '@/utils';
import UiToggle from '@/components/core/ui/UiToggle.vue';
import RamSlider from '@/components/core/modpack/components/RamSlider.vue';

@Component({
  methods: {prettyByteFormat},
  components: {
    RamSlider,
    UiToggle,
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

  resolutionId = "";
  
  async created() {
    await this.loadSettings();
    this.loadedSettings = true;

    // Make a copy of the settings so we don't mutate the vuex state
    this.localSettings = { ...this.settingsState.settings };
    this.lastSettings = { ...this.localSettings };

    this.resolutionId = this.resolutionList
      .find((e) => e.value === `${this.localSettings.width ?? ''}|${this.localSettings.height ?? ''}`)
      ?.value ?? "";
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

  selectResolution(id: string) {
    const selected = this.settingsState.hardware.supportedResolutions.find(e => `${e.width}|${e.height}` === id);
    if (!selected) {
      return;
    }
    
    this.localSettings.width = selected.width;
    this.localSettings.height = selected.height;
    this.saveMutated();
  }

  get resolutionList() {
    const resList = [];
    resList.push({
      value: "",
      label: "Custom",
      meta: "Custom"
    });

    for (const res of this.settingsState.hardware.supportedResolutions) {
      resList.push({
        value: `${res.width}|${res.height}`,
        label: `${res.width} x ${res.height}`,
        // Calculate the aspect ratio in the form of a 16:9 for example
        meta: computeAspectRatio(res.width, res.height)
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
