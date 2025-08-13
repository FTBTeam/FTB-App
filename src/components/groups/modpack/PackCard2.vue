<script lang="ts" setup>
import {packUpdateAvailable, resolveArtwork, resolveModloader, typeIdToProvider} from '@/utils/helpers/packHelpers';
import {RouterNames} from '@/router';
import {SugaredInstanceJson} from '@/core/types/javaApi';
import ProgressBar from '@/components/ui/ProgressBar.vue';
import UpdateConfirmModal from '@/components/modals/UpdateConfirmModal.vue';
import {AppContextController} from '@/core/context/contextController';
import {ContextMenus} from '@/core/context/contextMenus';
import {InstanceActions} from '@/core/actions/instanceActions';
import { watch, ref, onMounted, computed } from 'vue';
import { useFetchingPack } from '@/components/groups/modpack/useFetchingPack.ts';
import { useRouter } from 'vue-router';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { useRunningInstancesStore } from '@/store/runningInstancesStore.ts';
import { useInstallStore } from '@/store/installStore.ts';
import { Versions } from '@/core/types/appTypes.ts';
import {faBolt, faCheck, faCircleNotch, faDownload, faPlay} from '@fortawesome/free-solid-svg-icons';

const router = useRouter()
const runningInstancesStore = useRunningInstancesStore();
const installStore = useInstallStore()

const {
  instance,
  checked,
  onCheckedChange
} = defineProps<{
  instance: SugaredInstanceJson;
  checked?: boolean;
  onCheckedChange?: (checked: boolean) => void;
}>()

const latestVersion = ref<Versions | null>(null);
const updateOpen = ref(false);

const {apiModpack, fetchModpack} = useFetchingPack()
const isInstalling = computed(() => {
  if (!installStore.currentInstall) {
    return false;
  }

  return installStore.currentInstall?.forInstanceUuid === instance.uuid
})

onMounted(() => {
  fetchModpack(instance.id, typeIdToProvider(instance.packType));
})

watch(apiModpack, (newValue) => {
  if (newValue) {
    latestVersion.value = packUpdateAvailable(instance, newValue) ?? null;
  }
})

watch(() => instance, () => {
  if (!instance || !apiModpack.value) {
    return;
  }

  latestVersion.value = packUpdateAvailable(instance, apiModpack.value) ?? null
});

function play() {
  InstanceActions.start(instance);
}

function openInstancePage() {
  if (isInstalling.value) {
    return;
  }

  router.push({
    name: RouterNames.ROOT_LOCAL_PACK,
    params: {
      uuid: instance.uuid
    }
  })
}

function openInstanceMenu(event: MouseEvent) {
  if (isInstalling.value || isUpdating.value) {
    return;
  }

  AppContextController.openMenu(ContextMenus.INSTANCE_MENU, event, () => {
    return {
      instance: instance
    }
  });
}

const modloader = computed(() => resolveModloader(instance));
const packLogo = computed(() => resolveArtwork(instance, "square", apiModpack.value))
const isRunning = computed(() => runningInstancesStore.instances.some(e => e.uuid === instance.uuid));
const isUpdating = computed(() => installStore.currentInstall?.request?.updatingInstanceUuid === instance.uuid);
const versionName = computed(() => _versionName().trim());

