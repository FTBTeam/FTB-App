<script lang="ts" setup>
import DuplicateInstanceModal from '@/components/modals/actions/DuplicateInstanceModal.vue';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {button, dialog, dialogsController} from '@/core/controllers/dialogsController';
import {InstanceController} from '@/core/controllers/InstanceController';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {RouterNames} from '@/router';
import {SugaredInstanceJson} from '@/core/types/javaApi';
import {createLogger} from '@/core/logger';
import {AuthProfile} from '@/modules/core/core.types';
import platform from '@/utils/interface/electron-overwolf';
import {InstanceRunningData} from '@/core/state/misc/runningState';
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { Modal } from '@/components/ui';

const {
  instance,
  allowOffline = false,
} = defineProps<{
  instance: SugaredInstanceJson;
  allowOffline: boolean;
}>()

const router = useRouter();

// @Getter('getProfiles', { namespace: 'core' }) getProfiles!: AuthProfile[];
// @State("instances", ns("v2/running")) public runningInstances!: InstanceRunningData[]

const emit = defineEmits<{
  (event: 'playOffline'): void;
  (event: 'openSettings'): void;
}>()

const runningInstances = ref<InstanceRunningData[]>([]);
const getProfiles = ref<AuthProfile[]>([]);

const logger = createLogger("PackActions.vue")
const instanceFolders = ref<string[]>([]);
const duplicateConfirm = ref(false);

onMounted(() => {
  sendMessage('getInstanceFolders', { uuid: instance.uuid })
    .then((e) => (instanceFolders.value = e.folders))
    .catch(e => logger.error("Failed to get instance folders", e));
})

async function openInstanceFolder(folder: string) {
  logger.debug("Opening instance folder", folder)
  try {
    await platform.get.io.openFinder(`${instance.path}/${folder}`)
  } catch (e) {
    logger.error("Failed to open instance folder", e);
  }
}

function folderExists(path: string) {
  if (path === '' || !instanceFolders.length) {
    return false;
  }

  return instanceFolders.findIndex((e) => e === path) !== -1;
}

const isRunning = computed(() => runningInstances.some(e => e.uuid === instance.uuid))

function deleteInstance() {
  logger.debug("Asking user to confirm instance deletion", instance)
  const dialogRef = dialogsController.createDialog(
    dialog("Are you sure?")
      .withContent(`Are you absolutely sure you want to delete \`${instance.name}\`! Doing this **WILL permanently** delete all mods, world saves, configurations, and all the rest... There is no way to recover this pack after deletion...`)
      .withType("warning")
      .withButton(button("Delete")
        .withAction(async () => {
          dialogRef.setWorking(true)
          const controller = InstanceController.from(instance);
          await controller.deleteInstance();
          await gobbleError(() => router.push({
            name: RouterNames.ROOT_LIBRARY
          }));
          dialogRef.close();
        })
        .withIcon("trash")
        .withType("error")
        .build())
      .build()
  )
}
</script>

