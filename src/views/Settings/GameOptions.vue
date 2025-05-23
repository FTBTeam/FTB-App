<script lang="ts" setup>
import { useAppSettings } from '@/store/appSettingsStore.ts';
import {options} from "@/views/Settings/gameOptions.ts";
import {computed, ref} from "vue";

const appSettingsStore = useAppSettings();

const activeItem = ref("sound_volumes")

const activeCategory = computed(() => {
  const category = options.find((category) => category.key === activeItem.value);
  if (!category) {
    return null;
  }
  return category;
})
</script>

<template>
  <div class="proxy-settings">
    <h2 class="text-xl mb-2 font-bold flex items-center">
      Game options
    </h2>
    <p>Here you can setup game option presets, this allows you to automatically apply specific game settings to new instances. This means no more being defended by game sounds, rebinding x, y and z, etc.</p>
    
    <div class="flex flex-wrap gap-4 mt-6">
      <div class="font-bold cursor-pointer transition-colors duration-300" :class="{'text-green-400': activeItem === category.key}" v-for="(category, index) in options" :key="index" @click="activeItem = category.key">
        {{ category.name }}
      </div>
    </div>
    
    <div v-if="activeCategory" class="mt-8">
      <div v-for="(item, index) in activeCategory.options" :key="index">
        {{ item.name }}
      </div>
    </div>
  </div>
</template>

<style scoped lang='scss'>

</style>