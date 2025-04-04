<template>
  <div class="app-settings" v-if="localSettings.spec">
    <div class="app-info-section mb-8">
      <div class="items sm:flex">
        <div class="title-value mr-10">
          <div class="title text-muted mb-1">App Version</div>
          <div class="value">
            <span class="select-text">{{ configData.version }}</span>
            <div class="copy-me inline-block" aria-label="Click to copy" data-balloon-pos="up">
              <font-awesome-icon
                @click="copyToClipboard(configData.version)"
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
      v-if="!platform.isElectron()"
      label="Close Overwolf on Exit"
      desc="If enabled, we'll automatically close the Overwolf app when you exit the FTB App, can be useful if you don't use Overwolf."
      :value="localSettings.general.exitOverwolf"
      @input="exitOverwolf"
      class="mb-8"
    />

    <p class="block text-white-700 text-lg font-bold mb-4" v-if="platform.isElectron()">Appearance</p>

    <ui-toggle
      v-if="platform.isElectron()"
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
    <ftb-input
      label="Relocate instances"
      :value="localSettings.instanceLocation + ' (Current)'"
      :disabled="true"
      button="true"
      buttonText="Relocate / Move"
      buttonColor="primary"
      :buttonClick="moveInstances"
    />
    <small class="text-muted block max-w-xl"
    >Changing your instance location with instances installed will cause your instances to be moved to the new
      location automatically.</small
    >

    <modal :open="instanceMoveModalShow" title="Moving instances" :close-on-background-click="false" :has-closer="false">
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
    </modal>
  </div>
  <Loader v-else />
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {SettingsState} from '@/modules/settings/types';
import {Action, State} from 'vuex-class';
import platform from '@/utils/interface/electron-overwolf';
import UiToggle from '@/components/ui/UiToggle.vue';
import UiButton from '@/components/ui/UiButton.vue';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {alertController} from '@/core/controllers/alertController';
import {InstanceActions} from '@/core/actions/instanceActions';
import {MoveInstancesHandlerReply, OperationProgressUpdateData, SettingsData} from '@/core/@types/javaApi';
import Loader from '@/components/ui/Loader.vue';
import ProgressBar from '@/components/ui/ProgressBar.vue';
import {dialogsController} from '@/core/controllers/dialogsController';
import {toTitleCase} from '@/utils/helpers/stringHelpers';
import {emitter} from '@/utils';
import {createLogger} from '@/core/logger';

@Component({
  components: {
    ProgressBar,
    Loader,
    UiButton,
    UiToggle,
  },
})
export default class AppSettings extends Vue {
  @State('settings') settingsState!: SettingsState;
  @Action('saveSettings', { namespace: 'settings' }) saveSettings: any;
  @Action('loadSettings', { namespace: 'settings' }) loadSettings: any;

  private logger = createLogger("Blog.vue");
  
  platform = platform;
  localSettings: SettingsData = {} as SettingsData;

  working = false;
  uploadingLogs = false;

  instanceMoveModalShow = false;
  instanceMoveModalStage = "Preparing";
  instanceMoveModalComplete = false;
  instanceMoveLocations = {old: "", new: ""};

  async created() {
    await this.loadSettings();

    // Make a copy of the settings so we don't mutate the vuex state
    this.localSettings = { ...this.settingsState.settings };
  }

  // enablePreview(value: boolean): void {
  //   this.localSettings.enablePreview = value;
  //   this.saveSettings(this.localSettings);
  // }

  exitOverwolf(value: boolean): void {
    this.localSettings.general.exitOverwolf = value;
    this.saveSettings(this.localSettings);
    platform.get.actions.changeExitOverwolfSetting(value);
  }

  toggleSystemStyleWindow(value: boolean): void {
    this.localSettings.appearance.useSystemWindowStyle = value;
    this.saveSettings(this.localSettings);

    platform.get.frame.setSystemWindowStyle(value);
  }

  openFolder(location: string) {
    switch (location) {
      case 'home':
        toggleBeforeAndAfter(() => platform.get.io.openFinder(platform.get.io.appHome()), state => this.working = state)
        break;
      case 'instances':
        toggleBeforeAndAfter(() => platform.get.io.openFinder(this.localSettings.instanceLocation), state => this.working = state)
        break;
      case 'logs':
        toggleBeforeAndAfter(() => platform.get.io.openFinder(platform.get.io.pathJoin(platform.get.io.appHome(), 'logs')), state => this.working = state)
        break;
      default:
        toggleBeforeAndAfter(() => platform.get.io.openFinder(location), state => this.working = state)
        break;
    }
  }

  public async uploadLogData() {
    this.uploadingLogs = true;
    try {
      const result = await sendMessage("uploadLogs", {})
      if (result.path) {
        await platform.get.io.openFinder(result.path)
        alertController.success('Logs saved to ' + result.path)
      } else {
        alertController.error('Failed to generate logs, Please let us know in our Discord / Github')
      }
    } catch (e) {
      this.logger.error("Failed to generate logs", e)
      alertController.error('Failed to generate logs, Please let us know in our Discord / Github')
    }

    this.uploadingLogs = false;
  }

  public async refreshCachePlz() {
    await InstanceActions.clearInstanceCache()
  }

  public enableVerbose(value: boolean): void {
    this.localSettings.general.verbose = value;
    this.saveSettings(this.localSettings);
  }

  get configData() {
    return platform.get.config
  }

  async moveInstances() {
    const location: string | null = await new Promise(resolve => {
      platform.get.io.selectFolderDialog(this.localSettings.instanceLocation, (path) => {
        if (path == null) {
          return;
        }

        resolve(path);
      });
    })

    if (!location) {
      return;
    }

    if (!(await dialogsController.createConfirmationDialog("Are you sure?", `This will move all your instances\n\nFrom \`${this.localSettings.instanceLocation}\`\n\nTo \`${location}\`\n\nthis may take a while.`))) {
      return;
    }

    const result = await sendMessage("moveInstances", {
      newLocation: location
    })

    if (result.state === "error") {
      return alertController.error(result.error);
    }

    if (result.state === "processing") {
      this.instanceMoveModalShow = true;
      this.instanceMoveModalStage = "Preparing";
      this.instanceMoveModalComplete = false;
      this.instanceMoveLocations = {
        old: this.localSettings.instanceLocation,
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
            this.instanceMoveModalStage = toTitleCase(typedData.stage.toString().replaceAll("_", " "));
          }
        } else {
          const typedData = data as MoveInstancesHandlerReply;
          if (typedData.state !== "success") {
            // It's broken
            this.instanceMoveModalShow = false;
            alertController.error(typedData.error);
            emitter.off("ws.message", onMoveProgress);
            res(false);
          } else {
            this.instanceMoveModalComplete = true;
            emitter.off("ws.message", onMoveProgress);
            res(true);
          }
        }
      }

      emitter.on("ws.message", onMoveProgress);
    })

    if (migrationResult) {
      this.localSettings.instanceLocation = location;
      await InstanceActions.clearInstanceCache(false)
    }
  }
}
</script>

<style scoped lang="scss">
.app-info-section {
  background-color: var(--color-background);
  padding: 1rem;
  border-radius: 5px;
}
</style>
