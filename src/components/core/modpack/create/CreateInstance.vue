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
          <f-t-b-toggle :value="showVanillaSnapshots" @change="v => showVanillaSnapshots = v" label="Show snapshots" class="mb-4" />
          <category-selector v-model="userCategory" />
        </template>
        <Loader v-else />
      </div>
      
      <div class="modloader" v-show="step === 1">
        <template v-if="!loadingModloaders && Object.keys(availableLoaders).length > 0">
          <message header="Optional" icon="info" type="info" class="mb-4">
            A Mod Loader will allow you to load mods into Minecraft. Each Mod Loader will have different mods available so we recommend looking at <a>CurseForge</a> to see what's available.
          </message>

          <b class="mb-4 block">Select a Mod Loader</b>
          
          <div class="loaders mb-6">
            <div class="loader" :class="{active: userLoaderProvider === index}" v-for="(_, index) in availableLoaders" :key="index" @click="userLoaderProvider = index">
              {{ index | title }}
            </div>
          </div>

          <f-t-b-toggle v-if="!stringIsEmpty(userLoaderProvider)" :value="userUseLatestLoader" @change="v => userUseLatestLoader = v" label="Use latest Mod Loader version (recommended)" />
          
          <selection2
            v-if="!userUseLatestLoader && loaderVersions.length > 0"
            :open-up="true" 
            label="Version"
            class="mb-4" 
            :options="loaderVersions" 
            v-model="userModLoader" 
          />
        </template>
        <Loader v-else-if="loadingModloaders" />
        <div v-else>
          <message header="No loaders" icon="exclamation" type="warning" class="mb-4">
            Sadly we were not able to find any mod loaders for Minecraft {{ selectedMcVersion }}. This is likely due to there being no mod loaders available just yet.
            <br><br>
            You can continue to create an instance without a mod loader and set one up later once one is available.
          </message>
        </div>        
      </div>
      
      <div class="settings" v-show="step === 2">
        <f-t-b-slider label="Ram" v-model="settingRam" />
        
        <f-t-b-toggle label="Fullscreen" :value="settingFullscreen" @change="v => settingFullscreen = v" />
        <selection2 :open-up="true" :options="screenResolutions" label="Screen resolution" v-model="settingScreenResolution" />
        
        <pre>{{ {
          fullscreen: settingFullscreen,
          screenResolution: settingScreenResolution,
          ram: settingRam
        } }}</pre>
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
import ArtworkSelector from '@/components/core/modpack/ArtworkSelector.vue';
import Selection2, {SelectionOption} from '@/components/atoms/input/Selection2.vue';
import {ns} from '@/core/state/appState';
import {GetModpack} from '@/core/state/modpacks/modpacksState';
import {Action, State} from 'vuex-class';
import {ModPack} from '@/modules/modpacks/types';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import UiButton from '@/components/core/ui/UiButton.vue';
import {stringIsEmpty, toTitleCase} from '@/utils/helpers/stringHelpers';
import {JavaFetch} from '@/core/javaFetch';
import FTBSlider from '@/components/atoms/input/FTBSlider.vue';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {SettingsState} from '@/modules/settings/types';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {alertController} from '@/core/controllers/alertController';
import Loader from '@/components/atoms/Loader.vue';
import {ModLoader, ModLoadersResponse} from '@/core/@types/modpacks/modloaders';
import CategorySelector from '@/components/core/modpack/create/CategorySelector.vue';

@Component({
  components: {CategorySelector, Loader, FTBSlider, UiButton, FTBToggle, Selection2, ArtworkSelector},
  methods: {
    toTitleCase,
    stringIsEmpty
  }
})
export default class CreateInstance extends Vue {
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
  userModLoader = ""
  userLoaderProvider = "";
  userUseLatestLoader = true;
  userCategory = "Default";
  
  vanillaPack: ModPack | null = null;
  showVanillaSnapshots = false;

  availableLoaders: Record<string, ModLoader[]> = {};
  loadingModloaders = false;
  loadingVanilla = false;
  
  fatalError = false;
  
  settingFullscreen = false;
  settingScreenResolution = "";
  settingRam = 0;
  
  async mounted() {
    await this.loadInitialState();
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
    
    await toggleBeforeAndAfter(() => this.loadAvailableLoaders(version.name), state => this.loadingModloaders = state);
  }
  
  async loadAvailableLoaders(mcVersion: string) {
    const knownLoaders = ["forge", "neoforge", "fabric"];
    const foundLoadersForVersion = {} as Record<string, ModLoader[]>;
    
    for (const loader of knownLoaders) {
      const request = await JavaFetch.modpacksCh(`loaders/${mcVersion}/${loader}`).execute()
      if (request?.status !== "success") {
        continue;
      }
      
      const loaderData = request.json<ModLoadersResponse>();
      if (loaderData.total === 0) {
        continue;
      }
      
      foundLoadersForVersion[loader] = loaderData.loaders.sort((a, b) => b.id - a.id);
    }
    
    this.availableLoaders = foundLoadersForVersion;
  }
  
  // Set the user loader to the first one available
  @Watch("userLoaderProvider")
  async onLoaderProviderChange(newVal: string, oldVal: string) {
    if (newVal === oldVal) {
      return;
    }
    
    this.userModLoader = this.loaderVersions[0].value;
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
    }
    
    // Magic
    if (stringIsEmpty(this.userModLoader)) {
      instanceInstallController.requestInstall({
        id: 81, // Vanilla pack id
        version: this.userVanillaVersion ?? 0,
        ...sharedData
      })
    } else {
      // Get the modloader pack id
      const modLoader = this.availableLoaders[this.userLoaderProvider]
        .find((e: any) => e.version === this.userModLoader);
      
      if (!modLoader) {
        alertController.error("Failed to find mod loader")
        return;
      }
      
      // We're working with a modloader
      instanceInstallController.requestInstall({
        id: modLoader.pack,
        version: modLoader.id,
        ...sharedData
      })
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
  
  get loaderVersions() {
    if (this.userLoaderProvider === "" || !this.availableLoaders[this.userLoaderProvider]) {
      return []
    }
    
    return this.availableLoaders[this.userLoaderProvider].map((e: any) => ({
      label: e.version,
      value: e.version,
      meta: e.type
    })) as SelectionOption[]
  }
  
  get screenResolutions() {
    return this.settings.hardware.supportedResolutions.map(e => ({
      label: `${e.width}x${e.height}`,
      value: `${e.width}x${e.height}`
    }))
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

.loaders {
  display: flex;
  
  .loader {
    cursor: pointer;
    flex: 1;
    padding: 1rem 0;
    text-align: center;
    background-color: var(--color-navbar);
    transition: background-color .25s ease-in-out;
    font-weight: bold;
    
    &:hover {
      background-color: var(--color-light-primary-button);
    }
    
    &.active {
      background-color: var(--color-primary-button);
    }
    
    &:first-child {
      border-radius: 8px 0 0 8px;
    }

    &:last-child {
      border-radius: 0 8px 8px 0;
    }
    
    &:not(:last-child) {
      border-right: 1px solid rgba(white, .05);
    }
  }
}
</style>