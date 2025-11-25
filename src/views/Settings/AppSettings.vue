<script lang="ts" setup>
import appPlatform from '@platform';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {alertController} from '@/core/controllers/alertController';
import {InstanceActions} from '@/core/actions/instanceActions';
import {MoveInstancesHandlerReply, OperationProgressUpdateData, PurgeTarget, SettingsData} from '@/core/types/javaApi';
import {ProgressBar, Loader, Modal, UiToggle, UiButton} from '@/components/ui';
import {dialogsController} from '@/core/controllers/dialogsController';
import {toSentenceCase, toTitleCase} from '@/utils/helpers/stringHelpers';
import {createLogger} from '@/core/logger';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import {onMounted, ref} from 'vue';
import {useAppSettings} from '@/store/appSettingsStore.ts';
import {faCheck, faCopy, faFileZipper, faFolderOpen, faSync, faTrash} from '@fortawesome/free-solid-svg-icons';
import {faGithub} from '@fortawesome/free-brands-svg-icons';
import {useAppStore} from '@/store/appStore.ts';
import PurgeCacheCard from "@/components/groups/settings/PurgeCacheCard.vue";

const appSettingsStore = useAppSettings();
const appStore = useAppStore();

const logger = createLogger("AppSettings.vue");
const localSettings = ref<SettingsData>({} as SettingsData);

const working = ref(false);
const uploadingLogs = ref(false);

const instanceMoveModalShow = ref(false);
const instanceMoveModalStage = ref("Preparing");
const instanceMoveModalComplete = ref(false);
const instanceMoveLocations = ref({old: "", new: ""});

const osType = ref<string | null>(null);
const includeInstanceLogs = ref(false);

const refreshing = ref(false);

onMounted(async () => {
  await appSettingsStore.loadSettings()
  // Make a copy of the settings so we don't mutate the vuex state
  localSettings.value = {...appSettingsStore.rootSettings} as any; // TODO: Fix typings
  osType.value = await appPlatform.utils.getOsType();
})


function exitOverwolf(value: boolean): void {
  localSettings.value.general.exitOverwolf = value;
  appSettingsStore.saveSettings(localSettings.value);
  appPlatform.actions.changeExitOverwolfSetting(value);
}

function enableFeralGameMode(value: boolean): void {
  localSettings.value.enableFeralGameMode = !value;
  appSettingsStore.saveSettings(localSettings.value);
}

function openFolder(location: string) {
  switch (location) {
    case 'home':
      toggleBeforeAndAfter(() => appPlatform.io.openFinder(appPlatform.io.appHome()), state => working.value = state)
      break;
    case 'instances':
      toggleBeforeAndAfter(() => appPlatform.io.openFinder(localSettings.value.instanceLocation), state => working.value = state)
      break;
    case 'logs':
      toggleBeforeAndAfter(() => appPlatform.io.openFinder(appPlatform.io.pathJoin(appPlatform.io.appHome(), 'logs')), state => working.value = state)
      break;
    default:
      toggleBeforeAndAfter(() => appPlatform.io.openFinder(location), state => working.value = state)
      break;
  }
}

async function uploadLogData() {
  uploadingLogs.value = true;

  try {
    const result = await sendMessage("uploadLogs", {
      includeInstanceLogs: includeInstanceLogs.value
    })

    if (result.path) {
      await appPlatform.io.openFinder(result.path)
      alertController.success('Logs saved to ' + result.path)
    } else {
      alertController.error('Failed to generate logs, Please let us know in our Discord / Github')
    }
  } catch (e) {
    logger.error("Failed to generate logs", e)
    alertController.error('Failed to generate logs, Please let us know in our Discord / Github')
  } finally {
    uploadingLogs.value = false;
  }
}

async function refreshCachePlz() {
  await InstanceActions.clearInstanceCache()
}

const configData = appPlatform.config

