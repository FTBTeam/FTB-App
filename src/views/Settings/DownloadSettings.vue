<template>
  <div class="download-settings">
    <ftb-slider
      label="Download Threads"
      v-model="localSettings.threadLimit"
      :currentValue="localSettings.threadLimit"
      minValue="1"
      :maxValue="settingsState.hardware.totalCores * 2"
      @change="saveMutated"
      unit="threads"
      @blur="saveMutated"
      class="mb-8"
      small="Control how many threads our download system can use on your system, the higher the better but also the more resource intensive on your machine."
    />

    <ftb-slider
      label="Download Speed"
      v-model="localSettings.speedLimit"
      :currentValue="(localSettings.speedLimit / 1000).toFixed(0)"
      minValue="0"
      :maxValue="250000"
      @change="saveMutated"
      visual-max="250"
      step="256"
      unit="mbps"
      maxValueLabel="250"
      @blur="saveMutated"
      class="mb-8"
      small="Limit how fast each file can be downloaded. For poor internet connection, we recommend using a lower value to not effect others on your network."
    />

    <ftb-slider
      label="Cache Life"
      v-model="localSettings.cacheLife"
      :currentValue="localSettings.cacheLife"
      minValue="900"
      maxValue="15768000"
      @change="saveMutated"
      unit="s"
      maxValueLabel="15768000"
      @blur="saveMutated"
      class="mb-8"
      small="We store some information on your machine to speed up the app, you can control how long that data lives on your machine before being updated again."
    />
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import { Settings, SettingsState } from '@/modules/settings/types';
import { Action, State } from 'vuex-class';
import FTBSlider from '@/components/atoms/input/FTBSlider.vue';

@Component({
  components: {
    'ftb-slider': FTBSlider,
  },
})
export default class DownloadSettings extends Vue {
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @Action('loadSettings', { namespace: 'settings' }) public loadSettings: any;
  @State('settings') public settingsState!: SettingsState;

  localSettings: Settings = {} as Settings;

  async created() {
    await this.loadSettings();

    // Make a copy of the settings so we don't mutate the vuex state
    this.localSettings = { ...this.settingsState.settings };
  }

  saveMutated() {
    this.saveSettings(this.localSettings);
  }
}
</script>

<style scoped lang="scss"></style>
