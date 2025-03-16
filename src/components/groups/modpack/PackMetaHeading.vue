<script lang="ts" setup>
import { computed } from 'vue';
import {getColorForReleaseType} from '@/utils/colors';
import {ModPack, PackProviders} from '@/modules/modpacks/types';
import {InstanceJson, SugaredInstanceJson} from '@/core/types/javaApi';
import store from '@/modules/store';
import {resolveModloader, sourceProviderToProvider, typeIdToProvider} from '@/utils/helpers/packHelpers';
import {packBlacklist} from '@/core/state/modpacks/modpacksState';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';

type PackInfo = {
  id: number;
  modloader: string;
  provider: PackProviders;
  isImport: boolean;
  source: 'remote' | 'local';
}

const {
  hidePackDetails,
  versionType,
  apiPack,
  instance
} = defineProps<{
  hidePackDetails: boolean;
  versionType: string;
  apiPack?: ModPack;
  instance?: SugaredInstanceJson | InstanceJson;
}>()

const packInfo = computed(() => {
  if (instance) return getInstanceInfo(instance);
  if (apiPack) return getApiPackInfo(apiPack);

  return null;
})

function getInstanceInfo(instance: SugaredInstanceJson | InstanceJson): PackInfo {
  return {
    id: instance.id,
    modloader: this.modloader,
    provider: typeIdToProvider(instance.packType ?? 0),
    isImport: instance.isImport ?? false,
    source: 'local'
  }
}

function getApiPackInfo(apiPack: ModPack): PackInfo {
  return {
    id: apiPack.id,
    modloader: this.modloader,
    provider: sourceProviderToProvider(apiPack.provider),
    isImport: false,
    source: 'remote'
  }
}

const modloader = computed(() => {
  return resolveModloader((instance || apiPack) ?? null).toLowerCase()
})

const isVanilla = computed(() => apiPack?.id === 81 && !modloader)

const isLoader = computed(() => {
  if (!instance) {
    if (apiPack) {
      return packBlacklist.includes(apiPack.id);
    }
    return false;
  }
})
</script>

<template>
  <div class="meta-heading" :class="{ bolder: hidePackDetails }">
    <div class="back" @click="() => $emit('back')">
      <FontAwesomeIcon icon="chevron-left" class="mr-2" />
      Back to {{ hidePackDetails ? 'instance' : 'library' }}
    </div>

    <div
      class="beta-tag"
      :style="{ backgroundColor: getColorForReleaseType(versionType) }"
      v-if="versionType !== 'release'"
    >
      Running {{ versionType }} version
    </div>
    
    <div class="meta" v-if="packInfo && !isVanilla">
      <div
        class="origin icon ftb"
        v-if="!packInfo.isImport && packInfo.provider === 'modpacksch' && !isLoader"
        data-balloon-pos="left"
        aria-label="FTB Modpack"
      >
        <img src="../../../assets/images/ftb-logo.svg" alt="" />
      </div>
      <div class="origin icon" v-else-if="packInfo.provider !== 'modpacksch'" data-balloon-pos="left" aria-label="Curseforge Modpack">
        <img src="../../../assets/curse-logo.svg" alt="" />
      </div>
      <div class="modloader icon" v-if="packInfo.modloader === 'neoforge'" data-balloon-pos="left" aria-label="NeoForge Modloader">
        <img src="../../../assets/images/neoforge.png" alt="" />
      </div>
      <div class="modloader icon" v-if="packInfo.modloader === 'forge'" data-balloon-pos="left" aria-label="Forge Modloader">
        <img src="../../../assets/images/forge.svg" alt="" />
      </div>
      <div class="modloader icon" v-if="packInfo.modloader === 'fabric'" data-balloon-pos="left" aria-label="Fabric Modloader">
        <img src="../../../assets/images/fabric.webp" alt="" />
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.meta-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(black, 0.6);
  padding: 0.8rem 1rem;
  position: relative;
  z-index: 1;
  transition: background-color 0.25s ease-in-out;

  &.bolder {
    background-color: var(--color-navbar);

    .meta {
      opacity: 0;
    }
  }

  .back {
    opacity: 0.7;
    cursor: pointer;
    transition: opacity 0.25s ease-in-out;

    &:hover {
      opacity: 1;
    }
  }

  .back,
  .meta {
    flex: 1;
  }

  .meta {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    transition: opacity 0.25s ease-in-out;

    .icon {
      cursor: default;
      margin-left: 1.5rem;
      
      img {
        width: 30px;
      }

      & .ftb {
        filter: saturate(0%);
      }
    }
  }

  .beta-tag {
    padding: 0.2rem 0.5rem;
    border-radius: 4px;
    background-color: rgba(234, 32, 32, 0.89);
    font-size: 0.75rem;
    font-weight: bold;
  }
}
</style>