async function moveInstances() {
  const location: string | null = await new Promise(resolve => {
    appPlatform.io.selectFolderDialog(localSettings.value.instanceLocation, (path) => {
      if (path == null) {
        return;
      }

      resolve(path);
    });
  })

  if (!location) {
    return;
  }

  if (!(await dialogsController.createConfirmationDialog("Are you sure?", `This will move all your instances\n\nFrom \`${localSettings.value.instanceLocation}\`\n\nTo \`${location}\`\n\nthis may take a while.`))) {
    return;
  }

  const result = await sendMessage("moveInstances", {
    newLocation: location
  })

  if (result.state === "error") {
    return alertController.error(result.error);
  }

  if (result.state === "processing") {
    instanceMoveModalShow.value = true;
    instanceMoveModalStage.value = "Preparing";
    instanceMoveModalComplete.value = false;
    instanceMoveLocations.value = {
      old: localSettings.value.instanceLocation,
      new: location
    }
  }

  const migrationResult = await new Promise((res) => {
    const onMoveProgress = (data: any) => {
      if (data.type !== "operationUpdate" && data.type !== "moveInstancesReply") return;
      if (data.type === "operationUpdate") {
        const typedData = data as OperationProgressUpdateData;
        if (typedData.stage === "FINISHED") {
          // It's done
        } else {
          instanceMoveModalStage.value = toTitleCase(typedData.stage.toString().replace("_", " "));
        }
      } else {
        const typedData = data as MoveInstancesHandlerReply;
        if (typedData.state !== "success") {
          // It's broken
          instanceMoveModalShow.value = false;
          alertController.error(typedData.error);
          appStore.emitter.off("ws/message", onMoveProgress);
          res(false);
        } else {
          instanceMoveModalComplete.value = true;
          appStore.emitter.off("ws/message", onMoveProgress);
          res(true);
        }
      }
    }

    appStore.emitter.on("ws/message", onMoveProgress);
  })

  if (migrationResult) {
    localSettings.value.instanceLocation = location;
    await InstanceActions.clearInstanceCache(false)
  }
}

async function purge(type: PurgeTarget) {
  const prettyType = toSentenceCase(type.toLowerCase().replace("_", " ").replace("cache", "").trim());
  
  if (!(await dialogsController.createConfirmationDialog("Are you sure?", `This will purge the ${prettyType} cache. This action cannot be undone.`))) {
    return;
  }

  try {
    refreshing.value = true;
    const result = await sendMessage("purge", {
      target: type
    })
    
    if (result.status === "success") {
      alertController.success(`${prettyType} cache purged successfully.`);
    } else if (result.status === "error") {
      alertController.error(result.message);
      return;
    }
  } catch (e) {
    logger.error(`Failed to purge ${prettyType} cache`, e);
    alertController.error(`Failed to purge ${prettyType} cache. Please try again.`);
    return;
  } finally {
    refreshing.value = false;
  }
  
  if (type === "ALL") {
    await InstanceActions.clearInstanceCache();
  }
}
</script>

