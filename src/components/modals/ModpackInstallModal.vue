<template>
  <Modal :open="open" @closed="close" title="Install instance" :sub-title="packName" :external-contents="true">
    <ModalBody>
      <template v-if="apiModpack">
        <ArtworkSelector :pack="apiModpack" class="mb-6" v-model="userSelectedArtwork" />
        <FTBInput label="Name" :placeholder="packName" v-model="userPackName" class="mb-4" />
        
        <CategorySelector class="mb-4" v-model="selectedCategory" />

        <UiToggle label="Show advanced options" v-model="useAdvanced" />
        <Selection2 :open-up="true" v-if="useAdvanced" label="Version" :options="versions" v-model="selectedVersionId" class="mb-4 mt-6" />
        
        <UiToggle
          v-if="useAdvanced && hasUnstableVersions"
          v-model="allowPreRelease"
          label="Show pre-release builds (Stable by default)" 
          desc="Feeling risky? Enable pre-release builds to get access to Alpha and Beta versions of the Modpack"
        />
      </template>
    </ModalBody>
    <ModalFooter class="flex justify-end">
      <UiButton :wider="true" type="success" icon="download" @click="install">
        Install
      </UiButton>
    </ModalFooter>
  </Modal>
</template>

<script lang="ts" setup>
import {ModPack, PackProviders} from '@/modules/modpacks/types';
import {ns} from '@/core/state/appState';
import {GetModpack} from '@/core/state/modpacks/modpacksState';
import {timeFromNow} from '@/utils/helpers/dateHelpers';
import {getColorForReleaseType} from '@/utils';
import {toTitleCase} from '@/utils/helpers/stringHelpers';
import {isValidVersion} from '@/utils/helpers/packHelpers';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import platform from '@/utils/interface/electron-overwolf';
import {RouterNames} from '@/router';
import UiButton from '@/components/ui/UiButton.vue';
import CategorySelector from '@/components/groups/modpack/create/CategorySelector.vue';
import UiToggle from '@/components/ui/UiToggle.vue';
import ModalFooter from '@/components/ui/modal/ModalFooter.vue';
import Selection2 from '@/components/ui/Selection2.vue';
import FTBInput from '@/components/ui/input/FTBInput.vue';
import ArtworkSelector from '@/components/groups/modpack/components/ArtworkSelector.vue';
import ModalBody from '@/components/ui/modal/ModalBody.vue';

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

  allowPreRelease = false;
  useAdvanced = false;

  userPackName = "";
  userSelectedArtwork: File | null = null;
  
  @Watch("open")
  async onOpenChanged() {
    if (this.open && !this.apiModpack) {
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
      logo: this.userSelectedArtwork?.path ?? null, // The backend will default for us.
      private: this.apiModpack?.private ?? false,
      provider: this.provider,
    })
    
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
    return (this.apiModpack && this.apiModpack.versions) 
      ? [...this.apiModpack.versions].sort((a, b) => b.id - a.id) 
      : [];
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