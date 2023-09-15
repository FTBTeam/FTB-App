<template>
  <modal :open="open" @closed="close" title="Install instance" :sub-title="packName" :external-contents="true">
    <modal-body>
      <template v-if="apiModpack">
        <artwork-selector :pack="apiModpack" class="mb-6" v-model="userSelectedArtwork" />
        <ftb-input label="Name" :placeholder="packName" v-model="userPackName" class="mb-6" />
        <f-t-b-toggle label="Show advanced options" :value="useAdvanced" @change="v => useAdvanced = v" />
        <selection2 v-if="useAdvanced" label="Version" :options="versions" v-model="selectedVersionId" class="mb-4" />
        <f-t-b-toggle v-if="useAdvanced" label="Use pre-release builds (Latest by default)" :value="allowPreRelease" @change="v => allowPreRelease = v"  />
      </template>
    </modal-body>
    <modal-footer class="flex justify-end">
      <ftb-button color="primary" class="px-8 py-2" @click="install">
        <font-awesome-icon icon="download" class="mr-4" />
        <span>Install</span>
      </ftb-button>
    </modal-footer>
  </modal>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue, Watch} from 'vue-property-decorator';
import {SugaredInstanceJson} from '@/core/@types/javaApi';
import {ModPack} from '@/modules/modpacks/types';
import {ns} from '@/core/state/appState';
import {Action, Getter } from 'vuex-class';
import {GetModpack} from '@/core/state/modpacks/modpacksState';
import Selection2 from '@/components/atoms/input/Selection2.vue';
import {timeFromNow} from '@/utils/helpers/dateHelpers';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import {getColorForReleaseType} from '@/utils';
import {toTitleCase} from '@/utils/helpers/stringHelpers';
import {resolveArtwork} from '@/utils/helpers/packHelpers';
import ArtworkSelector from '@/components/core/modpack/ArtworkSelector.vue';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import platform from '@/utils/interface/electron-overwolf'

@Component({
  components: {ArtworkSelector, FTBToggle, Selection2},
  methods: {
    resolveArtwork
  }
})
export default class ModpackInstallModal extends Vue {
  @Action("getModpack", ns("v2/modpacks")) getModpack!: GetModpack;
  
  @Prop() open!: boolean;
  @Emit("close") close() {}
  
  @Prop() packId!: number;
  @Prop() uuid?: string;
  
  apiModpack: ModPack | null = null;
  selectedVersionId = "";

  allowPreRelease = false;
  useAdvanced = false;

  userPackName = "";
  userSelectedArtwork: File | null = null;
  
  @Watch("open")
  async onOpenChanged() {
    if (this.open && !this.apiModpack) {
      // TODO: Catch errors
      this.apiModpack = await this.getModpack(this.packId);
      this.selectedVersionId = this.apiModpack?.versions[0].id.toString() ?? "";
      this.userPackName = this.apiModpack?.name ?? "";
    }
  }

  install() {
    instanceInstallController.requestInstall({
      id: this.packId,
      version: parseInt(this.selectedVersionId),
      // Name fallback but it's not really needed
      name: this.userPackName ?? this.apiModpack?.name ?? "failed-to-name-the-modpack-somehow-" + platform.get.utils.crypto.randomUUID().split("-")[0],
      versionName: this.versions.find(e => e.value === this.selectedVersionId)?.label ?? "",
      logo: this.userSelectedArtwork?.path ?? resolveArtwork(this.apiModpack, "square"),
    })
    
    // TODO: Toast notification
    this.close();
  }
  
  get packName() {
    return this.apiModpack?.name ?? "Loading...";
  }
  
  get versions() {
    return (this.apiModpack?.versions ?? [])
      .sort((a, b) => b.id - a.id)
      .map(e => ({
        value: e.id.toString(),
        label: e.name,
        meta: timeFromNow(e.updated),
        badge: {
          color: getColorForReleaseType(e.type),
          text: toTitleCase(e.type)
        }
      }))
  }
}
</script>

<style lang="scss" scoped>

</style>