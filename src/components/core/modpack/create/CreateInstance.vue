<template>
  <modal :open="open" @closed="close" :external-contents="true" title="Create instance" sub-title="Build your own Vanilla or Modded experience">
    <modal-body>
      <header>
        <div class="steps flex gap-6 pt-4 pb-6">
          <div class="step flex-1" :class="{active: step === 0, done: step > 0}"><font-awesome-icon fixedWidth icon="info" /> About <span /></div>
          <div class="step flex-1" :class="{active: step === 1, done: step > 1}"><font-awesome-icon fixedWidth icon="boxes" /> Mod Loader <span /></div>
          <div class="step flex-1" :class="{active: step === 2}"><font-awesome-icon fixedWidth icon="puzzle-piece" /> Settings <span /></div>
        </div>
      </header>
      <div class="about" v-show="step === 0">
        <template v-if="!loadingVanilla">
          <artwork-selector class="mb-6" v-model="userSelectedArtwork" />
          <ftb-input label="Name" placeholder="Next best instance!" v-model="userPackName" class="mb-4" />
          <selection2 :open-up="true" label="Minecraft version" class="mb-4" :options="vanillaVersions" v-model="userVanillaVersion" />
          
          <ui-toggle class="mb-4" label="Show snapshots" desc="Snapshot versions of Minecraft are typically unstable and no longer maintained" v-model="showVanillaSnapshots" />
          
          <category-selector v-model="userCategory" />
        </template>
        <Loader v-else />
      </div>
      
      <div class="modloader" v-show="step === 1">
          <modloader-select :mc-version="selectedMcVersion" @select="v => userModLoader = v ?? null" :show-optional="true" />
      </div>
      
      <div class="settings" v-show="step === 2">
        <div class="mb-4 header flex items-center justify-between">
          <h3 class="font-bold">Instance settings</h3>
          <ui-toggle label="Override defaults" class="ui-toggle" :align-right="true" v-model="userOverrideInstanceDefaults" />  
        </div>
        
        <div :class="{'opacity-25 duration-200 transition-opacity pointer-events-none cursor-not-allowed': !userOverrideInstanceDefaults}">
          <ram-slider v-model="settingRam" class="mb-4" />
        </div>

        <hr />
        
        <div class="mb-4 header flex items-center justify-between">
          <h3 class=" font-bold">Preferences</h3>
          <ui-toggle label="Override defaults" class="ui-toggle" :align-right="true" v-model="userOverridePreferences" />
        </div>
        
        <div :class="{'opacity-25 duration-200 transition-opacity pointer-events-none cursor-not-allowed': !userOverridePreferences}">
          <ui-toggle label="Fullscreen" class="mb-4" desc="Set Minecraft to fullscreen the game when possible" v-model="settingFullscreen" />
          
          <div :class="{'opacity-25 duration-200 transition-opacity pointer-events-none cursor-not-allowed': settingFullscreen}">
            <selection2 class="mb-4" :open-up="true" :options="screenResolutions" label="Screen resolution" v-model="settingScreenResolution" @input="onScreenResolutionChange" />

            <div class="flex items-center mb-4">
              <div class="block flex-1 mr-2">
                <b>Width</b>
                <small class="text-muted block mt-2">The Minecraft windows screen width</small>
              </div>
              <ftb-input class="mb-0" v-model="userWidth" />
            </div>
            <div class="flex items-center">
              <div class="block flex-1 mr-2">
                <b>Height</b>
                <small class="text-muted block mt-2">The Minecraft windows screen height</small>
              </div>
              <ftb-input class="mb-0" v-model="userHeight" />
            </div>
          </div>
        </div>
        <hr />
        
        <h3 class="mb-4 font-bold">Cloud Sync</h3>

        <ui-toggle
          :align-right="true"
          label="Enable cloud sync uploads"
          desc="You can only use Cloud sync if you have an active paid plan on MineTogether."
          :disabled="!accountHasPlan"
          v-model="userCloudSaves"
        />
        
        <p class="mt-4 text-light-warning" v-if="!accountHasPlan">Cloud syncing / Cloud saves are only available to Premium MineTogether users. Find out more on the <a class="text-blue-500 hover:text-blue-200" @click="openExternal" href="https://minetogether.io">MineTogether website</a>.</p>
      </div>
    </modal-body>
    
    <modal-footer>
      <div class="flex justify-end">
        <ui-button v-if="step > 0" icon="arrow-left" class="mr-2" @click="step --">
          Back
        </ui-button>
        <ui-button :wider="true" type="success" v-if="step < 2" :disabled="!canProceed()" icon="arrow-right" @click="step ++">
          Next
        </ui-button>
        <ui-button :wider="true" type="success" v-if="step === 2" icon="arrow-right" @click="createInstance">
          Create
        </ui-button>
      </div>
    </modal-footer>
  </modal>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue, Watch} from 'vue-property-decorator';
