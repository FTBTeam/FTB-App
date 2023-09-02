<template>
  <div class="pack-card-v2" @click="openInstancePage">
    <div class="artwork-container">
      <img :src="packLogo" alt="Modpack Artwork">
    </div>
    <div class="card-body">
      <div class="name">{{ instance.name }}</div>
      <div class="version">{{ modLoader }} - {{ instance.version }}</div>
      
      {{ updateAvailable ? "Update Available" : "" }}
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

@Component
export default class PackCard2 extends PackCardCommon {
  @Prop() instance!: InstanceJson;
  
  async mounted() {
    // Always fetch the modpack from the API so we can see if updates are available
    await this.fetchModpack(this.instance.id);
  }
  
  get updateAvailable() {
    if (!this.apiModpack) {
      return false;
    }
    
    const versions = this.apiModpack.versions
      .filter(e => isValidVersion(e.type))
      .sort((a, b) => b.id - a.id);
    
    const instanceVersion = this.instance.versionId;
    const latestVersion = versions[0].id;
    
    return latestVersion > instanceVersion;
  }

  openInstancePage() {
    this.$router.push({
      name: RouterNames.ROOT_LOCAL_PACK,
      params: {
        uuid: this.instance.uuid
      }
    })
  }
  
  get modLoader() {
    return resolveModloader(this.instance);
  }
  
  get packLogo() {
    return resolveArtwork(this.instance, "square", this.apiModpack)
  }
}
</script>

<style lang="scss" scoped>
.pack-card-v2 {
  background-color: #ffffff12;
  border-radius: 5px;
  padding: .75rem;
  
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