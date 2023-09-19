<template>
  <modal :open="open" @closed="close" title="Install instance" :sub-title="packName" :external-contents="true">
    <modal-body>
      <template v-if="apiModpack">
        <artwork-selector :pack="apiModpack" class="mb-6" v-model="userSelectedArtwork" />
        <ftb-input label="Name" :placeholder="packName" v-model="userPackName" class="mb-4" />
        <div class="category-or-create mb-6">
          <div class="selection flex gap-2 items-end" v-if="!manualCategoryCreate">
            <selection2 label="Category" :options="(categories ?? []).map(e => ({value: e, label: e}))" v-model="selectedCategory" class="flex-1" />
            <ftb-button color="info" class="px-4 py-3" @click="manualCategoryCreate = true"><font-awesome-icon class="mr-2" icon="plus" />Create new</ftb-button>
          </div>
          <div class="selection flex gap-2 items-end" v-else>
            <ftb-input label="Category name" v-model="selectedCategory" placeholder="My category" class="flex-1 mb-0" />
            <ftb-button color="warning" class="px-4 py-3" @click="() => {
              selectedCategory = 'Default';
              manualCategoryCreate = false;
            }"><font-awesome-icon icon="times" /></ftb-button>
          </div>
        </div>
        <f-t-b-toggle label="Show advanced options" :value="useAdvanced" @change="v => useAdvanced = v" />
        <selection2 v-if="useAdvanced" label="Version" :options="versions" v-model="selectedVersionId" class="mb-4" />
        <f-t-b-toggle v-if="useAdvanced && hasUnstableVersions" label="Show pre-release builds (Stable by default)" :value="allowPreRelease" @change="v => allowPreRelease = v"  />
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
import {ModPack, PackProviders} from '@/modules/modpacks/types';
import {ns} from '@/core/state/appState';
import {Action, Getter } from 'vuex-class';
import {GetModpack} from '@/core/state/modpacks/modpacksState';
import Selection2 from '@/components/atoms/input/Selection2.vue';
import {timeFromNow} from '@/utils/helpers/dateHelpers';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import {getColorForReleaseType} from '@/utils';
import {toTitleCase} from '@/utils/helpers/stringHelpers';
import {isValidVersion, resolveArtwork} from '@/utils/helpers/packHelpers';
import ArtworkSelector from '@/components/core/modpack/ArtworkSelector.vue';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import platform from '@/utils/interface/electron-overwolf'
import {RouterNames} from '@/router';

@Component({
  components: {ArtworkSelector, FTBToggle, Selection2},
  methods: {
    resolveArtwork
  }
})
export default class ModpackInstallModal extends Vue {
  @Action("getModpack", ns("v2/modpacks")) getModpack!: GetModpack;
  @Getter("categories", ns("v2/instances")) categories!: string[];
  
  @Prop() open!: boolean;
  @Emit("close") close() {}
  
  @Prop() packId!: number;
  @Prop() uuid?: string;
  @Prop({default: "modpacksch" as PackProviders}) provider!: PackProviders; 
  
  apiModpack: ModPack | null = null;
  selectedVersionId = "";
  selectedCategory = "Default";
  manualCategoryCreate = false;

  allowPreRelease = false;
  useAdvanced = false;

  userPackName = "";
  userSelectedArtwork: File | null = null;
  
  @Watch("open")
  async onOpenChanged() {
    if (this.open && !this.apiModpack) {
      // TODO: Catch errors
      this.apiModpack = await this.getModpack({
        id: this.packId, 
        provider: this.provider
      });
      
      this.userPackName = this.apiModpack?.name ?? "";
      
      // No stable versions, default to pre-release
      if (!this.hasStableVersion) {
        this.allowPreRelease = true;
        this.useAdvanced = true;
      }
      
      this.selectedVersionId = this.restrictedVersions[0].id.toString() ?? "";
    }
  }

  install() {
    instanceInstallController.requestInstall({
      id: this.packId,
      category: this.selectedCategory,
      version: parseInt(this.selectedVersionId ?? this.sortedApiVersions[0].id),
      // Name fallback but it's not really needed
      name: this.userPackName ?? this.apiModpack?.name ?? "failed-to-name-the-modpack-somehow-" + platform.get.utils.crypto.randomUUID().split("-")[0],
      versionName: this.versions.find(e => e.value === this.selectedVersionId)?.label ?? "",
      logo: this.userSelectedArtwork?.path ?? null, // The backend will default for us.
      private: this.apiModpack?.private ?? false,
    })
    
    // TODO: Toast notification
    this.close();
    if (this.$route.name !== RouterNames.ROOT_LIBRARY) {
      this.$router.push({
        name: RouterNames.ROOT_LIBRARY
      })
    }
  }
  
  get packName() {
    return this.apiModpack?.name ?? "Loading...";
  }
  
  get versions() {
    let versions = this.restrictedVersions
    
    return versions
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
  
  get restrictedVersions() {
    let versions = this.sortedApiVersions;
    if (this.allowPreRelease) {
      versions = versions.filter(e => isValidVersion(e.type, "all"))
    } else {
      versions = versions.filter(e => isValidVersion(e.type, "release"))
    }

    return versions
  }
  
  get sortedApiVersions() {
    return this.apiModpack?.versions.sort((a, b) => b.id - a.id) ?? [];
  }
  
  get hasStableVersion() {
    return this.apiModpack?.versions
      .some(e => isValidVersion(e.type, "release")) ?? false
  }
  
  get hasUnstableVersions() {
    return this.apiModpack?.versions
      .some(e => isValidVersion(e.type, "alpha") || isValidVersion(e.type, "beta") || isValidVersion(e.type, "hotfix"))
  }
}
</script>

<style lang="scss" scoped>

</style>