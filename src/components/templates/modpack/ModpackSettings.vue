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
    />
    
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

    <ftb-slider
      label="Instance Memory"
      v-model="instanceSettings.memory"
      :currentValue="instanceSettings.memory"
      minValue="512"
      :maxValue="settingsState.hardware.totalMemory"
      @blur="saveSettings"
      @change="saveSettings"
      step="64"
      unit="MB"
      css-class="memory"
      :dark="true"
      class="mb-8"
      :raw-style="`background: linear-gradient(to right, #8e0c25 ${
        (this.instance.minMemory / settingsState.hardware.totalMemory) * 100 - 5
      }%, #a55805 ${(this.instance.minMemory / settingsState.hardware.totalMemory) * 100}%, #a55805 ${
        (this.instance.recMemory / settingsState.hardware.totalMemory) * 100 - 5
      }%, #005540 ${(this.instance.recMemory / settingsState.hardware.totalMemory) * 100}%);`"
    />
    
    <ftb-toggle
      label="Fullscreen"
      :value="instanceSettings.fullScreen"
      @change="v => {
          instanceSettings.fullScreen = v;
          saveSettings();
      }"
      class="mb-4"
      small="Always open Minecraft in Fullscreen mode"
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

    <ftb-toggle
      label="Enable cloud save uploads"
      :disabled="canUseCloudSaves || toggleSavesWorking"
      onColor="bg-primary"
      :value="instanceSettings.cloudSaves"
      @change="toggleCloudSaves"
      small="You can only use Cloud Saves if you have an active paid plan on MineTogether."
      class="mb-8"
    />

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

          <ui-button type="primary" icon="folder" @click="browseForJava">Browse</ui-button>
        </div>
      </section>

      <ftb-input
        label="Java runtime arguments"
        placeholder="-TestArgument=120"
        v-model="instanceSettings.jvmArgs"
        @blur="saveSettings"
        class="flex-1"
      />
      
      
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
      />
    </modal>
    
    <share-instance-modal :open="shareConfirm" @closed="shareConfirm = false" :uuid="instance.uuid" />
  </div>
</template>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import {Getter, State} from 'vuex-class';
import {AuthState} from '@/modules/auth/types';
import {JavaVersion, SettingsState} from '@/modules/settings/types';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import FTBSlider from '@/components/atoms/input/FTBSlider.vue';
import ShareInstanceModal from '@/components/organisms/modals/actions/ShareInstanceModal.vue';
import Platform from '@/utils/interface/electron-overwolf';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {gobbleError, toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {InstanceController, SaveJson} from '@/core/controllers/InstanceController';
import {InstanceJson, SugaredInstanceJson} from '@/core/@types/javaApi';
import {RouterNames} from '@/router';
import {button, dialog, dialogsController} from '@/core/controllers/dialogsController';
import {alertController} from '@/core/controllers/alertController';
import DuplicateInstanceModal from '@/components/organisms/modals/actions/DuplicateInstanceModal.vue';
import {ReleaseChannelOptions} from '@/utils/commonOptions';
import Selection2 from '@/components/core/ui/Selection2.vue';
import ArtworkSelector from '@/components/core/modpack/components/ArtworkSelector.vue';
import UiButton from '@/components/core/ui/UiButton.vue';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {typeIdToProvider} from '@/utils/helpers/packHelpers';
import CategorySelector from '@/components/core/modpack/create/CategorySelector.vue';
import {computeAspectRatio} from '@/utils';

@Component({
  components: {
    CategorySelector,
    UiButton,
    ArtworkSelector,
    Selection2,
    DuplicateInstanceModal,
    'ftb-toggle': FTBToggle,
    'ftb-slider': FTBSlider,
    ShareInstanceModal,
  },
})
export default class ModpackSettings extends Vue {
  // Vuex
  @State('auth') public auth!: AuthState;
  @State('settings') public settingsState!: SettingsState;
  @Getter('getActiveProfile', { namespace: 'core' }) public getActiveMcProfile!: any;

  @Prop() instance!: InstanceJson | SugaredInstanceJson;
  
  instanceSettings: SaveJson = {} as any;
  previousSettings: SaveJson = {} as any;

  showDuplicate = false;
  shareConfirm = false;
  jreSelection = '';
  javaVersions: JavaVersion[] = [];
  deleting = false;
  
  toggleSavesWorking = false;
  
  imageFile: File | null = null;
  resolutionId = "";

  mounted() {
    this.instanceSettings = this.createInstanceSettingsFromInstance(this.instance)
    
    this.previousSettings = {
      ...this.instanceSettings
    }
    
    sendMessage("getJavas", {})
      .then(data => {
        this.javaVersions = data.javas;
        
        if (this.instance.embeddedJre) {
          return;
        }
        
        // Java version not in our list, thus it must be custom so flag it as custom
        if (!data.javas.find((e) => e.path === this.instanceSettings.jrePath)) {
          this.jreSelection = '-1';
        }
      })
      .catch(console.error)
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

  get canUseCloudSaves() {
    return (
      this.auth.token?.activePlan !== null &&
      !this.settingsState.settings.cloudSaves &&
      (this.settingsState.settings.cloudSaves as boolean | 'true' | 'false') !== 'true'
    );
  }

  get channelOptions() {
    return ReleaseChannelOptions(true);
  }
}
</script>

<style scoped lang="scss">
.pack-settings {
  font-size: 14px;
}
</style>
