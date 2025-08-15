<script lang="ts" setup>
import {InstanceJson, SugaredInstanceJson} from '@/core/types/javaApi';
import {typeIdToProvider} from '@/utils/helpers/packHelpers';
import { ModPack } from '@/core/types/appTypes.ts';
import {computed} from "vue";
import {packBlacklist} from "@/store/modpackStore.ts";

const {
  packInstance,
  packName,
  isInstalled = false,
  instance,
} = defineProps<{
  packInstance: ModPack | null;
  packName: string;
  isInstalled?: boolean;
  instance?: SugaredInstanceJson | InstanceJson;
}>()

const provider = typeIdToProvider(packInstance?.id ?? 0);

const modloaderVersion = computed(() => {
  if (!instance) return;
  
  const loader = instance.modLoader.toLowerCase();
  if (loader.startsWith("fabric")) {
    return `Fabric ${loader.slice(loader.lastIndexOf("-") + 1) ?? "Unknown"}`;
  } else if (loader.includes("forge") && !loader.includes("neoforge")) {
    return `Forge ${loader.slice(loader.lastIndexOf("-") + 1) ?? "Unknown"}`;
  } else if (loader.includes("neoforge")) {
    return `NeoForge ${loader.split("-")[1] ?? "Unknown"}`;
  }
})

const isLoader = computed(() => {
  if (!instance) return false;
  return packBlacklist.includes(instance.id)
})
</script>

<template>
  <div class="pack-info">
    <div class="info">
      <div class="name select-text">{{ packName }}</div>
      <div class="desc select-text" v-if="(!instance || !instance.isImport) && !isInstalled">
        {{ packInstance?.name }}
        <template v-if="packInstance && packInstance.authors && packInstance.authors.length">
          by 
          <span v-if='provider === "modpacksch" && packInstance.tags.findIndex(e => e.name.toLowerCase() === "ftb") !== -1'>FTB Team</span>
          <span v-else v-for="(author, i) in packInstance.authors" :key="'athrs' + i">{{ author.name }}</span>
        </template>
        -
        {{ packInstance?.synopsis }}
      </div>
      <div class="inline-flex items-center gap-2">
        <div class="select-text text-sm px-4 py-1 rounded bg-black/30 inline-block" v-if="instance && !instance.isImport && !isLoader">
          Version {{ instance.version }}
        </div>
        <div class="select-text text-sm px-4 py-1 rounded bg-black/30 inline-block" v-if="modloaderVersion">{{modloaderVersion}}</div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.pack-info {
  padding: 2rem 1rem;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  text-shadow: 0 0 5px rgba(black, 0.6);
  background-color: rgba(black, 0.2);

  .info {
    padding: 1rem 4rem;
    @media (min-width: 1300px) {
      max-width: 90%;
    }
  }

  .name {
    font-size: 2.2rem;
    font-weight: bold;
    line-height: 1.2em;
    margin-bottom: 0.5rem;

    @media (min-width: 1300px) {
      font-size: 2.8rem;
    }
  }
}
</style>
