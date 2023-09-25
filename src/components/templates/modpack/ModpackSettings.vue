<template>
  <div class="pack-settings">
    <ftb-input
      label="Instance Name"
      v-model="instanceSettings.name"
      @blur="saveSettings"
      class="mb-6"
    />

    <div class="buttons flex gap-4 mb-8">
      <ftb-button class="py-2 px-3 text-sm" color="info" css-class="text-center text-l" @click="browseInstance()">
        <font-awesome-icon icon="folder" class="mr-2" size="1x" />
        Open Folder
      </ftb-button>

      <ftb-button
        :disabled="!getActiveMcProfile"
        :title="
          !getActiveMcProfile
            ? 'You need to be logged in to your Minecraft account to share packs'
            : 'Share your modpack with friends'
        "
        class="py-2 px-3 text-sm"
        color="info"
        css-class="text-center text-l"
        @click="shareConfirm = true"
      >
        <font-awesome-icon icon="share" class="mr-2" size="1x" />
        Share
      </ftb-button>

      <ftb-button class="py-2 px-3 text-sm" color="danger" css-class="text-center text-l" @click="confirmDelete()">
        <font-awesome-icon icon="trash" class="mr-2" size="1x" />
        Delete instance
      </ftb-button>
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
    
    <h3 class="font-bold text-lg mb-4">Minecraft window size</h3>
    <div class="mb-6">
      <div class="flex items-center mb-2">
        <div class="block flex-1 mr-2">
          <b>Size presets</b>
          <p class="text-muted text-sm">Select a preset based on your system</p>
        </div>
        
        <selection
          v-if="resolutionList.length"
          @selected="(e) => e && selectResolution(e)"
          :inheritedSelection="resolutionList.find((e) => e.text === `${instanceSettings.width ? instanceSettings.width.toString() : ''} x ${instanceSettings.height ? instanceSettings.height.toString() : ''}px`)"
          :style="{width: '240px'}"
          :options="resolutionList"
          :allow-deselect="false"
        />
      </div>
      <div class="flex items-center mb-2">
        <div class="block flex-1 mr-2">
          <b>Width</b>
          <p class="text-muted text-sm">The Minecraft windows screen width</p>
        </div>
        <ftb-input class="mb-0" v-model="instanceSettings.width" @blur="saveSettings" />
      </div>
      <div class="flex items-center">
        <div class="block flex-1 mr-2">
          <b>Height</b>
          <p class="text-muted text-sm">The Minecraft windows screen height</p>
        </div>
        <ftb-input class="mb-0" v-model="instanceSettings.height" @blur="saveSettings" />
      </div>
    </div>

    <ftb-toggle
      label="Enable cloud save uploads"
      :disabled="canUseCloudSaves"
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
    
    <h3 class="font-bold text-lg mb-2">Java Runtime</h3>
    <div class="flex items-center mb-8">
      <div class="w-1/2 mr-8 flex items-end justify-between">
        <section class="mr-4 flex-1">
          <label class="block uppercase tracking-wide text-white-700 text-xs font-bold mb-2"> Java Version </label>
          <select
            class="appearance-none block w-full bg-input text-gray-400 border border-input py-3 px-4 leading-tight focus:outline-none rounded w-full"
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
        </section>

        <ftb-button color="primary" class="py-2 px-4 mb-1" @click="browseForJava">
          <font-awesome-icon icon="folder" size="1x" class="cursor-pointer" />
          <span class="ml-4">Browse</span>
        </ftb-button>
      </div>

      <ftb-input
        label="Java runtime arguments"
        placeholder="-TestArgument=120"
        v-model="instanceSettings.jvmArgs"
        @blur="saveSettings"
        class="flex-1"
      />
    </div>
    

    <share-instance-modal :open="shareConfirm" @closed="shareConfirm = false" :uuid="instance.uuid" />
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import { Getter, State } from 'vuex-class';
import { AuthState } from '@/modules/auth/types';
import { JavaVersion, SettingsState } from '@/modules/settings/types';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import FTBSlider from '@/components/atoms/input/FTBSlider.vue';
import ShareInstanceModal from '@/components/organisms/modals/actions/ShareInstanceModal.vue';
import Platform from '@/utils/interface/electron-overwolf';
import Selection from '@/components/atoms/input/Selection.vue';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {gobbleError, toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {InstanceController, SaveJson} from '@/core/controllers/InstanceController';
import {InstanceJson, SugaredInstanceJson} from '@/core/@types/javaApi';
import {RouterNames} from '@/router';
import {button, dialog, dialogsController} from '@/core/controllers/dialogsController';
import {alertController} from '@/core/controllers/alertController';

@Component({
  components: {
    Selection,
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
  
  shareConfirm = false;
  jreSelection = '';
  javaVersions: JavaVersion[] = [];
  deleting = false;

  mounted() {
    this.instanceSettings = {
      name: this.instance.name,
      jvmArgs: this.instance.jvmArgs,
      jrePath: this.instance.jrePath,
      memory: this.instance.memory,
      width: this.instance.width,
      height: this.instance.height,
      cloudSaves: this.instance.cloudSaves, // TODO: Stop using
    }
    
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

  // I'm not sure this works, at best, it's VueX state mutation which is bad hmm kay...
  public toggleCloudSaves() {
    this.instanceSettings.cloudSaves = !this.instanceSettings.cloudSaves;
    this.saveSettings();
  }
  
  selectResolution(id: number) {
    const selected = this.settingsState.hardware.supportedResolutions[id];
    if (!selected) {
      return;
    }

    this.instanceSettings.width = this.settingsState.hardware.supportedResolutions[id].width;
    this.instanceSettings.height = this.settingsState.hardware.supportedResolutions[id].height;
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
    for (const [key, res] of Object.entries(this.settingsState.hardware.supportedResolutions)) {
      resList.push({
        value: key,
        text: `${res.width} x ${res.height}px`,
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
}
</script>

<style scoped lang="scss"></style>
