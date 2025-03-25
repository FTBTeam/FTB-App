<script lang="ts" setup>
import appPlatform from '@platform';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {alertController} from '@/core/controllers/alertController';
import {InstanceActions} from '@/core/actions/instanceActions';
import {MoveInstancesHandlerReply, OperationProgressUpdateData, SettingsData} from '@/core/types/javaApi';
import { ProgressBar, Loader, Modal, UiToggle, UiButton, FTBInput } from '@/components/ui';
import {dialogsController} from '@/core/controllers/dialogsController';
import {toTitleCase} from '@/utils/helpers/stringHelpers';
import {emitter} from '@/utils';
import {createLogger} from '@/core/logger';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { onMounted, ref } from 'vue';
import { useAppSettings } from '@/store/appSettingsStore.ts';

const appSettingsStore = useAppSettings();

const logger = createLogger("AppSettings.vue");
const localSettings = ref<SettingsData>({} as SettingsData);

const working = ref(false);
const uploadingLogs = ref(false);

const instanceMoveModalShow = ref(false);
const instanceMoveModalStage = ref("Preparing");
const instanceMoveModalComplete = ref(false);
const instanceMoveLocations = ref({old: "", new: ""});

onMounted(async () => {
  await appSettingsStore.loadSettings()
  // Make a copy of the settings so we don't mutate the vuex state
  localSettings.value = { ...appSettingsStore.rootSettings } as any; // TODO: Fix typings
})

function exitOverwolf(value: boolean): void {
  localSettings.value.general.exitOverwolf = value;
  appSettingsStore.saveSettings(localSettings.value);
  appPlatform.actions.changeExitOverwolfSetting(value);
}

