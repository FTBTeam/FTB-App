<template>
  <div class="instance-settings" v-if="localSettings.spec">
    <div class="mb-6">
      <h1 class="font-bold text-xl mb-2">Instance defaults</h1>
      <p class="text-muted">These settings are used when creating a new instance or installing a modpack. These settings will <b>Not</b> updated existing modpacks settings.</p>
    </div>
    
    <p class="block text-white-700 text-lg font-bold mb-4">Updates</p>
    <div class="flex items-center mb-4 border-b border-white border-opacity-25 pb-6">
      <div class="block flex-1 mr-2">
        <b>Release Channel</b>
        <small class="block text-muted mr-6 mt-2">
          The selected release channel will determine when we show that a supported modpack has an update.<span class="mb-2 block" /> Release is the most stable, then Beta should be playable and Alpha could introduce game breaking bugs.</small>
      </div>

      <selection2
        v-if="loadedSettings"
        :options="channelOptions"
        v-model="localSettings.instanceDefaults.updateChannel"
        :style="{width: '192px'}"
        @change="() => saveMutated()"
      />
    </div>
    <p class="block text-white-700 text-lg font-bold mb-4">Window Size</p>
    <div class="mb-4 border-b border-white border-opacity-25 pb-6">
      <ui-toggle label="Fullscreen" desc="Always open Minecraft in Fullscreen mode" v-model="localSettings.instanceDefaults.fullscreen" class="mb-4" @input="() => {
        saveMutated()
      }" />

      <ui-toggle label="Use presets" desc="You can pick between preset window sizes or entering your own values for the windows width and height." v-model="usePresets" class="mb-4" />
      
      <div :class="{'cursor-not-allowed opacity-50 pointer-events-none': localSettings.instanceDefaults.fullscreen}">
        <template v-if="usePresets">
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
        </template>
        <template v-else>
          <div class="flex items-center mb-4">
            <div class="block flex-1 mr-2">
              <b>Width</b>
              <small class="text-muted block mt-2">The Minecraft windows screen width</small>
            </div>
            <ftb-input class="mb-0" v-model="localSettings.instanceDefaults.width" :value="localSettings.instanceDefaults.width" @blur="saveMutated" />
          </div>
          <div class="flex items-center">
            <div class="block flex-1 mr-2">
              <b>Height</b>
              <small class="text-muted block mt-2">The Minecraft windows screen height</small>
            </div>
            <ftb-input class="mb-0" v-model="localSettings.instanceDefaults.height" :value="localSettings.instanceDefaults.height" @blur="saveMutated" />
          </div>
        </template>
      </div>
    </div>

    <p class="block text-white-700 text-lg font-bold mb-4">Java</p>

    <ui-toggle 
      label="Disable helper mod injection"
      desc="The FTB App will inject helper mods into your instance to help the app and your instance work together. Sometimes this can cause issues with Minecraft, Mods, Etc. You can disable this behaviour here." 
      v-model="localSettings.instanceDefaults.preventMetaModInjection" 
      class="mb-4" @input="() => {
        saveMutated()
      }" />
    
    <ram-slider class="mb-6" v-model="localSettings.instanceDefaults.memory" @change="saveMutated" />

    <div class="flex gap-4 flex-col mb-6">
      <label class="block tracking-wide text-white-700 font-bold">
        Java runtime arguments
      </label>

      <small class="text-muted block -mt-2 max-w-xl">
        These arguments are appended to your instances upon start, they are normal java arguments. <em>New lines will be removed.</em>
      </small>

      <textarea
        placeholder="-TestArgument=120"
        v-model="localSettings.instanceDefaults.javaArgs"
        @blur="() => {
          // Remove all new lines and trim the string
          localSettings.instanceDefaults.javaArgs = localSettings.instanceDefaults.javaArgs.trim().replaceAll(/(\r\n|\n|\r)/gm, '')
          saveMutated()
        }"
        spellcheck="false"
        rows="4"
        class="flex-1 mb-0 appearance-none block w-full ftb-btn bg-input text-gray-400 border border-input py-3 px-4 focus:outline-none rounded resize-none break-normal font-mono"
      />

      <div class="flex gap-4">
        <ui-button size="small" icon="undo" @click="() => {
            localSettings.instanceDefaults.javaArgs = '-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M'
            saveMutated()
          }">
          Reset to Vanilla defaults
        </ui-button>
      </div>
    </div>

    <ftb-input
      label="Program arguments"
      :value="localSettings.instanceDefaults.programArgs"
      v-model="localSettings.instanceDefaults.programArgs"
      placeholder="--fullscreen"
      @blur="saveMutated"
    />
    <small class="text-muted block mb-6 max-w-xl">
      These arguments are appended to the end of the java command, these are typically arguments Minecraft uses.
    </small>
    
    <ftb-input
      label="Shell arguments"
      :value="localSettings.instanceDefaults.shellArgs"
      v-model="localSettings.instanceDefaults.shellArgs"
      placeholder="/usr/local/application-wrapper"
      @blur="saveMutated"
    />
    <small class="text-muted block mb-6 max-w-xl">
      These arguments will be inserted before java is run, see the example below. It's recommended to not change these unless you know what you are doing.
    </small>
    
    <p class="mb-2">Startup preview</p>
    <small class="mb-4 block">This is for illustrative purposes only, this is not a complete example.</small>
    
    <code class="block bg-black rounded mb-6 px-2 py-2 overflow-x-auto" v-if="localSettings && localSettings.instanceDefaults.memory">
      {{localSettings.instanceDefaults.shellArgs}} java {{localSettings.instanceDefaults.javaArgs}} -Xmx{{prettyByteFormat(Math.floor(parseInt(localSettings.instanceDefaults.memory.toString()) * 1024 * 1000))}} -jar minecraft.jar {{localSettings.instanceDefaults.programArgs}}
    </code>
  </div>
  <Loader v-else />
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';

