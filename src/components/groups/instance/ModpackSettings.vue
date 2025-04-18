<template>
  <div class="pack-settings">
    <artwork-selector :pack="instance" class="mb-4" v-model="imageFile" :allow-remove="false" @change="(v) => {
      instanceSettings.instanceImage = v ? v.path : null;
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
          @change="(v) => saveSettings()"
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
      
      <ui-toggle
        :align-right="true"
        label="Disable helper mod injection"
        desc="The FTB App will inject helper mods into your instance to help the app and your instance work together. Sometimes this can cause issues with Minecraft, Mods, Etc. You can disable this behaviour here."
        v-model="instanceSettings.preventMetaModInjection"
        @input="saveSettings"
        class="mb-6" />

      <section class="flex-1 mb-4">
        <label class="block tracking-wide text-white-700 font-bold mb-2">Java Version</label>
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
      
      <div class="flex gap-4 flex-col mb-6">
        <label class="block tracking-wide text-white-700 font-bold">
          Java runtime arguments
        </label>

        <small class="text-muted block -mt-2 max-w-xl">
          These arguments are appended to your instances upon start, they are normal java arguments. <em>New lines will be removed.</em>
        </small>

        <textarea
          placeholder="-TestArgument=120"
          v-model="instanceSettings.jvmArgs"
          @blur="() => {
            // Remove all new lines and trim the string
            instanceSettings.jvmArgs = instanceSettings.jvmArgs.trim().replaceAll(/(\r\n|\n|\r)/gm, '')
            saveSettings()
          }"
          spellcheck="false"
          rows="4"
          class="flex-1 mb-0 appearance-none block w-full ftb-btn bg-input text-gray-400 border border-input py-3 px-4 focus:outline-none rounded resize-none break-normal font-mono"
        />
        
        <div class="flex gap-4">
          <ui-button size="small" icon="undo" @click="() => {
            instanceSettings.jvmArgs = settingsState.settings.instanceDefaults.javaArgs
          }">
            Reset to Instance defaults
          </ui-button>

          <ui-button size="small" icon="undo" @click="() => {
            instanceSettings.jvmArgs = '-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M'
          }">
            Reset to Vanilla defaults
          </ui-button>
        </div>
        
        <ui-toggle label="Prefer IPv4 network requests" :value="prefersIPv4" @input="preferIPv4Clicked" />
      </div>

      <ftb-input
        label="Program arguments"
        :value="instanceSettings.programArgs"
        v-model="instanceSettings.programArgs"
        placeholder="--fullscreen"
        @blur="saveSettings"
      />
      <small class="text-muted block mb-6 max-w-xl">
        These arguments are appended to the end of the java command, these are typically arguments Minecraft uses.
      </small>
      
      <ftb-input
        label="Shell arguments"
        :value="instanceSettings.shellArgs"
        v-model="instanceSettings.shellArgs"
        placeholder="/usr/local/application-wrapper"
        @blur="saveSettings"
      />
      <p class="text-muted block mb-6 max-w-xl text-sm">
        These arguments will be inserted before java is run, see the example below. It's recommended to not change these unless you know what you are doing.
      </p>
      
      <p class="mb-2 font-bold">Startup preview</p>
      <p class="mb-4 block text-sm">This is for illustrative purposes only, this is not a complete example.</p>

      <code class="block bg-black rounded mb-6 px-2 py-2 overflow-x-auto select-text" v-if="instanceSettings.memory">
        {{instanceSettings.shellArgs}} java {{instanceSettings.jvmArgs}} -Xmx{{prettyByteFormat(Math.floor(parseInt(instanceSettings.memory.toString()) * megabyteSize))}} -jar minecraft.jar {{instanceSettings.programArgs}}
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
import {Action, State} from 'vuex-class';
import {JavaVersion, SettingsState} from '@/modules/settings/types';
import FTBSlider from '@/components/ui/input/FTBSlider.vue';
import Platform from '@/utils/interface/electron-overwolf';
import platform from '@/utils/interface/electron-overwolf';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {gobbleError, toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {InstanceController, SaveJson} from '@/core/controllers/InstanceController';
import {InstanceJson, SugaredInstanceJson} from '@/core/@types/javaApi';
import {RouterNames} from '@/router';
import {button, dialog, dialogsController} from '@/core/controllers/dialogsController';
import {alertController} from '@/core/controllers/alertController';
import DuplicateInstanceModal from '@/components/modals/actions/DuplicateInstanceModal.vue';
import {ReleaseChannelOptions} from '@/utils/commonOptions';
import Selection2 from '@/components/ui/Selection2.vue';
import ArtworkSelector from '@/components/groups/modpack/components/ArtworkSelector.vue';
import UiButton from '@/components/ui/UiButton.vue';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {resolveModloader, resolveModLoaderVersion, typeIdToProvider} from '@/utils/helpers/packHelpers';
import CategorySelector from '@/components/groups/modpack/create/CategorySelector.vue';
import {computeAspectRatio, megabyteSize, prettyByteFormat} from '@/utils';
import UiToggle from '@/components/ui/UiToggle.vue';
import ModloaderSelect from '@/components/groups/modpack/components/ModloaderSelect.vue';
import {ModLoaderWithPackId} from '@/core/@types/modpacks/modloaders';
import RamSlider from '@/components/groups/modpack/components/RamSlider.vue';
import {ns} from '@/core/state/appState';
import {ModLoaderUpdateState} from '@/core/@types/states/appState';
import KeyValueEditor from '@/components/groups/modpack/components/KeyValueEditor.vue';

const preferIPv4Arg = "-Djava.net.preferIPv4Stack=true"

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
    'ftb-slider': FTBSlider
  },
})
export default class ModpackSettings extends Vue {
  @State('settings') public settingsState!: SettingsState;

  @Prop() instance!: InstanceJson | SugaredInstanceJson;
  @Action("addModloaderUpdate", ns("v2/install")) addModloaderUpdate!: (request: ModLoaderUpdateState) => void;
  
  instanceSettings: SaveJson = {} as any;
  previousSettings: SaveJson = {} as any;

  megabyteSize = megabyteSize
  
  showDuplicate = false;
  jreSelection = '';
  javaVersions: JavaVersion[] = [];
  deleting = false;
  
  imageFile: File | null = null;
  resolutionId = "";
  
  userSelectModLoader = false;
  userSelectedLoader: [string, ModLoaderWithPackId] | null = null;

  platform = platform;
  
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
    await gobbleError(async () => {
      if ("path" in this.instance) {
        await this.platform.get.io.openFinder(this.instance.path)
        return;
      }
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
      fullScreen: instance.fullscreen,
      releaseChannel: instance.releaseChannel,
      category: instance.category,
      locked: instance.locked,
      shellArgs: instance.shellArgs,
      programArgs: instance.programArgs,
      preventMetaModInjection: instance.preventMetaModInjection,
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
  
  preferIPv4Clicked(event: any) {
    if (!event) {
      this.instanceSettings.jvmArgs = this.instanceSettings.jvmArgs.replace(preferIPv4Arg, "").trim()
    } else {
      this.instanceSettings.jvmArgs = `${this.instanceSettings.jvmArgs} ${preferIPv4Arg}`.trim()
    }
    this.saveSettings()
  }

  get prefersIPv4() {
    return this.instanceSettings?.jvmArgs?.includes(preferIPv4Arg)
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
