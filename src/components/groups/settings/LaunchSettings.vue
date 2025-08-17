<script setup lang="ts">
import {InstanceSettingsValue} from "@/components/groups/settings/InstanceSettings.vue";
import {Input, UiButton, UiToggle} from "@/components/ui";
import {faUndo} from "@fortawesome/free-solid-svg-icons";
import {megabyteSize, prettyByteFormat} from "@/utils";
import TextArea from "@/components/ui/form/TextArea/TextArea.vue";
import {computed} from "vue";
import {useAppSettings} from "@/store/appSettingsStore.ts";

const appSettingsStore = useAppSettings();

const preferIPv4Arg = "-Djava.net.preferIPv4Stack=true"

const {
  settings
} = defineProps<{
  settings: InstanceSettingsValue
}>()

const value = defineModel<InstanceSettingsValue["launch"]>()
const prefersIPv4 = computed(() => value.value?.javaArgs.includes(preferIPv4Arg))

function preferIPv4Clicked(event: any) {
  if (!value.value) {
    return
  }
  
  if (!event) {
    value.value.javaArgs = value.value.javaArgs.replace(preferIPv4Arg, "").trim()
  } else {
    value.value.javaArgs = `${value.value.javaArgs} ${preferIPv4Arg}`.trim()
  }
}
</script>

<template>
  <div v-if="value">
    <ui-toggle
      :align-right="true"
      label="Disable helper mods"
      desc="Sometimes the app will inject helper mods into your instance to help the app and your instance work together."
      v-model="value.disableInjectedMods"
      class="mb-6" />

    <ui-toggle
      :align-right="true"
      label="Disable java agents"
      desc="Java agents are used to inject code into the Java process to help the Minecraft instance to fix common issues or security problems."
      v-model="value.disableJavaAgents"
      class="mb-6" />

    <div class="flex gap-4 flex-col mb-6">
        <TextArea
          label="Java runtime arguments"
          hint="These arguments are appended to your instances upon start, they are normal java arguments. New lines will be removed."
          placeholder="-TestArgument=120"
          v-model="value.javaArgs"
          @blur="() => {
            if (!value) return 
            // Remove all new lines and trim the string
            value.javaArgs = value.javaArgs.trim().replaceAll(/(\r\n|\n|\r)/gm, '')
          }"
          :spellcheck="false"
          fill
          :rows="4"
        />

      <div class="flex gap-4">
        <UiButton size="small" :icon="faUndo" @click="() => {
            if (!value) return 
            value.javaArgs = appSettingsStore.rootSettings?.instanceDefaults.javaArgs ?? ''
          }">
          Reset to Instance defaults
        </UiButton>

        <UiButton size="small" :icon="faUndo" @click="() => {
            if (!value) return 
            value.javaArgs = '-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M'
          }">
          Reset to Vanilla defaults
        </UiButton>
      </div>

      <ui-toggle class="mb-4" label="Prefer IPv4 network requests" :value="prefersIPv4" @input="preferIPv4Clicked" />
    </div>

    <Input
      class="mb-6"
      label="Program arguments"
      v-model="value.programArgs"
      placeholder="--fullscreen"
      hint="These arguments are appended to the end of the java command, these are typically arguments Minecraft uses."
      fill
    />

    <Input
      class="mb-6"
      label="Shell arguments"
      v-model="value.shellArgs"
      placeholder="/usr/local/application-wrapper"
      fill
      hint="These arguments will be inserted before java is run, see the example below. It's recommended to not change these unless you know what you are doing."
    />

    <p class="mb-2 font-bold">Startup preview</p>
    <p class="mb-4 block text-sm">This is for illustrative purposes only, this is not a complete example.</p>

    <code class="block bg-black rounded mb-6 px-2 py-2 overflow-x-auto select-text" v-if="settings.java.ram">
      {{value.shellArgs}} java {{value.javaArgs}} -Xmx{{prettyByteFormat(Math.floor(parseInt(settings.java.ram.toString()) * megabyteSize))}} -jar minecraft.jar {{value.programArgs}}
    </code>
  </div>
</template>