import {Action, State} from 'vuex-class';
import {SettingsState} from '@/modules/settings/types';
import {alertController} from '@/core/controllers/alertController';
import Selection2 from '@/components/ui/Selection2.vue';
import {ReleaseChannelOptions} from '@/utils/commonOptions';
import {computeAspectRatio, prettyByteFormat} from '@/utils';
import UiToggle from '@/components/ui/UiToggle.vue';
import RamSlider from '@/components/groups/modpack/components/RamSlider.vue';
import {SettingsData} from '@/core/@types/javaApi';
import UiButton from '@/components/ui/UiButton.vue';
import ProgressBar from '@/components/ui/ProgressBar.vue';
import Loader from '@/components/ui/Loader.vue';
import KeyValueEditor from '@/components/groups/modpack/components/KeyValueEditor.vue';

@Component({
  methods: {prettyByteFormat},
  components: {
    KeyValueEditor,
    Loader,
    ProgressBar,
    UiButton,
    RamSlider,
    UiToggle,
    Selection2
  },
})
export default class InstanceSettings extends Vue {
  @State('settings') public settingsState!: SettingsState;
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @Action('loadSettings', { namespace: 'settings' }) public loadSettings: any;
  
  localSettings: SettingsData = {} as SettingsData;
  lastSettings: string = ""

  loadedSettings = false;

  resolutionId = "";
  usePresets = false;
  
  async created() {
    await this.loadSettings();
    this.loadedSettings = true;

    // Make a copy of the settings so we don't mutate the vuex state
    this.localSettings = { ...this.settingsState.settings };
    this.lastSettings = JSON.stringify(this.localSettings)

    this.resolutionId = this.resolutionList
      .find((e) => e.value === `${this.localSettings.instanceDefaults.width ?? ''}|${this.localSettings.instanceDefaults.height ?? ''}`)
      ?.value ?? "";
    
    if (this.resolutionId !== "") {
      this.usePresets = true;
    }
  }
  
  saveMutated() {
    // Compare the last settings to the current settings, if they are the same, don't save
    if (this.lastSettings === JSON.stringify(this.localSettings)) {
      return;
    }
    
    alertController.success("Settings saved")
    this.saveSettings(this.localSettings);
    this.lastSettings = JSON.stringify(this.localSettings)
  }
  
  selectResolution(id: string) {
    const selected = this.settingsState.hardware.supportedResolutions.find(e => `${e.width}|${e.height}` === id);
    if (!selected) {
      return;
    }
    
    this.localSettings.instanceDefaults.width = selected.width;
    this.localSettings.instanceDefaults.height = selected.height;
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
