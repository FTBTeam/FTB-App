<template>
  <div>
    <div class="pack-card-v2" :class="{'installing': isInstalling}" @click="openInstancePage">
      <div class="artwork-container">
        <img :src="packLogo" alt="Modpack Artwork">
        <div class="notifiers">
          <div class="is-cloud" v-if="instance.cloudSaves">
            <font-awesome-icon icon="cloud" />
          </div>
        </div>
        
        <transition name="transition-fade" duration="250">
          <div class="install-progress" v-if="isInstalling && currentInstall">
            <div class="percent">{{currentInstall.progress}}<span>%</span></div>
            <b>{{currentInstall.stage ?? "??"}}</b>
            <transition name="transition-fade" duration="250">
              <div class="files text-sm" v-if="currentInstall.speed">
                <font-awesome-icon icon="bolt" class="mr-2" />({{(currentInstall.speed / 12500000).toFixed(2)}}) Mbps
              </div>
            </transition>
            <progress-bar class="progress" :progress="parseFloat(currentInstall?.progress ?? '0') / 100" />
          </div>
        </transition>
      </div>
      <div class="card-body">
        <div class="info">
          <div class="name">{{ instance.name }}</div>
          <div class="version text-sm opacity-75">
            <template v-if="!isInstalling">{{ instance.version }} ({{ modLoader | title }})</template>
            <template v-else><font-awesome-icon icon="circle-notch" spin class="mr-2" /> Installing</template>
          </div>
        </div>
        <div class="action-buttons" v-if="!isInstalling" @click.stop>
          <div class="button flex-1" aria-label="Download from cloud saves" data-balloon-pos="down" :class="{disabled: isUpdating}" v-if="needsSyncing" @click.stop="syncInstance">
            <font-awesome-icon icon="cloud" />
          </div>
          <div class="button" aria-label="Update available!" data-balloon-pos="down-left" :class="{disabled: isUpdating}" v-if="latestVersion && !needsSyncing" @click.stop="updateOpen = true">
            <font-awesome-icon icon="download" />
          </div>
          <div class="play-button button" aria-label="Play" data-balloon-pos="down" v-if="!needsSyncing" :class="{disabled: isUpdating}" @click.stop="play">
            <font-awesome-icon icon="play" />
          </div>
        </div>
      </div>
    </div>
    
    <update-confirm-modal v-if="latestVersion" :local-instance="instance" :latest-version="latestVersion" :open="updateOpen" @close="updateOpen = false" />
  </div>
</template>

<script lang="ts">
import {Prop, Component, Watch} from 'vue-property-decorator';
import PackCardCommon from '@/components/core/modpack/PackCardCommon.vue';
import {
  packUpdateAvailable,
  resolveArtwork,
  resolveModloader,
  typeIdToProvider
} from '@/utils/helpers/packHelpers';
import {RouterNames} from '@/router';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {SugaredInstanceJson} from '@/core/@types/javaApi';
import Popover from '@/components/atoms/Popover.vue';
import ProgressBar from '@/components/atoms/ProgressBar.vue';
import {Versions} from '@/modules/modpacks/types';
import UpdateConfirmModal from '@/components/core/modpack/UpdateConfirmModal.vue';

@Component({
  components: {
    UpdateConfirmModal,
    ProgressBar,
    Popover
  }
})
export default class PackCard2 extends PackCardCommon {
  @Prop() instance!: SugaredInstanceJson;
  
  latestVersion: Versions | null = null;
  updateOpen = false;
  
  async mounted() {
    // Always fetch the modpack from the API so we can see if updates are available
    await this.fetchModpack(this.instance.id, typeIdToProvider(this.instance.packType));
    if (this.apiModpack) {
      this.latestVersion = packUpdateAvailable(this.instance, this.apiModpack) ?? null
    }
  }
  
  @Watch('instance')
  onInstanceChange() {
    if (!this.instance || !this.apiModpack) {
      return;
    }
    
    this.latestVersion = packUpdateAvailable(this.instance, this.apiModpack) ?? null
  }
  
  play() {
    this.$router.push({
      name: RouterNames.ROOT_LAUNCH_PACK,
      query: { uuid: this.instance.uuid },
    })
  }

  openInstancePage() {
    if (this.isInstalling || this.needsSyncing) {
      return;
    }

    this.$router.push({
      name: RouterNames.ROOT_LOCAL_PACK,
      params: {
        uuid: this.instance.uuid
      }
    })
  }
  
  syncInstance() {
    instanceInstallController.requestSync(this.instance);
  }
  
  get modLoader() {
    return resolveModloader(this.instance);
  }
  
  get packLogo() {
    return resolveArtwork(this.instance, "square", this.apiModpack)
  }

  get isInstalling() {
    if (!this.currentInstall) {
      return false;
    }

    return this.currentInstall?.forInstanceUuid === this.instance.uuid
  }
  
  get isUpdating() {
    return this.currentInstall?.request?.updatingInstanceUuid === this.instance.uuid;
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
  
  .install-progress {
    position: absolute;
    inset: 0;
    padding: .5rem;
    background-color: rgba(black, .5);
    backdrop-filter: blur(3px);
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    
    .percent {
      font-size: 1.8rem;
      font-weight: bold;
      
      span {
        font-size: 1.25rem;
        font-weight: normal;
        margin-left: .5rem;
      }
    }
    
    b {
      margin-bottom: .5rem;
    }
    
    .progress {
      position: absolute;
      bottom: 0;
      height: 12px !important;
      border-radius: 0 0 8px 8px;
    }
  }
}
</style>