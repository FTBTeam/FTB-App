<template>
  <div class="meta-heading" :class="{ bolder: hidePackDetails }">
    <div class="back" @click="() => $emit('back')">
      <font-awesome-icon icon="chevron-left" class="mr-2" />
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

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {Prop} from 'vue-property-decorator';
import {getColorForReleaseType} from '@/utils/colors';
import {Getter} from 'vuex-class';
import {ModPack, PackProviders} from '@/modules/modpacks/types';
import {InstanceJson, SugaredInstanceJson} from '@/core/@types/javaApi';
import store from '@/modules/store';
import {resolveModloader, sourceProviderToProvider, typeIdToProvider} from '@/utils/helpers/packHelpers';
import {packBlacklist} from '@/core/state/modpacks/modpacksState';

type PackInfo = {
  id: number;
  modloader: string;
  provider: PackProviders;
  isImport: boolean;
  source: 'remote' | 'local';
}

@Component
export default class PackMetaHeading extends Vue {
  @Getter('getActiveProfile', { namespace: 'core' }) public getActiveMcProfile!: any;

  @Prop() hidePackDetails!: boolean;
  @Prop() versionType!: string;
  @Prop() apiPack?: ModPack;
  @Prop() instance?: SugaredInstanceJson | InstanceJson

  getColorForReleaseType = getColorForReleaseType;
  
  get packInfo() {
    if (this.instance) return this.getInstanceInfo(this.instance);
    if (this.apiPack) return this.getApiPackInfo(this.apiPack);
    
    return null;
  }

  getInstanceInfo(instance: SugaredInstanceJson | InstanceJson): PackInfo {
    return {
      id: instance.id,
      modloader: this.modloader,
      provider: typeIdToProvider(instance.packType ?? 0),
      isImport: instance.isImport ?? false,
      source: 'local'
    }
  }

  getApiPackInfo(apiPack: ModPack): PackInfo {
    return {
      id: apiPack.id,
      modloader: this.modloader,
      provider: sourceProviderToProvider(apiPack.provider),
      isImport: false,
      source: 'remote'
    }
  }
  
  get modloader() {
    return resolveModloader((this.instance || this.apiPack) ?? null).toLowerCase()
  }

  loaderFromApiPack() {
    if (!this.apiPack) {
      return 'vanilla';
    }
    
    const releaseChannel = store.state.settings?.settings.instanceDefaults.updateChannel ?? 'release';
    const sortedVersions = this.apiPack.versions
      .sort((a, b) => b.id - a.id);
    
    const latest = sortedVersions
      .find((v) => v.type.toLowerCase() === releaseChannel);
    
    return (latest ?? sortedVersions[0])?.targets.find(e => e.type === "modloader")?.name ?? 'vanilla';
  }
  
  get isVanilla() {
    return this.apiPack?.id === 81 && !this.modloader;
  }
  
  get isLoader() {
    if (!this.instance) {
      if (this.apiPack) {
        return packBlacklist.includes(this.apiPack.id);
      }
      return false;
    }
    
    return packBlacklist.includes(this.instance.id);
  }
}
</script>

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
