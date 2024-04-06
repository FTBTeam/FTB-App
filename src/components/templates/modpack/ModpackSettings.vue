<template>
  <div class="pack-settings">
    <artwork-selector :pack="instance" class="mb-4" v-model="imageFile" :allow-remove="false" @change="(v) => {
      this.instanceSettings.instanceImage = v ? v.path : null;
      saveSettings();
    }" />
    
    <ftb-input
      label="Instance Name"
      v-model="instanceSettings.name"
      @blur="saveSettings"
      class="mb-4"
    >
      <template #extra>
        <ui-button :icon="instanceSettings.locked ? 'unlock' : 'lock'" @click="toggleLock">
          {{instanceSettings.locked ? 'Unlock' : 'Lock'}} instance
        </ui-button>
      </template>
    </ftb-input>
  
    <category-selector :open-down="true" class="mb-6" v-model="instanceSettings.category" @input="() => saveSettings()" />

    <div class="buttons flex gap-4 mb-8">
      <ui-button size="small" type="info" icon="folder" @click="browseInstance()">
        Open Folder
      </ui-button>

      <ui-button size="small"
        :disabled="!getActiveMcProfile"
        :aria-label="
          !getActiveMcProfile
            ? 'You need to be logged in to your Minecraft account to share packs'
            : 'Share your modpack with friends'
        "
        @click="shareConfirm = true"
        type="info"
        icon="share"
      >
        Share
      </ui-button>

      <ui-button size="small" icon="copy" @click="showDuplicate = true" type="info" aria-label="Copy this instance to a new instance, mods, worlds and all">
        Duplicate
      </ui-button>
      
      <ui-button v-if="instance.id != -1" size="small" icon="wrench" type="warning" aria-label="Something not looking right? This might help!" @click="repairInstance">
        Repair
      </ui-button>
      
      <ui-button size="small" type="danger" icon="trash" @click="confirmDelete">
        Delete instance
      </ui-button>
    </div>

    <ram-slider class="mb-6" v-model="instanceSettings.memory" @change="saveSettings" />
    
    <ui-toggle
      label="Fullscreen"
      desc="Always open Minecraft in Fullscreen mode"
      v-model="instanceSettings.fullScreen"
      @input="() => {
          saveSettings();
      }"
      class="mb-4"
      :align-right="true"
    />
    
    <div class="mb-6" :class="{'cursor-not-allowed opacity-50 pointer-events-none': instanceSettings.fullScreen}">
      <div class="flex items-center mb-4">
        <div class="block flex-1 mr-2">
          <b>Size presets</b>
          <small class="text-muted block mt-2">Select a preset based on your system</small>
        </div>
        
        <selection2
          v-if="resolutionList.length"
          v-model="resolutionId"
          @change="selectResolution"
          :style="{width: '220px'}"
          :options="resolutionList"
        />
      </div>
      <div class="flex items-center mb-4">
        <div class="block flex-1 mr-2">
          <b>Width</b>
          <small class="text-muted block mt-2">The Minecraft windows screen width</small>
        </div>
        <ftb-input class="mb-0" v-model="instanceSettings.width" @blur="saveSettings" />
      </div>
      <div class="flex items-center">
        <div class="block flex-1 mr-2">
          <b>Height</b>
          <small class="text-muted block mt-2">The Minecraft windows screen height</small>
        </div>
        <ftb-input class="mb-0" v-model="instanceSettings.height" @blur="saveSettings" />
      </div>
    </div>

    <ui-toggle
      :align-right="true"
      label="Enable cloud sync uploads"
      desc="You can only use Cloud sync if you have an active paid plan on MineTogether."
      :disabled="!accountHasPlan || toggleSavesWorking"
      :value="instanceSettings.cloudSaves"
      @input="toggleCloudSaves"
      class="mb-2"
    />

    <p class="mb-6 text-light-warning" v-if="!accountHasPlan">Cloud syncing / Cloud saves are only available to Premium MineTogether users. Find out more on the <a class="text-blue-500 hover:text-blue-200" @click="openExternal" href="https://minetogether.io">MineTogether website</a>.</p>
    <span v-else class="block mb-6" />
    
    <h2 class="text-lg mb-4 font-bold text-warning">
      <font-awesome-icon icon="warning" class="mr-2" />
      Advanced
    </h2>
    
    <div class="mb-8">
      <div class="flex items-center mb-6">
        <div class="block flex-1 mr-2">
          <b>Release Channel</b>
          <small class="block text-muted mr-6 mt-2">
            The selected release channel will determine when we show that a supported modpack has an update.<span class="mb-2 block" /> Release is the most stable, then Beta should be playable and Alpha could introduce game breaking bugs.</small>
        </div>

        <selection2
          :options="channelOptions"
          v-model="instanceSettings.releaseChannel"
          :style="{width: '192px'}"
          @change="v => saveSettings()"
        />
      </div>

      <div class="flex items-center mb-6">
        <div class="block flex-1 mr-2" :class="{'opacity-75': instance.locked}">
          <b>Modloader</b>
          <div v-if="instance.locked" class="mt-2 text-red-400">The instances Modloader can not be modified whilst the instance is locked.</div>
          <small class="block text-muted mr-6 mt-2">At any point you can update / down grade your mod loader for any modpack. This can sometimes be a destructive action and we recommend only doing this when you know what you're doing.</small>
          
          <div class="buttons flex gap-2 mt-4" v-if="!instance.locked">
            <ui-button v-if="!hasModloader" size="small" type="info" icon="download" @click="userSelectModLoader = true">Install Modloader</ui-button>
            <ui-button v-else size="small" type="info" icon="pen" @click="userSelectModLoader = true">Update Modloader</ui-button>
          </div>
        </div>
      </div>

      <section class="flex-1 mb-4">
        <label class="block tracking-wide text-white-700 mb-2">Java Version</label>
        <div class="flex items-center gap-4">
          <select
            class="appearance-none block flex-1 bg-input text-gray-400 border border-input py-3 px-4 leading-tight focus:outline-none rounded w-full"
            v-model="jreSelection"
            @change="updateJrePath"
          >
            <option value="-1" v-if="jreSelection === '-1'" disabled>
              Custom selection ({{ instanceSettings.jrePath }})
            </option>
            <option
              v-for="index in Object.keys(javaVersions)"
              :value="javaVersions[index].path"
              :key="javaVersions[index].name"
            >
              {{ javaVersions[index].name }}
            </option>
          </select>

          <ui-button type="success" icon="folder" @click="browseForJava">Browse</ui-button>
        </div>
      </section>
      
      <ftb-input
        label="Java runtime arguments"
        placeholder="-TestArgument=120"
        v-model="instanceSettings.jvmArgs"
        @blur="saveSettings"
        class="flex-1"
      />
      <small class="text-muted block mb-6 max-w-xl">
        These arguments are appended to your instances upon start, they are normal java arguments.
      </small>
      
      <ftb-input
        label="Shell arguments"
        :value="instanceSettings.shellArgs"
        v-model="instanceSettings.shellArgs"
        placeholder="/usr/local/application-wrapper"
        @blur="saveSettings"
      />
      <small class="text-muted block mb-6 max-w-xl">
        These arguments will be inserted before java is run, see the example below. It's recommended to not change these unless you know what you are doing.
      </small>

      <p class="mb-2">Startup preview</p>
      <small class="mb-4 block">This is for illustrative purposes only, this is not a complete example.</small>

      <code class="block bg-black rounded mb-6 px-2 py-2 overflow-x-auto select-text" v-if="instanceSettings.memory">
        {{instanceSettings.shellArgs}} java -jar minecraft.jar -Xmx{{prettyByteFormat(Math.floor(parseInt(instanceSettings.memory.toString()) * megabyteSize))}} {{instanceSettings.jvmArgs}}
      </code>
    </div>

    <modal
      :open="showDuplicate"
      :externalContents="true"
      @closed="showDuplicate = false"
      title="Duplicate Instance"
      subTitle="Are you sure?!"
    >
      <duplicate-instance-modal
        @finished="showDuplicate = false"
        :uuid="instance.uuid"
        :instanceName="instance.name"
        :category="instance.category"
      />
    </modal>
    
    <share-instance-modal :open="shareConfirm" @closed="shareConfirm = false" :uuid="instance.uuid" />
    
    <modal :open="userSelectModLoader" title="Select Modloader" :sub-title="`This instance is currently using ${hasModloader ? this.instance.modLoader : 'Vanilla'}`" @closed="() => {
      userSelectModLoader = false
      userSelectedLoader = null
    }">
      <div class="current mb-6 grid grid-cols-2 gap-y-2 wysiwyg items-center">
        <b>Current Modloader</b> <code>{{(hasModloader ? resolveModloader(instance) : 'Vanilla') | title}}</code>
        <b>Current version</b> <code>{{resolveModLoaderVersion(instance)}}</code>
      </div>
      
      <p class="mb-2">You can select a Modloader to install or switch to using the selection below. Please be aware that if you are currently running a modpack, switching Modloader version may break the modpack.</p>
      <p class="mb-6">Switching between Modloader provider (aka: Forge -> Fabric) will <b>not</b> remove incompatible mods.</p>
      
      <modloader-select @select="e => userSelectedLoader = e" :mc-version="instance.mcVersion" :provide-latest-option="false" :show-none="false" />
      
      <template #footer>
        <div class="flex justify-end gap-4">
          <ui-button type="warning" icon="times" @click="() => {
            userSelectModLoader = false
            userSelectedLoader = null
          }">Close</ui-button>
          <ui-button type="success" :wider="true" icon="download" :disabled="userSelectedLoader === null" @click="installModloader">Install</ui-button>
        </div>
      </template>
    </modal>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import {Action, Getter, State} from 'vuex-class';