function toggleSystemStyleWindow(value: boolean): void {
  localSettings.value.appearance.useSystemWindowStyle = value;
  appSettingsStore.saveSettings(localSettings.value);

  appPlatform.frame.setSystemWindowStyle(value);
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
    const result = await sendMessage("uploadLogs", {})
    if (result.path) {
      await appPlatform.io.openFinder(result.path)
      alertController.success('Logs saved to ' + result.path)
    } else {
      alertController.error('Failed to generate logs, Please let us know in our Discord / Github')
    }
  } catch (e) {
    logger.error("Failed to generate logs", e)
    alertController.error('Failed to generate logs, Please let us know in our Discord / Github')
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
          emitter.off("ws/message", onMoveProgress);
          res(false);
        } else {
          instanceMoveModalComplete.value = true;
          emitter.off("ws/message", onMoveProgress);
          res(true);
        }
      }
    }

    emitter.on("ws/message", onMoveProgress);
  })

  if (migrationResult) {
    localSettings.value.instanceLocation = location;
    await InstanceActions.clearInstanceCache(false)
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
                icon="copy"
                size="1x"
              />
            </div>
            <a class="cursor-pointer hover:underline" href="https://go.ftb.team/app-feedback" target="_blank"
            ><font-awesome-icon class="ml-2 cursor-pointer" :icon="['fab', 'github']" size="1x"
            /></a>
          </div>
        </div>
      </div>

      <router-link :to="{ name: 'license' }" class="hover:underline cursor-pointer text-muted text-sm mt-4 block"
      >Software License Information</router-link
      >
    </div>

    <!-- <ftb-toggle label="Enable Analytics: " :value="settingsCopy.enableAnalytics" @change="enableAnalytics"
                    onColor="bg-primary"/> -->
    <!--    <ui-toggle-->
    <!--      label="Use the beta channel"-->
    <!--      desc="This allows you to opt-in to the beta channel of the FTB App, this version is typically less stable than the normal release channel."-->
    <!--      :value="localSettings.enablePreview"-->
    <!--      @input="enablePreview"-->
    <!--      class="mb-8"-->
    <!--    />-->
    <!--    -->
    <ui-toggle
      v-if="!appPlatform.isElectron"
      label="Close Overwolf on Exit"
      desc="If enabled, we'll automatically close the Overwolf app when you exit the FTB App, can be useful if you don't use Overwolf."
      :value="localSettings.general.exitOverwolf"
      @input="exitOverwolf"
      class="mb-8"
    />

    <p class="block text-white-700 text-lg font-bold mb-4" v-if="appPlatform.isElectron">Appearance</p>

    <ui-toggle
      v-if="appPlatform.isElectron"
      label="Use systems window style"
      desc="Instead of using the apps internal Titlebar, we'll use the system's titlebar instead. This setting will restart the app!"
      v-model="localSettings.appearance.useSystemWindowStyle"
      @input="toggleSystemStyleWindow"
      class="mb-8"
    />


    <p class="block text-white-700 text-lg font-bold mb-4">Actions</p>

    <p class="block text-white-700 font-bold mb-4">Common Folders</p>
    <div class="flex items-center gap-4 mb-6">
      <ui-button size="small" type="info" icon="folder-open" @click="openFolder('home')" :working="working">Home</ui-button>
      <ui-button size="small" type="info" icon="folder-open" @click="openFolder('instances')" :working="working">Instances</ui-button>
      <ui-button size="small" type="info" icon="folder-open" @click="openFolder('logs')" :working="working">Logs</ui-button>
    </div>

    <div class="section logs mb-6 sm:flex items-center">
      <div class="desc flex-1">
        <p class="font-bold mb-1">Export app logs</p>
        <p class="text-muted pr-10">
          If you're having an issue with the app, you can use this to create a ZIP file of the latest logs from the App. You can the
          provide these logs to our App team to investigate.
        </p>
      </div>
      <ui-button :working="uploadingLogs" size="small" class="mt-6 sm:mt-0 my-2 w-2/7" type="info" @click="uploadLogData" icon="file-zipper">Create App Logs ZIP</ui-button>
    </div>

    <div class="section cache mb-6 sm:flex items-center">
      <div class="desc flex-1">
        <p class="font-bold mb-1">Manage cache</p>
        <p class="text-muted pr-10">
          Having an issue with the App loading content? Maybe you want to check if you've got the latest data? Refresh
          the cache, it'll make sure you're running the latest version of all available data.
        </p>
      </div>
      <ui-button size="small" class="mt-6 sm:mt-0 my-2 w-2/7" type="info" @click="refreshCachePlz" icon="sync">Refresh Cache</ui-button>
    </div>

    <p class="block text-white-700 text-lg font-bold mb-4 mt-6">Misc</p>
    <FTBInput
      label="Relocate instances"
      :value="localSettings.instanceLocation + ' (Current)'"
      :disabled="true"
      button="true"
      buttonText="Relocate / Move"
      buttonColor="primary"
      @click="moveInstances"
    />
    <small class="text-muted block max-w-xl"
    >Changing your instance location with instances installed will cause your instances to be moved to the new
      location automatically.</small
    >

    <Modal :open="instanceMoveModalShow" title="Moving instances" :close-on-background-click="false" :has-closer="false">
      <template v-if="!instanceMoveModalComplete">
        <div class="wysiwyg mb-6">
          <p>This may take a while, please wait.</p>

          <p>Moving <code>{{instanceMoveLocations.old}}</code><br/>To <code>{{instanceMoveLocations.new}}</code></p>

          <p>Stage: <code>{{instanceMoveModalStage}}</code></p>
        </div>

        <progress-bar :infinite="true" />
      </template>
      <div class="wysiwyg" v-else>
        <p>Instances moved successfully 🎉</p>
      </div>

      <template #footer v-if="instanceMoveModalComplete">
        <div class="flex justify-end">
          <ui-button @click="instanceMoveModalShow = false" type="success" icon="check">Done</ui-button>
        </div>
      </template>
    </Modal>
  </div>
  <Loader v-else />
</template>

<style scoped lang="scss">
.app-info-section {
  background-color: var(--color-background);
  padding: 1rem;
  border-radius: 5px;
}
</style>