<template>
  <div class="app-settings" v-if="localSettings.spec">
    <div class="app-info-section mb-8">
      <div class="items sm:flex">
        <div class="title-value mr-10">
          <div class="title text-muted mb-1">App Version</div>
          <div class="value">
            <span class="select-text">{{ configData?.version }}</span>
            <div class="copy-me inline-block" aria-label="Click to copy" data-balloon-pos="up">
              <FontAwesomeIcon
                @click="appPlatform.cb.copy(configData?.version)"
                class="ml-2 cursor-pointer"
                :icon="faCopy"
                size="1x"
              />
            </div>
            <a class="cursor-pointer hover:underline" href="https://go.ftb.team/app-feedback" target="_blank"
            >
              <FontAwesomeIcon class="ml-2 cursor-pointer" :icon="faGithub" size="1x"
              />
            </a>
          </div>
        </div>
      </div>

      <router-link :to="{ name: 'license' }" class="hover:underline cursor-pointer text-muted text-sm mt-4 block"
      >Software License Information
      </router-link
      >
    </div>

    <ui-toggle
      v-if="!appPlatform.isElectron"
      label="Close Overwolf on Exit"
      desc="If enabled, we'll automatically close the Overwolf app when you exit the FTB App, can be useful if you don't use Overwolf."
      :value="localSettings.general.exitOverwolf"
      @input="exitOverwolf"
      class="mb-8"
    />

    <p class="block text-white-700 text-lg font-bold mb-4">Actions</p>

    <p class="block text-white-700 font-bold mb-4">Common Folders</p>
    <div class="flex items-center gap-4 mb-6">
      <UiButton :icon="faFolderOpen" @click="openFolder('home')" :working="working">Home</UiButton>
      <UiButton :icon="faFolderOpen" @click="openFolder('instances')" :working="working">Instances</UiButton>
      <UiButton :icon="faFolderOpen" @click="openFolder('logs')" :working="working">Logs</UiButton>
    </div>

    <div class="section logs mb-6">
      <div class="desc flex-1">
        <p class="font-bold mb-1">Export app logs</p>
        <p class="text-muted pr-10">
          If you're having an issue with the app, you can use this to create a ZIP file of the latest logs from the App.
          You can the
          provide these logs to our App team to investigate.
        </p>
      </div>

      <ui-toggle v-model="includeInstanceLogs" label="Include instance logs"
                 desc="When enabled, any logs from your Modpacks / instances will also be included in the export."
                 class="mt-4"/>
      <ui-button type="info" :working="uploadingLogs" class="mt-6" @click="uploadLogData" :icon="faFileZipper">Export
        logs
      </ui-button>
    </div>

    <p class="block text-white-700 text-lg font-bold mb-4 mt-6">Misc</p>

    <div class="section cache mb-6">
      <div class="desc flex-1">
        <p class="font-bold mb-1">Instance location</p>
        <p class="text-muted pr-10">
          Changing your instance location with instances installed will cause your instances to be moved to the new
          location automatically.
        </p>
      </div>

      <code class="select-text py-2 mt-4 block px-4 bg-black/30 rounded border border-white/30 font-mono">
        {{ localSettings.instanceLocation }}
      </code>

      <UiButton :icon="faFolderOpen" type="info" class="mt-4" @click="moveInstances" :working="working">
        Relocate
      </UiButton>
    </div>

    <ui-toggle
      v-if="osType === 'linux'"
      label="Enable Feral GameMode"
      desc="Enables Feral Interactive's GameMode, which can improve performance"
      v-model="localSettings.enableFeralGameMode"
      @input="enableFeralGameMode"
      class="mt-4"
    />

    <p class="block text-white-700 text-lg font-bold mb-4 mt-6">Caches & Data</p>

    <div class="section cache mb-6">
      <div class="desc flex-1">
        <p class="font-bold mb-1">Purge all</p>
        <p class="text-muted pr-10">
          Purges all of the caches below in one go. This isn't usually necessary, but can help resolve some issues.
        </p>
      </div>
      <ui-button type="danger" class="mt-4" @click="purge('ALL')" :icon="faTrash">Purge all</ui-button>
    </div>

    <p class="block text-white-700 text-lg font-bold mb-4 mt-6">Caches & Data</p>
    
    <div class="grid grid-cols-2 gap-4 mb-4">
      <PurgeCacheCard @clicked="refreshCachePlz" title="Reload instance data" button-title="Reload" desc="Reload all instance related data and update any cached packs" :working="refreshing" />
      <PurgeCacheCard @clicked="purge('INSTANCE_CACHE')" title="Purge instance cache" button-title="Purge" desc="Remove all instance cache meta data, typically download caches" :working="refreshing" />
      <PurgeCacheCard @clicked="purge('RUNTIMES')" title="Purge Java Runtimes" button-title="Purge" desc="Remove all 'managed' Java Runtimes from the app. They'll be installed again on next pack launch." :working="refreshing" />
      <PurgeCacheCard @clicked="purge('MINECRAFT')" title="Purge Minecraft & Modloader data" button-title="Purge" desc="Remove all 'managed' Minecraft / Modloader data from the app." :working="refreshing" />
    </div>
    
    <small>Please note: 'managed' refers to downloaded data managed by the app, not any external instances you might have. Purging Minecraft & Modloader data for example would only purge Minecraft instances managed by the FTB App.</small>

    <Modal :open="instanceMoveModalShow" title="Moving instances" :close-on-background-click="false"
           :has-closer="false">
      <template v-if="!instanceMoveModalComplete">
        <div class="wysiwyg mb-6">
          <p>This may take a while, please wait.</p>

          <p>Moving <code>{{ instanceMoveLocations.old }}</code><br/>To <code>{{ instanceMoveLocations.new }}</code></p>

          <p>Stage: <code>{{ instanceMoveModalStage }}</code></p>
        </div>

        <progress-bar :infinite="true"/>
      </template>
      <div class="wysiwyg" v-else>
        <p>Instances moved successfully 🎉</p>
      </div>

      <template #footer v-if="instanceMoveModalComplete">
        <div class="flex justify-end">
          <ui-button @click="instanceMoveModalShow = false" type="success" :icon="faCheck">Done</ui-button>
        </div>
      </template>
    </Modal>
  </div>
  <Loader v-else/>
</template>

<style scoped lang="scss">
.app-info-section {
  background-color: var(--color-background);
  padding: 1rem;
  border-radius: 5px;
}
</style>