import ArtworkSelector from '@/components/core/modpack/components/ArtworkSelector.vue';
import Selection2, {SelectionOption} from '@/components/core/ui/Selection2.vue';
import {ns} from '@/core/state/appState';
import {GetModpack} from '@/core/state/modpacks/modpacksState';
import {Action, State} from 'vuex-class';
import {ModPack} from '@/modules/modpacks/types';
import UiButton from '@/components/core/ui/UiButton.vue';
import {stringIsEmpty, toTitleCase} from '@/utils/helpers/stringHelpers';
import FTBSlider from '@/components/atoms/input/FTBSlider.vue';
import {SettingsState} from '@/modules/settings/types';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {alertController} from '@/core/controllers/alertController';
import Loader from '@/components/atoms/Loader.vue';
import {ModLoader} from '@/core/@types/modpacks/modloaders';
import CategorySelector from '@/components/core/modpack/create/CategorySelector.vue';
import ModloaderSelect from '@/components/core/modpack/components/ModloaderSelect.vue';
import UiToggle from '@/components/core/ui/UiToggle.vue';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import RamSlider from '@/components/core/modpack/components/RamSlider.vue';
import {AuthState} from '@/modules/auth/types';

@Component({
  components: {
    RamSlider,
    UiToggle,
    ModloaderSelect, CategorySelector, Loader, FTBSlider, UiButton, Selection2, ArtworkSelector},
  methods: {
    toTitleCase,
    stringIsEmpty
  }
})
export default class CreateInstance extends Vue {
  @State('auth') public auth!: AuthState;
  @State('settings') public settingsState!: SettingsState;
  
  @Action("getModpack", ns("v2/modpacks")) getModpack!: GetModpack;
  @State('settings') private settings!: SettingsState;
  
  @Prop() open!: boolean;
  @Emit() close() {
    this.step = 0;
    this.userSelectedArtwork = null;
    this.userPackName = "";
    this.userVanillaVersion = -1;
    this.showVanillaSnapshots = false;
  }
  
  step = 0;
  
  userSelectedArtwork: File | null = null;
  userPackName = "";
  userVanillaVersion = -1;
  userModLoader: [string, ModLoader] | null = null
  userCategory = "Default";
  
  vanillaPack: ModPack | null = null;
  showVanillaSnapshots = false;
  
  loadingVanilla = false;
  
  fatalError = false;
  
  userOverrideInstanceDefaults = false;
  userOverridePreferences = false;
  
  settingFullscreen = false;
  settingScreenResolution = "";
  settingRam = 0;
  userCloudSaves = false;
  userHeight = 0;
  userWidth = 0;
  
  async mounted() {
    await this.loadInitialState();
    
    this.settingFullscreen = this.settings.settings.fullScreen;
    this.settingScreenResolution = this.settings.settings.width + "x" + this.settings.settings.height;
    this.settingRam = this.settings.settings.memory;
    this.userWidth = this.settings.settings.width;
    this.userHeight = this.settings.settings.height;
  }
  
  onScreenResolutionChange(newVal: string) {
    const [width, height] = newVal.split("x");
    this.userWidth = parseInt(width);
    this.userHeight = parseInt(height);
  }
  
  @Watch("open")
  async setupComponent() {
    if (!this.open) {
      return
    }

     await this.loadInitialState();
  }
  
  async loadInitialState() {
    this.vanillaPack = await toggleBeforeAndAfter(() => this.getModpack({
      id: 81,
      provider: "modpacksch"
    }) ?? null, state => this.loadingVanilla = state);
    
    if (!this.vanillaPack) {
      alertController.error("Failed to load Minecraft versions")
      this.fatalError = true;
      return;
    }

    const vanillaLatest = this.vanillaVersions[0];
    this.userPackName = `Minecraft ${vanillaLatest.label}`;
    this.userVanillaVersion = vanillaLatest.value;
  }
  
