<template>
  <modal :open="open" @closed="close" :external-contents="true" title="Create instance" sub-title="Build your own Vanilla or Modded experience">
    <modal-body>
      <header>
        <div class="steps flex gap-4">
          <div class="step flex-1" :class="{active: step === 0}"><font-awesome-icon fixedWidth icon="info" /> About <span /></div>
          <div class="step flex-1" :class="{active: step === 1}"><font-awesome-icon fixedWidth icon="boxes" /> Mod Loader <span /></div>
          <div class="step flex-1" :class="{active: step === 2}"><font-awesome-icon fixedWidth icon="puzzle-piece" /> Settings <span /></div>
        </div>
      </header>
      <div class="about" v-show="step === 0">
        <artwork-selector class="mb-6" v-model="userSelectedArtwork" />
        <ftb-input label="Name" placeholder="Next best instance!" v-model="userPackName" class="mb-4" />
        <selection2 label="Minecraft version" class="mb-4" :options="vanillaVersions" v-model="userVanillaVersion" />
        <f-t-b-toggle :value="showVanillaSnapshots" @change="v => showVanillaSnapshots = v" label="Show snapshots" />
      </div>
      
      <div class="modloader" v-show="step === 1">
        <b>This step is optional</b>
        <p>A Mod Loader will allow you to load mods into Minecraft. Each Mod Loader will have different mods available so we recommend looking at <a>CurseForge</a> to see what's available.</p>

        <!-- TODO: Support a 'latest' flag -->
        
        <selection2 label="Minecraft version" class="mb-4" :options="vanillaVersions" v-model="userVanillaVersion" />
      </div>
      
      <div class="settings" v-show="step === 2">
        <f-t-b-toggle label="Fullscreen" />
        <selection2 :options="[]" label="Screen resolution" ></selection2>
        
        <f-t-b-slider label="Ram" />
      </div>
    </modal-body>
    
    <modal-footer>
      <div class="flex content-end">
        <ui-button v-if="step === 1" :disabled="stringIsEmpty(userPackName) || userVanillaVersion === -1" icon="arrow-right">Skip</ui-button>
        <ui-button :disabled="stringIsEmpty(userPackName) || userVanillaVersion === -1" icon="arrow-right">
          Next
        </ui-button>
      </div>
    </modal-footer>
  </modal>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue} from 'vue-property-decorator';
import ArtworkSelector from '@/components/core/modpack/ArtworkSelector.vue';
import Selection2, {SelectionOption} from '@/components/atoms/input/Selection2.vue';
import {ns} from '@/core/state/appState';
import {GetModpack} from '@/core/state/modpacks/modpacksState';
import { Action } from 'vuex-class';
import {ModPack} from '@/modules/modpacks/types';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import UiButton from '@/components/core/ui/UiButton.vue';
import {stringIsEmpty} from '@/utils/helpers/stringHelpers';
import {JavaFetch} from '@/core/javaFetch';
import FTBSlider from '@/components/atoms/input/FTBSlider.vue';

@Component({
  components: {FTBSlider, UiButton, FTBToggle, Selection2, ArtworkSelector},
  methods: {
    stringIsEmpty
  }
})
export default class CreateInstance extends Vue {
  @Action("getModpack", ns("v2/modpacks")) getModpack!: GetModpack;
  
  @Prop() open!: boolean;
  @Emit() close() {}
  
  step = 2;
  
  userSelectedArtwork: File | null = null;
  userPackName = "";
  userVanillaVersion = -1;
  
  vanillaPack: ModPack | null = null;
  showVanillaSnapshots = false;
  
  async mounted() {
    // Load the vanilla pack
    this.vanillaPack = await this.getModpack({
      id: 81,
      provider: "modpacksch"
    }) ?? null;

    const vanillaLatest = this.vanillaVersions[0];
    this.userPackName = `Minecraft ${vanillaLatest.label}`; 
    this.userVanillaVersion = vanillaLatest.value;
    
    // TODO: Only do this on next step change
    this.loadAvailableLoaders();
  }
  
  async loadAvailableLoaders() {
    const knownLoaders = ["forge", "neoforge", "fabric"];
    const foundLoadersForVersion = {} as Record<string, any>;

    const mcVersion = `1.19.2`
    for (const loader of knownLoaders) {
      const request = await JavaFetch.modpacksCh(`loaders/${mcVersion}/${loader}`).execute()
      if (request?.status !== "success") {
        continue;
      }
      
      const loaderData = request.json<any>();
      if (loaderData.total === 0) {
        continue;
      }
      
      foundLoadersForVersion[loader] = request.json();
    }
    
    console.log(foundLoadersForVersion);
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
}
</script>

<style lang="scss" scoped>

</style>