function _versionName() {
  let version = instance.version;

  if (version.length > 16) {
    // Test to see if we have a v1.0.0 like version in the string
    const semverLike = /v[\d.]+/i;
    if (semverLike.test(version)) {
      // Remove the v1.0.0 like version from the string
      version = semverLike.exec(version)![0] ?? version;
    }

    const packName = instance.name.split(' ');
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
</script>

<template>
  <div>
    <div class="pack-card-v2 relative" :class="{'installing': isInstalling}" @click="() => openInstancePage()" @click.right="(e) => openInstanceMenu(e)">
      <div class="check-box absolute z-[1] top-2 right-2 rounded-lg border-2 border-black/40 w-7 h-7 backdrop-blur-sm flex items-center justify-center"
           :class="{
            'bg-green-500 checked': checked,
            'bg-white/20': !checked,
           }"
           v-if="typeof checked !== 'undefined' && onCheckedChange && !isInstalling"
           @click.stop="onCheckedChange(!checked)">
        
        <transition name="transition-fade" :duration="250">
          <FontAwesomeIcon v-if="checked" :icon="faCheck" class="text-white" />
        </transition>
      </div>
      <div class="artwork-container aspect-square">
        <div class="flex h-full items-center justify-center rounded-lg bg-black/20">
          <img class="object-contain rounded-lg" :src="packLogo" alt="Modpack Artwork">
        </div>
        <div class="notifiers">
          <div class="notifier modloader" aria-label="Minecraft Forge" data-balloon-pos="down-left" v-if="modloader.toLowerCase() === 'forge'">
            <img :width="30" src="../../../assets/images/forge.svg" alt="" />
          </div>
          <div class="notifier modloader" aria-label="Fabric" data-balloon-pos="down-left" v-if="modloader.toLowerCase() === 'fabric'">
            <img :width="30" src="../../../assets/images/fabric.webp" alt="" />
          </div>
          <div class="notifier modloader" aria-label="NeoForge" data-balloon-pos="down-left" v-if="modloader.toLowerCase() === 'neoforge'">
            <img :width="30" src="../../../assets/images/neoforge.png" alt="" />
          </div>
          <div class="notifier modloader" aria-label="QuiltMc" data-balloon-pos="down-left" v-if="modloader.toLowerCase() === 'quilt'">
            <img :width="30" src="../../../assets/images/quiltmc.svg" alt="" />
          </div>
        </div>
        
        <transition name="transition-fade" :duration="250">
          <div class="install-progress" v-if="isInstalling && installStore.currentInstall">
            <div class="percent">{{installStore.currentInstall.progress}}<span>%</span></div>
            <b>{{installStore.currentInstall.stage ?? "??"}}</b>
            <small class="text-center opacity-75" v-if="installStore.currentInstall.stage === 'Mod loader'">This may take a minute</small>
            <transition name="transition-fade" :duration="250">
              <div class="files text-sm" v-if="installStore.currentInstall.speed">
                <FontAwesomeIcon :icon="faBolt" class="mr-2" />({{(installStore.currentInstall.speed / 12500000).toFixed(2)}}) Mbps
              </div>
            </transition>
            <progress-bar class="progress" :progress="parseFloat(installStore.currentInstall?.progress ?? '0') / 100" />
          </div>
        </transition>
      </div>
      <div class="card-body">
        <div class="info">
          <div class="name">{{ instance.name }}</div>
          <div class="version text-sm opacity-75">
            <template v-if="!isInstalling">{{ versionName }}</template>
            <template v-else><FontAwesomeIcon :icon="faCircleNotch" spin class="mr-2" /> Installing</template>
          </div>
        </div>
        <div class="action-buttons" v-if="!isInstalling" @click.stop>
          <div class="button transition-colors bg-orange-500 hover:bg-orange-600 duration-300" aria-label="Update available!" data-balloon-pos="down-left" :class="{disabled: isUpdating}" v-if="latestVersion" @click.stop="updateOpen = true">
            <FontAwesomeIcon :icon="faDownload" />
          </div>
          <div class="play-button transition-colors duration-300 button bg-green-600 hover:bg-green-500" aria-label="Play" data-balloon-pos="down" :class="{disabled: isUpdating || isRunning}" @click.stop="play">
            <FontAwesomeIcon :icon="faPlay" />
          </div>
        </div>
      </div>
    </div>
    
    <update-confirm-modal v-if="latestVersion" :local-instance="instance" :latest-version="latestVersion" :open="updateOpen" @close="updateOpen = false" />
  </div>
</template>

<style lang="scss" scoped>
.pack-card-v2 {
  cursor: pointer;
  
  .check-box {
    visibility: hidden;
    opacity: 0;
    transition: opacity 0.25s ease-in-out, visibility 0.25s ease-in-out;
  }
  
  &:hover {
    .check-box {
      visibility: visible;
      opacity: 1;
    }
  }

  .check-box.checked {
    visibility: visible;
    opacity: 1;
  }
  
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
      top: .75rem;
      left: .75rem;
      display: flex;
      align-items: center;
      flex-direction: row-reverse;
      gap: .25rem;
      
      .notifier {
        width: 25px;
        height: 25px;
        
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
    }
    
    .play-button {
      flex: 1;
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
      bottom: -1px;
      height: 12px !important;
      border-radius: 0 0 8px 8px;
    }
  }
}
</style>