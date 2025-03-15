<template>
  <div>
    <div class="pack-card-v2" :class="{'installing': isInstalling}" @click="openInstancePage" @click.right="openInstanceMenu">
      <div class="artwork-container">
        <img :src="packLogo" alt="Modpack Artwork">
        <div class="notifiers">
          <div class="notifier modloader" aria-label="Minecraft Forge" data-balloon-pos="down-right" v-if="modLoader === 'forge'">
            <img width="30" src="../../../assets/images/forge.svg" alt="" />
          </div>
          <div class="notifier modloader" aria-label="Fabric" data-balloon-pos="down-right" v-if="modLoader === 'fabric'">
            <img width="30" src="../../../assets/images/fabric.webp" alt="" />
          </div>
          <div class="notifier modloader" aria-label="NeoForge" data-balloon-pos="down-right" v-if="modLoader === 'neoforge'">
            <img width="30" src="../../../assets/images/neoforge.png" alt="" />
          </div>
          <div class="notifier modloader" aria-label="QuiltMc" data-balloon-pos="down-right" v-if="modLoader === 'quilt'">
            <img width="30" src="../../../assets/images/quiltmc.svg" alt="" />
          </div>
        </div>
        
        <transition name="transition-fade" duration="250">
          <div class="install-progress" v-if="isInstalling && currentInstall">
            <div class="percent">{{currentInstall.progress}}<span>%</span></div>
            <b>{{currentInstall.stage ?? "??"}}</b>
            <small class="text-center opacity-75" v-if="currentInstall.stage === 'Mod loader'">This may take a minute</small>
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
            <template v-if="!isInstalling">{{ versionName }}</template>
            <template v-else><font-awesome-icon icon="circle-notch" spin class="mr-2" /> Installing</template>
          </div>
        </div>
        <div class="action-buttons" v-if="!isInstalling" @click.stop>
          <div class="button" aria-label="Update available!" data-balloon-pos="down-left" :class="{disabled: isUpdating}" v-if="latestVersion" @click.stop="updateOpen = true">
            <font-awesome-icon icon="download" />
          </div>
          <div class="play-button button" aria-label="Play" data-balloon-pos="down" :class="{disabled: isUpdating || isRunning}" @click.stop="play">
            <font-awesome-icon icon="play" />
          </div>
        </div>
      </div>
    </div>
    
    <update-confirm-modal v-if="latestVersion" :local-instance="instance" :latest-version="latestVersion" :open="updateOpen" @close="updateOpen = false" />
  </div>
</template>

<script lang="ts">
import PackCardCommon from '@/components/groups/modpack/PackCardCommon.vue';
import {packUpdateAvailable, resolveArtwork, resolveModloader, typeIdToProvider} from '@/utils/helpers/packHelpers';
import {RouterNames} from '@/router';
import {SugaredInstanceJson} from '@/core/@types/javaApi';
import Popover from '@/components/ui/Popover.vue';
import ProgressBar from '@/components/ui/ProgressBar.vue';
import {Versions} from '@/modules/modpacks/types';
import UpdateConfirmModal from '@/components/modals/UpdateConfirmModal.vue';
import {AppContextController} from '@/core/context/contextController';
import {ContextMenus} from '@/core/context/contextMenus';
import {InstanceActions} from '@/core/actions/instanceActions';
import {createLogger} from '@/core/logger';
import {ns} from '@/core/state/appState';
import {InstanceRunningData} from '@/core/state/misc/runningState';

const logger = createLogger('PackCard2.vue');

@Component({
  components: {
    UpdateConfirmModal,
    ProgressBar,
    Popover
  }
})
export default class PackCard2 extends PackCardCommon {
  @State("instances", ns("v2/running")) public runningInstances!: InstanceRunningData[]
  
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
    InstanceActions.start(this.instance);
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
  
  openInstanceMenu(event: PointerEvent) {
    if (this.isInstalling || this.isUpdating) {
      return;
    }
    
    AppContextController.openMenu(ContextMenus.INSTANCE_MENU, event, () => {
      return {
        instance: this.instance
      }
    });
  }
  
  get modLoader() {
    return resolveModloader(this.instance);
  }
  
  get packLogo() {
    return resolveArtwork(this.instance, "square", this.apiModpack)
  }
  
  get isRunning() {
    return this.runningInstances.some(e => e.uuid === this.instance.uuid);
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
  
  get versionName() {
    return this._versionName().trim();
  }
  
  _versionName() {
    let version = this.instance.version;
    
    if (version.length > 16) {
      // Test to see if we have a v1.0.0 like version in the string
      const semverLike = /v[\d.]+/i;
      if (semverLike.test(version)) {
        // Remove the v1.0.0 like version from the string
        version = semverLike.exec(version)![0] ?? version;
      }
      
      const packName = this.instance.name.split(' ');
      if (packName.some(name => version.toLowerCase().includes(name.toLowerCase()))) {
        const splitPackName = packName[0].split('-')[0];
        version = version.toLowerCase().replace(splitPackName.toLowerCase(), "").trim();
      }

      version = version.replace(".zip", "").trim()
      if (version.startsWith("-")) {
        version = version.substring(1);
      } else if (version.endsWith("-")) {
        version = version.substring(0, version.length - 1);
      }
      
      if (version.length > 16) {
        // Return the last 10 characters
        return "..." + version.substring(version.length - 16);
      }
    }
    
    return version;
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
      -webkit-user-drag: none;
    }
    
    .notifiers {
      --balloon-font-size: 10px;
      position: absolute;
      top: .25rem;
      right: .25rem;
      display: flex;
      align-items: center;
      flex-direction: row-reverse;
      gap: .25rem;
      
      .notifier {
        background-color: rgba(black, .8);
        display: flex;
        padding: .5rem;
        align-items: center;
        justify-content: center;
        border-radius: 5px;
        width: 30px;
        height: 30px;
        
        svg {
          font-size: 10px;
        }
      }
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