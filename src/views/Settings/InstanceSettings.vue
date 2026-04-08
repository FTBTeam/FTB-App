<script lang="ts" setup>
import {alertController} from '@/core/controllers/alertController';
import UiToggle from '@/components/ui/UiToggle.vue';
import RamSlider from '@/components/groups/modpack/components/RamSlider.vue';
import {SettingsData} from '@/core/types/javaApi';
import { Loader, Input } from '@/components/ui';
import { useAppSettings } from '@/store/appSettingsStore.ts';
import { onMounted, ref } from 'vue';
import { faCode } from '@fortawesome/free-solid-svg-icons';
import ResolutionSelector, {ResolutionValue} from "@/components/groups/modpack/components/ResolutionSelector.vue";
import ReleaseChannelSelector from "@/components/groups/modpack/components/ReleaseChannelSelector.vue";

const appSettingsStore = useAppSettings();

const localSettings = ref({} as SettingsData);
const lastSettings = ref("")

const loadedSettings = ref(false);

onMounted(async () => {
  await appSettingsStore.loadSettings();
  loadedSettings.value = true;

  // Make a copy of the settings so we don't mutate the vuex state
  localSettings.value = { ...appSettingsStore.rootSettings } as SettingsData;
  lastSettings.value = JSON.stringify(localSettings.value)
})

function saveMutated() {
  // Compare the last settings to the current settings, if they are the same, don't save
  console.log("Saving mutated settings", localSettings.value);
  
  if (lastSettings.value === JSON.stringify(localSettings.value)) {
    return;
  }

  alertController.success("Settings saved")
  appSettingsStore.saveSettings(localSettings.value);
  lastSettings.value = JSON.stringify(localSettings.value)
}

function selectResolution(value: ResolutionValue) {
  if (!value) {
    return;
  }
  
  localSettings.value.instanceDefaults.fullscreen = value.fullScreen;
  localSettings.value.instanceDefaults.width = value.width;
  localSettings.value.instanceDefaults.height = value.height;
  saveMutated();
}
</script>

<template>
  <div class="instance-settings" v-if="localSettings.spec">
    <div class="mb-6">
      <h1 class="font-bold text-xl mb-2">Instance defaults</h1>
      <p class="text-muted">These settings are used when creating a new instance or installing a modpack. These settings will <b>Not</b> updated existing modpacks settings.</p>
    </div>
    
    <p class="block text-white-700 text-lg font-bold mb-4">Updates</p>
    <div class="flex items-center mb-4 border-b border-white/10 pb-6">
      <div class="block flex-1 mr-2">
        <b>Release Channel</b>
        <small class="block text-muted mr-6 mt-2">
          The selected release channel will determine when we show that a supported modpack has an update.<span class="mb-2 block" /> Release is the most stable, then Beta should be playable and Alpha could introduce game breaking bugs.</small>
      </div>
      
      <ReleaseChannelSelector v-if="loadedSettings" v-model="localSettings.instanceDefaults.updateChannel" @update:modelValue="saveMutated" />
    </div>
    
    <div class="mb-4 border-b border-white/10 pb-4">
      <ResolutionSelector :model-value="{
        width: localSettings.instanceDefaults.width,
        height: localSettings.instanceDefaults.height,
        fullScreen: localSettings.instanceDefaults.fullscreen
      }" @update="selectResolution" class="mb-6" />
    </div>

    <p class="block text-white-700 text-lg font-bold mb-4">Java</p>
    
    <ram-slider class="mb-6" v-model="localSettings.instanceDefaults.memory" @update="saveMutated" />

    <Input
      class="mb-4"
      :icon="faCode"
      label="Program arguments"
      :value="localSettings.instanceDefaults.programArgs"
      v-model="localSettings.instanceDefaults.programArgs"
      placeholder="--fullscreen"
      @blur="saveMutated"
      fill
      hint="These arguments are appended to the end of the java command, these are typically arguments Minecraft uses."
    />
    
    <Input
      class="mb-4"
      :icon="faCode"
      label="Shell arguments"
      :value="localSettings.instanceDefaults.shellArgs"
      v-model="localSettings.instanceDefaults.shellArgs"
      placeholder="/usr/local/application-wrapper"
      @blur="saveMutated"
      fill
      hint="These arguments will be inserted before java is run, see the example below. It's recommended to not change these unless you know what you are doing."
    />

    <ui-toggle
      label="Disable helper mod injection"
      desc="The FTB App will inject helper mods into your instance to help the app and your instance work together. Sometimes this can cause issues with Minecraft, Mods, Etc. You can disable this behaviour here."
      v-model="localSettings.instanceDefaults.preventMetaModInjection"
      align-right
      class="mb-6" @input="() => {
        saveMutated()
      }" />

    <ui-toggle
      align-right
      label="Disable Java Agent injection"
      desc="The FTB App will inject Java Agents into your instance to patch known vulnerabilities and issues. We do NOT recommend disabling this option!"
      v-model="localSettings.instanceDefaults.preventMetaAgentInjection"
      class="mb-4" @input="() => {
        saveMutated()
      }" />
  </div>
  <Loader v-else />
</template>

<style scoped lang="scss">
.flex-1 {
  flex: 1;
}
</style>
