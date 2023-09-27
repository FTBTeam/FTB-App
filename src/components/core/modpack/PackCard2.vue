<template>
  <div class="pack-card-v2" :class="{'installing': isInstalling}" @click="openInstancePage">
    <div class="artwork-container">
      <img :src="packLogo" alt="Modpack Artwork">
      <div class="notifiers">
        <div class="is-cloud" v-if="instance.cloudSaves">
          <font-awesome-icon icon="cloud" />
        </div>
      </div>
    </div>
    <div class="card-body">
      <div class="info">
        <div class="name">{{ instance.name }}</div>
        <div class="version text-sm opacity-75">
          <template v-if="!isInstalling">{{ instance.version }} ({{ modLoader | title }})</template>
          <template v-else><font-awesome-icon icon="circle-notch" spin class="mr-2" /> Installing</template>
        </div>
      </div>
      <div class="action-buttons" v-if="!isInstalling">
        <div class="button" :class="{disabled: isUpdating}" v-if="needsSyncing" @click.stop="updateInstance">
          <font-awesome-icon icon="cloud" />
        </div>
        <div class="button" :class="{disabled: isUpdating}" v-if="updateAvailable && !needsSyncing" @click.stop="updateInstance">
          <font-awesome-icon icon="download" />
        </div>
        <div class="play-button button" :class="{disabled: isUpdating || needsSyncing}">
          <font-awesome-icon icon="play" />
        </div>
      </div>
    </div>
    
    <modpack-install-modal :pack-id="instance.id" :instance="instance" />
  </div>
</template>

<script lang="ts">
import {Prop, Component} from 'vue-property-decorator';
import PackCardCommon from '@/components/core/modpack/PackCardCommon.vue';
import {isValidVersion, resolveArtwork, resolveModloader, typeIdToProvider} from '@/utils/helpers/packHelpers';
import {RouterNames} from '@/router';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {SugaredInstanceJson} from '@/core/@types/javaApi';
import Popover from '@/components/atoms/Popover.vue';
import ModpackInstallModal from '@/components/core/modpack/ModpackInstallModal.vue';

@Component({
  components: {
    ModpackInstallModal,
    Popover
  }
})
export default class PackCard2 extends PackCardCommon {
  @Prop() instance!: SugaredInstanceJson;
  
  async mounted() {
    // Always fetch the modpack from the API so we can see if updates are available
    await this.fetchModpack(this.instance.id, typeIdToProvider(this.instance.packType));
  }

  openInstancePage() {
    if (this.isInstalling) {
      return;
    }
    
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
    
    return latestVersion.id > this.instance.versionId;
  }

  get isInstalling() {
    if (!this.currentInstall) {
      return false;
    }

    return this.currentInstall?.forInstanceUuid === this.instance.uuid
  }
  
  get isUpdating() {
    return this.currentInstall?.request.updatingInstanceUuid === this.instance.uuid;
  }
  
  get needsSyncing() {
    return this.instance.pendingCloudInstance;
  }
}
</script>

<style lang="scss" scoped>
.pack-card-v2 {
  cursor: pointer;
  
  &:not(.installing):hover {
    .action-buttons {
      opacity: 1;
      visibility: visible;
    }
    
    .info {
      opacity: 0;
      visibility: hidden;
    }
  }
  
  .artwork-container {
    position: relative;
    margin-bottom: .75rem;
    width: 100%;
    
    > img {
      border-radius: 8px;
      border: 1px solid rgba(black, 0.5);
      box-shadow: 0 2px 10px rgba(black, 0.2);
    }
    
    .notifiers {
      position: absolute;
      top: .85rem;
      left: .85rem;
    }
  }
  
  .info, .action-buttons {
    transition: opacity 0.25s ease-in-out, visibility 0.25s ease-in-out;
  }
  
  .action-buttons {
    opacity: 0;
    visibility: hidden;
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    gap: .75rem;
    
    .button {
      text-align: center;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: .6rem .8rem;
      border-radius: 5px;
      box-shadow: 0 2px 10px rgba(black, 0.2);
      
      &.disabled {
        opacity: .5;
        pointer-events: none;
      }

      background-color: var(--color-info-button);

      &:hover {
        background-color: var(--color-light-info-button);
      }
    }
    
    .play-button {
      flex: 1;
      background-color: var(--color-primary-button);
      
      &:hover {
        background-color: var(--color-light-primary-button);
      }
    }
  }
  
  .card-body {
    position: relative;
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