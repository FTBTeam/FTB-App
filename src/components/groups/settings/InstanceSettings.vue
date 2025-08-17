<script setup lang="ts">
import {computed, ref} from "vue";
import JavaSettings from "@/components/groups/settings/JavaSettings.vue";
import LaunchSettings from "@/components/groups/settings/LaunchSettings.vue";
import GameOptions from "@/components/groups/settings/GameOptions.vue";

type Tab = "general" | "java" | "launch" | "game-options"

export type InstanceSettingsValue = {
  java: {
    javaPath: string;
    ram: number;
  },
  launch: {
    javaArgs: string;
    programArgs: string;
    shellArgs: string;
    disableInjectedMods: boolean;
    disableJavaAgents: boolean;
  },
  game: {},
}

const {
  hasGeneralTab = true,
} = defineProps<{
  hasGeneralTab?: boolean;
}>();

const model = defineModel<InstanceSettingsValue>();

const modpackSettingsTabs: Record<Tab, string> = {
  general: "General",
  java: "Java",
  "game-options": "Game Options",
  launch: "Launch",
};

// Just remove general tab from the default settings
const modpackDefaultSettingsTabs = Object.keys(modpackSettingsTabs)
  .filter(e => e !== "general")
  .reduce((acc, key) => {
    acc[key as Tab] = modpackSettingsTabs[key as Tab];
    return acc;
  }, {} as Record<Tab, string>);

const tab = ref<Tab>("general");
const tabsToShow = computed(() => hasGeneralTab ? modpackSettingsTabs : modpackDefaultSettingsTabs);
</script>

<template>
  <div>
    <nav class="p-1 flex gap-2 rounded-xl mb-6 border border-white/10">
      <div 
        v-for="(label, key) in tabsToShow" 
        :key="key" 
        class="rounded-lg px-2 py-1.5 flex-1 text-center cursor-pointer hover:bg-white/10 transition-colors duration-200"
        :class="{
          '!bg-blue-400/80 text-white': tab === key,
        }"
        @click="tab = key as Tab"
      >
        {{ label }}
      </div>
    </nav>
    
    <div v-if="model">
      <slot name="general" v-if="tab === 'general'" />
      <JavaSettings v-if="tab === 'java'" v-model="model.java" />
      <LaunchSettings v-if="tab === 'launch'" v-model="model.launch" :settings="model" />
      <GameOptions v-if="tab === 'game-options'" v-model="model.game" />
    </div>
  </div>
</template>