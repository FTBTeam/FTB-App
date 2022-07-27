<template>
  <div class="instance-settings">
    <p class="block uppercase text-white-700 font-bold mb-4">Window Size</p>

    <div class="sm:flex mb-2">
      <div class="flex items-center">
        <ftb-input label="Width" v-model="localSettings.width" :value="localSettings.width" @blur="saveMutated" />
        <font-awesome-icon class="mt-auto mb-6 mx-4 text-gray-600" icon="times" size="1x" />
        <ftb-input label="Height" v-model="localSettings.height" :value="localSettings.height" @blur="saveMutated" />
      </div>

      <div class="mb-2 sm:pl-4 mt-2 sm:mt-0 self-end">
        <v-selectmenu
          :title="false"
          :query="false"
          :data="resolutionList"
          align="left"
          :value="resSelectedValue"
          @values="resChange"
        >
          <ftb-button color="primary" class="py-2 px-4 my-1 flex items-center">
            <font-awesome-icon icon="desktop" size="1x" class="cursor-pointer" />
            <span class="ml-4">Resolutions</span>
          </ftb-button>
        </v-selectmenu>
      </div>
    </div>

    <small class="text-muted block mb-8 max-w-xl"> This sets your default Minecraft window size </small>

    <!--    <ftb-toggle-->
    <!--      label="Keep launcher open when game starts"-->
    <!--      :value="localSettings.keepLauncherOpen"-->
    <!--      @change="keepLauncherOpen"-->
    <!--      onColor="bg-primary"-->
    <!--    />-->

    <!--    <small class="text-muted block mb-8 max-w-xl">-->
    <!--      Keeps your launcher-->
    <!--    </small>-->

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

@Component({
  components: {
    'ftb-toggle': FTBToggle,
  },
})
export default class InstanceSettings extends Vue {
  @State('settings') public settingsState!: SettingsState;
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @Action('loadSettings', { namespace: 'settings' }) public loadSettings: any;
  @Action('showAlert') public showAlert: any;
  localSettings: Settings = {} as Settings;

  resSelectedValue: string = '0';

  async created() {
    await this.loadSettings();

    // Make a copy of the settings so we don't mutate the vuex state
    this.localSettings = { ...this.settingsState.settings };
  }

  keepLauncherOpen(value: boolean): void {
    this.localSettings.keepLauncherOpen = value;
    this.saveSettings(this.localSettings);
  }

  saveMutated() {
    this.saveSettings(this.localSettings);
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

  resChange(data: any) {
    if (data && data.length) {
      if (this.resSelectedValue === data[0].value) {
        return;
      }
      this.resSelectedValue = data[0].value;
      this.localSettings.width = this.settingsState.hardware.supportedResolutions[data[0].id].width;
      this.localSettings.height = this.settingsState.hardware.supportedResolutions[data[0].id].height;

      this.saveMutated();
      return;
    }
  }

  get resolutionList() {
    const resList = [];
    for (const [key, res] of Object.entries(this.settingsState.hardware.supportedResolutions)) {
      resList.push({ id: key, name: res.width + 'x' + res.height, value: key });
    }
    return resList;
  }
}
</script>

<style scoped lang="scss"></style>
