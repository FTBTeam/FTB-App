<script lang="ts" setup>
import {alertController} from '@/core/controllers/alertController';
import {ReleaseChannelOptions} from '@/utils/commonOptions';
import {prettyByteFormat} from '@/utils';
import UiToggle from '@/components/ui/UiToggle.vue';
import RamSlider from '@/components/groups/modpack/components/RamSlider.vue';
import {SettingsData} from '@/core/types/javaApi';
import { Loader, UiButton, Input } from '@/components/ui';
import { useAppSettings } from '@/store/appSettingsStore.ts';
import { computed, onMounted, ref } from 'vue';
import { faCode, faUndo } from '@fortawesome/free-solid-svg-icons';
import TextArea from '@/components/ui/form/TextArea/TextArea.vue';
import ResolutionSelector, {ResolutionValue} from "@/components/groups/modpack/components/ResolutionSelector.vue";
import UiSelect from "@/components/ui/select/UiSelect.vue";
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

    <ui-toggle 
      label="Disable helper mod injection"
      desc="The FTB App will inject helper mods into your instance to help the app and your instance work together. Sometimes this can cause issues with Minecraft, Mods, Etc. You can disable this behaviour here." 
      v-model="localSettings.instanceDefaults.preventMetaModInjection" 
      class="mb-4" @input="() => {
        saveMutated()
      }" />
    
    <ram-slider class="mb-6" v-model="localSettings.instanceDefaults.memory" @update="saveMutated" />

    <div class="flex gap-4 flex-col mb-6">
      <TextArea
        label="Java runtime arguments"
        class="mb-4"
        hint="These arguments are appended to your instances upon start, they are normal java arguments. New lines will be removed."
        v-model="localSettings.instanceDefaults.javaArgs"
        @blur="() => {
            // Remove all new lines and trim the string
            localSettings.instanceDefaults.javaArgs = localSettings.instanceDefaults.javaArgs.trim().replaceAll(/(\r\n|\n|\r)/gm, '')
            saveMutated()
          }"
        fill
        :spellcheck="false"
        :rows="4"
      />

      <div class="flex gap-4">
        <UiButton size="small" :icon="faUndo" @click="() => {
            localSettings.instanceDefaults.javaArgs = '-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M'
            saveMutated()
          }">
          Reset to Vanilla defaults
        </UiButton>
      </div>
    </div>

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
    
    <p class="mb-2">Startup preview</p>
    <small class="mb-4 block">This is for illustrative purposes only, this is not a complete example.</small>
    
    <code class="block bg-black rounded mb-6 px-2 py-2 overflow-x-auto" v-if="localSettings && localSettings.instanceDefaults.memory">
      {{localSettings.instanceDefaults.shellArgs}} java {{localSettings.instanceDefaults.javaArgs}} -Xmx{{prettyByteFormat(Math.floor(parseInt(localSettings.instanceDefaults.memory.toString()) * 1024 * 1000))}} -jar minecraft.jar {{localSettings.instanceDefaults.programArgs}}
    </code>
  </div>
  <Loader v-else />
</template>

<style scoped lang="scss">
.flex-1 {
  flex: 1;
}
</style>
