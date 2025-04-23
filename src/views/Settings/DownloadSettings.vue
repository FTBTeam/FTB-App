<script lang="ts" setup>
import {SettingsData} from '@/core/types/javaApi';
import { useAppSettings } from '@/store/appSettingsStore.ts';
import { onMounted, ref } from 'vue';
import { FTBSlider, Loader } from '@/components/ui';

const appSettingsStore = useAppSettings();
const localSettings = ref<SettingsData>({} as SettingsData);

onMounted(() => {
  if (!appSettingsStore.rootSettings) {
    appSettingsStore.loadIfNeeded();
  }

  if (!appSettingsStore.rootSettings) {
    return;
  }

  localSettings.value = { ...appSettingsStore.rootSettings };
})

function saveMutated() {
  appSettingsStore.saveSettings(localSettings.value);
}
</script>

<template>
  <div class="download-settings" v-if="localSettings.spec">
    <div class="mb-6">
      <h1 class="font-bold text-xl mb-2">Downloads</h1>
      <p class="text-muted">Adjust the download settings to your liking, these settings will effect how the app downloads files and how it caches data.</p>
    </div>
    
    <FTBSlider
      label="Download Threads"
      v-model="localSettings.download.threadLimit"
      :currentValue="localSettings.download.threadLimit"
      :minValue="1"
      :maxValue="(appSettingsStore.systemHardware?.totalCores ?? 1) * 2"
      @change="saveMutated"
      unit="threads"
      @blur="saveMutated"
      class="mb-8"
      small="Control how many threads our download system can use on your system, the higher the better but also the more resource intensive on your machine."
    />

    <FTBSlider
      label="Download Speed"
      v-model="localSettings.download.speedLimit"
      :currentValue="localSettings.download.speedLimit / 1000"
      :minValue="0"
      :maxValue="250000"
      @change="saveMutated"
      :visual-max="250"
      :step="256"
      unit="mbps"
      maxValueLabel="250"
      @blur="saveMutated"
      class="mb-8"
      small="Limit how fast each file can be downloaded. For poor internet connection, we recommend using a lower value to not effect others on your network."
    />

    <FTBSlider
      label="Cache Life"
      v-model="localSettings.general.cacheLife"
      :currentValue="localSettings.general.cacheLife"
      :minValue="900"
      :maxValue="15768000"
      @change="saveMutated"
      unit="s"
      maxValueLabel="15768000"
      @blur="saveMutated"
      class="mb-8"
      small="We store some information on your machine to speed up the app, you can control how long that data lives on your machine before being updated again."
    />
  </div>
  <Loader v-else />
</template>