<template>
  <div class="pack-actions-holder">
    <div class="pack-actions" tabindex="0">
      <div class="icon">
        <FontAwesomeIcon icon="ellipsis-vertical" />
      </div>

      <ul class="actions">
        <li class="title">Tools</li>
        <li v-if="allowOffline && !isRunning" @click="emit('playOffline')">
          <span><FontAwesomeIcon icon="play" /></span> Play Offline
        </li>
        <li @click="emit('openSettings')">
          <span><FontAwesomeIcon icon="cog" /></span>Settings
        </li>
        <li tabindex="1">
          <span><FontAwesomeIcon icon="folder-open" /></span>Open...
          <span class="submenu">
            <FontAwesomeIcon icon="chevron-right" />
          </span>
          <ul>
            <li @click="openInstanceFolder('')">Instance folder</li>
            <li v-if="folderExists('config')" @click="openInstanceFolder('config')">Config folder</li>
            <li v-if="folderExists('mods')" @click="openInstanceFolder('mods')">Mods folder</li>
            <li v-if="folderExists('saves')" @click="openInstanceFolder('saves')">Worlds folder</li>
            <li v-if="folderExists('backups')" @click="openInstanceFolder('backups')">Backups folder</li>
            <li v-if="folderExists('resourcepacks')" @click="openInstanceFolder('resourcepacks')">
              Resource packs folder
            </li>
            <li v-if="folderExists('logs')" @click="openInstanceFolder('logs')">Logs folder</li>
            <li v-if="folderExists('crashlogs')" @click="openInstanceFolder('crashlogs')">Crash logs folder</li>
            <li v-if="folderExists('kubejs')" @click="openInstanceFolder('kubejs')">KubeJS folder</li>
          </ul>
        </li>
        <li @click="duplicateConfirm = true">
          <span><FontAwesomeIcon icon="copy" /></span>Duplicate instance
        </li>
        <li class="title" v-if="!isRunning">Danger</li>
        <li @click="deleteInstance" v-if="!isRunning">
          <span><FontAwesomeIcon icon="trash" /></span>Delete instance
        </li>
      </ul>
    </div>
    
    <Modal
      :open="duplicateConfirm"
      :externalContents="true"
      @closed="duplicateConfirm = false"
      title="Duplicate Instance"
      subTitle="Are you sure?!"
    >
      <duplicate-instance-modal
        @finished="duplicateConfirm = false"
        :uuid="instance.uuid"
        :instanceName="instance.name"
        :category="instance.category"
      />
    </Modal>
  </div>
</template>

<style scoped lang="scss">
.pack-actions-holder {
  display: flex;
}

.pack-actions {
  display: flex;
  position: relative;
  align-items: center;
  margin-left: -5px;
  border-radius: 5px;
  background-color: rgba(black, 0.5);
  cursor: pointer;

  .icon {
    padding: 0 1.2rem 0 1.7rem;
  }

  .actions,
  .actions li ul {
    font-size: 12px;
    position: absolute;
    white-space: nowrap;
    top: -7rem;
    left: 140%;
    background-color: var(--color-navbar);
    z-index: 500;
    padding: .7rem 0.5rem;
    border-radius: 5px;
    box-shadow: 0 4px 15px rgba(black, 0.2);
    border: 1px solid rgba(white, 0.1);
    opacity: 0;
    visibility: hidden;

    transition: visibility 0.25s ease-in-out, left 0.25s ease-in-out, opacity 0.25s ease-in-out;

    &::after {
      content: '';
      position: absolute;
      top: calc(7rem + 15px);
      left: -8px;
      border-top: 8px solid var(--color-navbar);
      border-left: 8px solid var(--color-navbar);
      border-radius: 4px;
      width: 15px;
      height: 15px;
      transform: rotateZ(-45deg);
    }

    li {
      position: relative;
      line-height: 2.4em;
      display: flex;
      padding: 0 0.8rem;
      border-radius: 5px;
      transition: background-color 0.15s ease-in-out;

      &.title {
        background-color: transparent !important;
        cursor: default;
        line-height: 1.2em;
      }

      &:hover {
        background-color: var(--color-info-button);
      }

      span {
        width: 35px;
      }

      .submenu {
        width: unset;
        margin-left: auto;
        font-size: 0.875em;
      }

      &:not(:last-child) {
        margin-bottom: 0.5rem;
      }
    }

    .title {
      font-weight: bold;
      color: rgba(white, 0.5);
    }
  }

  .actions li ul {
    left: 100%;
    top: calc(1rem - 22px);

    &::before {
      content: '';
      position: absolute;
      width: 18px;
      top: 0;
      left: -18px;
      height: 100%;
    }

    &::after {
      transform: rotateZ(-45deg);
      top: 1rem;
      left: -8px;
    }
  }

  &:focus-within {
    .actions {
      visibility: visible;
      opacity: 1;
      left: 125%;
    }
  }

  .actions li:hover ul {
    visibility: visible;
    opacity: 1;
    left: 115%;
  }
}
</style>
