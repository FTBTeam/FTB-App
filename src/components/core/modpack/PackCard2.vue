<template>
  <div class="pack-card-v2" @click="openInstancePage">
    <div class="artwork-container">
      <img :src="packLogo" alt="Modpack Artwork">
    </div>
    <div class="card-body">
      <div class="name">{{ instance.name }}</div>
      <div class="version text-sm opacity-75">{{ instance.version }} ({{ modLoader | title }})</div>
      
      {{ updateAvailable ? "Update Available" : "" }}
      <div class="update" v-if="!isUpdating" @click.stop="updateInstance">
        Update instance
      </div>
      
      {{ isUpdating ? "Updating" : "" }}
    </div>
  </div>
</template>

<script lang="ts">
import {Prop, Component} from 'vue-property-decorator';
import PackCardCommon from '@/components/core/modpack/PackCardCommon.vue';
import {InstanceJson} from '@/core/@types/javaDataTypes';
import {isValidVersion, resolveArtwork, resolveModloader} from '@/utils/helpers/packHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {emitter} from '@/utils';
import {RouterNames} from '@/router';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';

@Component
export default class PackCard2 extends PackCardCommon {
  @Prop() instance!: InstanceJson;
  
  async mounted() {
    // Always fetch the modpack from the API so we can see if updates are available
    await this.fetchModpack(this.instance.id);
  }

  openInstancePage() {
    this.$router.push({
      name: RouterNames.ROOT_LOCAL_PACK,
      params: {
        uuid: this.instance.uuid
      }
    })
  }

  getLatestVersion() {
    if (!this.apiModpack) {
      return null;
    }
    
    const versions = this.apiModpack.versions
      .filter(e => isValidVersion(e.type))
      .sort((a, b) => b.id - a.id);
    
    return versions[0];
  }
  
  updateInstance() {
    const latestVersion = this.getLatestVersion();
    if (this.isUpdating || !latestVersion) {
      return;
    }
    
    console.log("update instance", this.instance, latestVersion)
    instanceInstallController.requestUpdate(this.instance, latestVersion);
  }
  
  get modLoader() {
    return resolveModloader(this.instance);
  }
  
  get packLogo() {
    return resolveArtwork(this.instance, "square", this.apiModpack)
  }

  get updateAvailable() {
    const latestVersion = this.getLatestVersion();
    if (!latestVersion) {
      return false;
    }
    
    console.log(this.apiModpack?.name, latestVersion.id, latestVersion.name, this.instance.versionId, this.instance.version)
    return latestVersion.id > this.instance.versionId;
  }
  
  get isInstalling() {
    return false;
  }
  
  get isUpdating() {
    return this.currentInstall?.request.updatingInstanceUuid === this.instance.uuid;
  }
}
</script>

<style lang="scss" scoped>
.pack-card-v2 {
  background-color: #ffffff12;
  border-radius: 5px;
  padding: .75rem;
  cursor: pointer;
  
  &:hover {
    
  }
  
  .artwork-container {
    margin-bottom: .75rem;
    
    > img {
      border-radius: 3px;
    }
  }
  
  .card-body {
    .name {
      font-weight: bold;
      width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}
</style>