  @Watch("userVanillaVersion")
  async onVanillaVersionChange(newVal: number, oldVal: number) {
    if (!this.vanillaPack) {
      return;
    }
    
    const version = this.vanillaPack.versions.find(e => e.id === newVal);
    if (!version) {
      return;
    }
    
    const lastVersion = this.vanillaPack.versions.find(e => e.id === oldVal);
    // Update pack name
    if (this.userPackName === `Minecraft ${lastVersion?.name}`) {
      this.userPackName = `Minecraft ${version.name}`;
    }
  }
  
  // This may be more complex in the future
  canProceed() {
    if (this.fatalError) {
      return false;
    }
    
    switch (this.step) {
      case 0:
        return !stringIsEmpty(this.userPackName) && this.userVanillaVersion !== -1;
      case 1:
      case 2:
        return true;
      default:
        return false;
    }
  }

  /**
   * Due to the design of the api and the subprocess, we kinda need to build a special request for each type of 
   * pack that is configurable here. 
   * 
   * Vanilla with no mod loader? Just use the vanilla packid and versionid
   * Modloader? Use the modloader packid and versionid but this is filtered based on the vanilla pack
   */
  createInstance() {
    const sharedData = {
      name: this.userPackName,
      logo: this.userSelectedArtwork?.path ?? "",
      versionName: "",
      private: false,
      category: this.userCategory,
      ourOwn: true,
      ram: this.settingRam == this.settingsState.settings.memory ? -1 : this.settingRam,
      fullscreen: this.settingFullscreen === this.settingsState.settings.fullScreen ? undefined : this.settingFullscreen,
      width: this.userWidth == this.settingsState.settings.width ? undefined : this.userWidth,
      height: this.userHeight == this.settingsState.settings.height ? undefined : this.userHeight,
      cloudSaves: this.userCloudSaves === this.settingsState.settings.cloudSaves ? undefined : this.userCloudSaves,
    }
    
    // Magic
    if (!this.userModLoader) {
      instanceInstallController.requestInstall({
        id: 81, // Vanilla pack id
        version: this.userVanillaVersion ?? 0,
        ...sharedData
      })
    } else {
      // We're working with a modloader
      const request = {
        id: this.userModLoader[1].pack,
        version: this.userModLoader[1].id,
        ...sharedData
      } as any;
      
      if (this.userModLoader[0] === "fabric") {
        request["mcVersion"] = this.selectedMcVersion;
      }
      
      instanceInstallController.requestInstall(request)
    }
    
    this.close()
  }
  
  get selectedMcVersion() {
    if (!this.vanillaPack || this.userVanillaVersion === -1) {
      return "unknown";
    }
    
    return this.vanillaPack.versions.find(e => e.id === this.userVanillaVersion)?.name ?? "unknown";
  }
  
  get vanillaVersions() {
    if (!this.vanillaPack) {
      return []
    }
    
    return this.vanillaPack.versions
      .filter(e => e.type.toLowerCase() === "release" || (e.type.toLowerCase() === "snapshot" && this.showVanillaSnapshots))
      .sort((a, b) => b.id - a.id)
      .map(e => ({
        label: e.name,
        value: e.id,
        meta: e.type
      })) as SelectionOption[]
  }
  
  get screenResolutions() {
    return this.settings.hardware.supportedResolutions.map(e => ({
      label: `${e.width}x${e.height}`,
      value: `${e.width}x${e.height}`
    }))
  }

  get accountHasPlan() {
    if (!this.auth || !this.auth.token || !this.auth.token?.activePlan) {
      return false;
    }

    const plan = this.auth.token.activePlan;
    return plan.status === "Active";
  }
}
</script>

<style lang="scss" scoped>
.steps {
  
  .step {
    display: flex;
    align-items: center;
    gap: .5rem;
    color: rgba(white, .5);
    
    &.active {
      color: var(--color-light-info-button);
      
      span {
        background: var(--color-info-button);
      }
    }
    
    &.done {
      color: var(--color-light-success-button);
      
      span {
        background: var(--color-success-button);
      }
    }
    
    span {
      flex: 1;
      height: 2px;
      background: rgba(white, .2);
      border-radius: 2px;
    }
  }
}

hr {
  border: 0;
  border-bottom: 1px solid rgba(white, .1);
  margin: 1rem 0;
}

.header {
  .ui-toggle {
    transform-origin: right center;
    transform: scale(.8);
  }
}
</style>