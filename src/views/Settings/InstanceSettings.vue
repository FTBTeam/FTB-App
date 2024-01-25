<template>
  <div class="instance-settings" v-if="localSettings.spec">
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
        v-model="localSettings.instanceDefaults.updateChannel"
        :style="{width: '192px'}"
        @change="v => saveMutated()"
      />
    </div>
    <p class="block text-white-700 text-lg font-bold mb-4">Window Size</p>
    <div class="mb-6">
      <ui-toggle label="Fullscreen" desc="Always open Minecraft in Fullscreen mode" v-model="localSettings.instanceDefaults.fullscreen" class="mb-4" @input="() => {
        saveMutated()
      }" />
      
      <div :class="{'cursor-not-allowed opacity-50 pointer-events-none': localSettings.instanceDefaults.fullscreen}">
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
          <ftb-input class="mb-0" v-model="localSettings.instanceDefaults.width" :value="localSettings.instanceDefaults.width" @blur="saveMutated" />  
        </div>
        <div class="flex items-center">
          <div class="block flex-1 mr-2">
            <b>Height</b>
            <small class="text-muted block mt-2">The Minecraft windows screen height</small>
          </div>
          <ftb-input class="mb-0" v-model="localSettings.instanceDefaults.height" :value="localSettings.instanceDefaults.height" @blur="saveMutated" />
        </div>
      </div>
    </div>

    <p class="block text-white-700 text-lg font-bold mb-4">Java</p>
    <ram-slider class="mb-6" v-model="localSettings.instanceDefaults.memory" @change="saveMutated" />
    
    <ftb-input
      label="Custom Arguments"
      placeholder="-TestArgument=120"
      v-model="localSettings.instanceDefaults.javaArgs"
      @blur="saveMutated"
    />
    <small class="text-muted block mb-6 max-w-xl">
      These arguments are appended to your instances upon start, they are normal java arguments.
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
      {{localSettings.instanceDefaults.shellArgs}} java -jar minecraft.jar -Xmx{{prettyByteFormat(Math.floor(parseInt(localSettings.instanceDefaults.memory.toString()) * 1024 * 1000))}} {{localSettings.instanceDefaults.javaArgs}}
    </code>

    <p class="block text-white-700 text-lg font-bold mb-4">Misc</p>
    <ftb-input
      label="Instance Location"
      :value="localSettings.instanceLocation"
      :disabled="true"
      button="true"
      buttonText="Browse"
      buttonColor="primary"
      :buttonClick="moveInstances"
    />
    <small class="text-muted block max-w-xl"
      >Changing your instance location with instances installed will cause your instances to be moved to the new
      location automatically.</small
    >
    
    <modal :open="instanceMoveModalShow" title="Moving instances" :close-on-background-click="false" :has-closer="false">
      <template v-if="!instanceMoveModalComplete">
        <div class="wysiwyg mb-6">
          <p>This may take a while, please wait.</p>

          <p>Moving <code>{{instanceMoveLocations.old}}</code><br/>To <code>{{instanceMoveLocations.new}}</code></p>

          <p>Stage: <code>{{instanceMoveModalStage}}</code></p>
        </div>

        <progress-bar :infinite="true" />
      </template>
      <div class="wysiwyg" v-else>
        <p>Instances moved successfully 🎉</p>
      </div>
      
      <template #footer v-if="instanceMoveModalComplete">
        <div class="flex justify-end">
          <ui-button @click="instanceMoveModalShow = false" type="success" icon="check">Done</ui-button>
        </div>
      </template>
    </modal>
  </div>
  <Loader v-else />
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';

import {Action, State} from 'vuex-class';
import {SettingsState} from '@/modules/settings/types';
import platform from '@/utils/interface/electron-overwolf';
import {alertController} from '@/core/controllers/alertController';
import Selection2 from '@/components/core/ui/Selection2.vue';
import {ReleaseChannelOptions} from '@/utils/commonOptions';
import {computeAspectRatio, emitter, prettyByteFormat} from '@/utils';
import UiToggle from '@/components/core/ui/UiToggle.vue';
import RamSlider from '@/components/core/modpack/components/RamSlider.vue';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {dialogsController} from '@/core/controllers/dialogsController';
import {MoveInstancesHandlerReply, OperationProgressUpdateData, SettingsData} from '@/core/@types/javaApi';
import {toTitleCase} from '@/utils/helpers/stringHelpers';
import UiButton from '@/components/core/ui/UiButton.vue';
import ProgressBar from '@/components/atoms/ProgressBar.vue';
import {InstanceActions} from '@/core/actions/instanceActions';
import Loader from '@/components/atoms/Loader.vue';
import KeyValueEditor from '@/components/core/modpack/components/KeyValueEditor.vue';

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
  
  instanceMoveModalShow = false;
  instanceMoveModalStage = "Preparing";
  instanceMoveModalComplete = false;
  instanceMoveLocations = {old: "", new: ""};
  
  async created() {
    await this.loadSettings();
    this.loadedSettings = true;

    // Make a copy of the settings so we don't mutate the vuex state
    this.localSettings = { ...this.settingsState.settings };
    this.lastSettings = JSON.stringify(this.localSettings)

    this.resolutionId = this.resolutionList
      .find((e) => e.value === `${this.localSettings.instanceDefaults.width ?? ''}|${this.localSettings.instanceDefaults.height ?? ''}`)
      ?.value ?? "";
  }
  
  saveMutated() {
    // Compare the last settings to the current settings, if they are the same, don't save
    if (this.lastSettings === JSON.stringify(this.localSettings)) {
      console.log("Settings are the same, not saving")
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

  async moveInstances() {
    const location: string | null = await new Promise(resolve => {
      platform.get.io.selectFolderDialog(this.localSettings.instanceLocation, (path) => {
        if (path == null) {
          return;
        }

        resolve(path);
      });
    })

    if (!location) {
      return;
    }
    
    if (!(await dialogsController.createConfirmationDialog("Are you sure?", `This will move all your instances\n\nFrom \`${this.localSettings.instanceLocation}\`\n\nTo \`${location}\`\n\nthis may take a while.`))) {
      return;
    }
    
    const result = await sendMessage("moveInstances", {
      newLocation: location
    })
    
    if (result.state === "error") {
      return alertController.error(result.error);
    }
    
    if (result.state === "processing") {
      this.instanceMoveModalShow = true;
      this.instanceMoveModalStage = "Preparing";
      this.instanceMoveModalComplete = false;
      this.instanceMoveLocations = {
        old: this.localSettings.instanceLocation,
        new: location
      }
    }
    
    const migrationResult = await new Promise((res) => {
      const onMoveProgress = (data: any) => {
        if (data.type !== "operationUpdate" && data.type !== "moveInstancesReply") return;
        if (data.type === "operationUpdate") {
          const typedData = data as OperationProgressUpdateData;
          if (typedData.stage === "FINISHED") {
            // It's done
          } else {
            this.instanceMoveModalStage = toTitleCase(typedData.stage.toString().replaceAll("_", " "));
          }
        } else {
          const typedData = data as MoveInstancesHandlerReply;
          if (typedData.state !== "success") {
            // It's broken
            this.instanceMoveModalShow = false;
            alertController.error(typedData.error);
            emitter.off("ws.message", onMoveProgress);
            res(false);
          } else {
            this.instanceMoveModalComplete = true;
            emitter.off("ws.message", onMoveProgress);
            res(true);
          }
        }
      }
      
      emitter.on("ws.message", onMoveProgress);
    })
    
    if (migrationResult) {
      this.localSettings.instanceLocation = location;
      await InstanceActions.clearInstanceCache(false)
    }
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