import {JavaVersion, SettingsState} from '@/modules/settings/types';
import FTBSlider from '@/components/atoms/input/FTBSlider.vue';
import ShareInstanceModal from '@/components/organisms/modals/actions/ShareInstanceModal.vue';
import Platform from '@/utils/interface/electron-overwolf';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {gobbleError, toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {InstanceController, SaveJson} from '@/core/controllers/InstanceController';
import {InstanceJson, MineTogetherAccount, MineTogetherProfile, SugaredInstanceJson} from '@/core/@types/javaApi';
import {RouterNames} from '@/router';
import {button, dialog, dialogsController} from '@/core/controllers/dialogsController';
import {alertController} from '@/core/controllers/alertController';
import DuplicateInstanceModal from '@/components/organisms/modals/actions/DuplicateInstanceModal.vue';
import {ReleaseChannelOptions} from '@/utils/commonOptions';
import Selection2 from '@/components/core/ui/Selection2.vue';
import ArtworkSelector from '@/components/core/modpack/components/ArtworkSelector.vue';
import UiButton from '@/components/core/ui/UiButton.vue';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {resolveModloader, resolveModLoaderVersion, typeIdToProvider} from '@/utils/helpers/packHelpers';
import CategorySelector from '@/components/core/modpack/create/CategorySelector.vue';
import {computeAspectRatio, prettyByteFormat} from '@/utils';
import UiToggle from '@/components/core/ui/UiToggle.vue';
import ModloaderSelect from '@/components/core/modpack/components/ModloaderSelect.vue';
import {ModLoaderWithPackId} from '@/core/@types/modpacks/modloaders';
import RamSlider from '@/components/core/modpack/components/RamSlider.vue';
import {ns} from '@/core/state/appState';
import {ModLoaderUpdateState} from '@/core/@types/states/appState';
import KeyValueEditor from '@/components/core/modpack/components/KeyValueEditor.vue';

@Component({
  methods: {prettyByteFormat, resolveModLoaderVersion, resolveModloader},
  components: {
    KeyValueEditor,
    RamSlider,
    ModloaderSelect,
    UiToggle,
    CategorySelector,
    UiButton,
    ArtworkSelector,
    Selection2,
    DuplicateInstanceModal,
    'ftb-slider': FTBSlider,
    ShareInstanceModal,
  },
})

export default class ModpackSettings extends Vue {
  @State('settings') public settingsState!: SettingsState;
  @Getter('getActiveProfile', { namespace: 'core' }) public getActiveMcProfile!: any;

  @Getter("profile", ns("v2/mtauth")) getMtProfile!: MineTogetherProfile | null;
  @Getter("account", ns("v2/mtauth")) getMtAccount!: MineTogetherAccount | null;
  
  @Prop() instance!: InstanceJson | SugaredInstanceJson;
  @Action("addModloaderUpdate", ns("v2/install")) addModloaderUpdate!: (request: ModLoaderUpdateState) => void;
  
  instanceSettings: SaveJson = {} as any;
  previousSettings: SaveJson = {} as any;

  megabyteSize = 1024 * 1024;

  showDuplicate = false;
  shareConfirm = false;
  jreSelection = '';
  javaVersions: JavaVersion[] = [];
  deleting = false;
  
  toggleSavesWorking = false;
  
  imageFile: File | null = null;
  resolutionId = "";
  
  userSelectModLoader = false;
  userSelectedLoader: [string, ModLoaderWithPackId] | null = null;

  async mounted() {
    const javas = await sendMessage("getJavas");
    this.javaVersions = javas.javas;
    
    if (!this.instance.embeddedJre) {
      // Java version not in our list, thus it must be custom so flag it as custom
      if (!javas.javas.find((e) =>  e.path === this.instance.jrePath)) {
        this.jreSelection = '-1';
      } else {
        this.jreSelection = this.instance.jrePath;
      }
    }

    this.previousSettings = {
      ...this.instanceSettings
    }
    
    this.instanceSettings = this.createInstanceSettingsFromInstance(this.instance)
  }
  
  async toggleCloudSaves() {
    this.toggleSavesWorking = true;
    const newState = !this.instanceSettings.cloudSaves;
    
    if (!newState) {
      if (!(await dialogsController.createConfirmationDialog("Are you sure", "Disabling Cloudsaves will delete all of your cloudsave data, please make sure you have a backup of this instance if you plan to remove it from your system."))) {
        this.toggleSavesWorking = false;
        return;
      }
    }
    
    const reply = await sendMessage((newState ? "instanceEnableCloudSaves" : "instanceDisableCloudSaves"), {
      instance: this.instance.uuid,
    });
    
    if (!reply || reply.status !== "success") {
      alertController.error("Failed to toggle cloud saves");
      this.toggleSavesWorking = false;
      return;
    }
    
    this.instanceSettings.cloudSaves = newState;
    await this.saveSettings();
    this.toggleSavesWorking = false;
  }
  
  selectResolution(id: string) {
    const selected = this.settingsState.hardware.supportedResolutions.find(e => `${e.width}|${e.height}` === id);
    if (!selected) {
      return;
    }

    this.instanceSettings.width = selected.width;
    this.instanceSettings.height = selected.height;
    this.saveSettings();
  }

  browseForJava() {
    Platform.get.io.selectFileDialog((path) => {
      if (typeof path !== 'undefined' && path == null) {
        alertController.error('Unable to set Java location as the path was not found')
        return;
      } else if (!path) {
        return;
      }

      const javaVersion = this.javaVersions.find((e) => e.path === path);
      this.jreSelection = !javaVersion ? '-1' : javaVersion.path;

      this.instanceSettings.jrePath = path;
      this.saveSettings();
    });
  }

  updateJrePath(value: any) {
    this.instanceSettings.jrePath = value.target.value;
    this.saveSettings();
  }

  async repairInstance() {
    if (!(await dialogsController.createConfirmationDialog("Are you sure?", "We will attempt to repair this instance by reinstalling the modpack around your existing files. Even though this shouldn't remove any of your data, we recommend you make a backup of this instance before continuing."))) {
      return;
    }

    await instanceInstallController.requestUpdate(this.instance, this.instance.versionId, typeIdToProvider(this.instance.packType));
    this.$emit("back")
  }

  async installModloader() {
    if (!this.userSelectedLoader) {
      return;
    }
    
    const result = await sendMessage("instanceOverrideModLoader", {
      uuid: this.instance.uuid,
      modLoaderId: parseInt(this.userSelectedLoader[1].packId, 10),
      modLoaderVersion: this.userSelectedLoader[1].id,
    });
    
    if (result.status !== "error") {
      this.userSelectModLoader = false;
      this.userSelectedLoader = null;
      
      if (result.status === "prepare") {
        this.addModloaderUpdate({
          instanceId: this.instance.uuid,
          packetId: result.requestId
        })
      }
      
      this.$emit("back");
    }
  }

  async toggleLock() {
    const newState = !this.instanceSettings.locked;
    if (!newState && !(await dialogsController.createConfirmationDialog("Are you sure?", "Unlocking this instance will allow you to add extra mods and modify the instance in other ways. This can allow for destructive actions!\n\nAre you sure you want to unlock this instance?"))) {
      return;
    }
    this.instanceSettings.locked = newState;
    await this.saveSettings();
  }

  public async saveSettings() {
    // Compare the previous settings to the current settings
    // Yes... this is really how we do it...
    if (JSON.stringify(this.previousSettings) === JSON.stringify(this.instanceSettings)) {
      return;
    }
    
    const result = await InstanceController.from(this.instance)
      .updateInstance(this.instanceSettings);

    if (result) {
      alertController.success("Settings saved!")
      
      // Update the previous settings
      this.instanceSettings = this.createInstanceSettingsFromInstance(result.instanceJson)
      this.previousSettings = {
        ...this.instanceSettings
      }
    } else {
      alertController.error("Failed to save settings")
    }
  }

  async browseInstance() {
    await sendMessage("instanceBrowse", {
      uuid: this.instance?.uuid ?? "",
      folder: null
    })
  }

  public confirmDelete() {
    const dialogRef = dialogsController.createDialog(
      dialog("Are you sure?")
        .withContent(`Are you absolutely sure you want to delete \`${this.instance.name}\`! Doing this **WILL permanently** delete all mods, world saves, configurations, and all the rest... There is no way to recover this pack after deletion...`)
        .withType("warning")
        .withButton(button("Delete")
          .withAction(async () => {
            dialogRef.setWorking(true)
            await this.deleteInstance()
            dialogRef.close();
          })
          .withIcon("trash")
          .withType("error")
          .build())
        .build()
    )
  }
  
  createInstanceSettingsFromInstance(instance: InstanceJson): SaveJson {
    this.resolutionId = this.resolutionList
      .find((e) => e.value === `${instance.width ?? ''}|${instance.height ?? ''}`)
      ?.value ?? "";
    
    return {
      name: instance.name,
      jvmArgs: instance.jvmArgs,
      jrePath: instance.jrePath,
      memory: instance.memory,
      width: instance.width,
      height: instance.height,
      cloudSaves: instance.cloudSaves,
      fullScreen: instance.fullscreen,
      releaseChannel: instance.releaseChannel,
      category: instance.category,
      locked: instance.locked,
      shellArgs: instance.shellArgs,
    }
  }
  
  public async deleteInstance() {
    this.deleting = true;
    
    const controller = InstanceController.from(this.instance);
    await toggleBeforeAndAfter(() => controller.deleteInstance(), state => this.deleting = state);
    await gobbleError(() => this.$router.push({
      name: RouterNames.ROOT_LIBRARY
    }));
  }

  get resolutionList() {
    const resList = [];
    resList.push({
      value: "",
      label: "Custom",
      meta: "Custom"
    });
    
    for (const res of this.settingsState.hardware.supportedResolutions) {
      resList.push({
        value: `${res.width}|${res.height}`,
        label: `${res.width} x ${res.height}`,
        // Calculate the aspect ratio in the form of a 16:9 for example
        meta: computeAspectRatio(res.width, res.height)
      })
    }
    
    return resList;
  }

  get accountHasPlan() {
    if (!this.getMtAccount?.activePlan) {
      return false;
    }
    
    const plan = this.getMtAccount.activePlan;
    return plan.status === "Active";
  }

  get channelOptions() {
    return ReleaseChannelOptions(true);
  }
  
  get hasModloader() {
    return this.instance?.modLoader !== this.instance.mcVersion;
  }
}
</script>

<style scoped lang="scss">
.pack-settings {
  font-size: 14px;
}
</style>
