<template>
  <div class="flex flex-col">
    <h1 class="text-3xl font-bold">{{ packName }}</h1>
    <p class="mb-6">{{ packDescription }}</p>

    <selection
      @selected="e => (version = e.id)"
      placeholder="Selection version"
      :inheritedSelection="versionOptions[0]"
      :options="versionOptions"
    />

    <label class="inline-flex items-center mt-4">
      <input
        type="checkbox"
        :disabled="!hasReleaseVersion"
        class="h-5 w-5"
        v-model="showBetaAndAlpha"
        :checked="showBetaAndAlpha"
      /><span class="ml-4" :class="{ 'opacity-25': !hasReleaseVersion }">Show Beta and Alpha versions</span>
    </label>

    <FTBButton color="secondary" class="mt-8 mb-4 py-2 px-4 rounded text-center" @click="install">Install</FTBButton>
  </div>
</template>
<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import FTBButton from '@/components/FTBButton.vue';
import Selection from '@/components/elements/Selection.vue';
import { Versions } from '@/modules/modpacks/types';
import { getColorForReleaseType } from '@/utils/colors';

@Component({
  components: {
    Selection,
    FTBButton,
  },
  name: 'InstallModal',
  props: ['packName', 'packDescription', 'versions', 'doInstall', 'selectedVersion'],
})
export default class InstallModal extends Vue {
  @Prop() private versions!: Versions[];
  @Prop() private doInstall!: (version: number) => {};
  @Prop() private selectedVersion!: number | null;

  showBetaAndAlpha = false;

  private version: number =
    this.selectedVersion === null || this.selectedVersion === undefined ? this.versions[0].id : this.selectedVersion;

  public install(): void {
    this.doInstall(this.version);
  }

  get hasReleaseVersion() {
    return this.versions.findIndex(e => e.type.toLowerCase() === 'release') !== -1;
  }

  get versionsBasedOnBeta() {
    if (this.showBetaAndAlpha) {
      return this.versions;
    }

    const onlyReleaseVersions = this.versions.filter(e => e.type.toLowerCase() === 'release');
    return !onlyReleaseVersions.length ? this.versions : onlyReleaseVersions;
  }

  get versionOptions() {
    return this.versionsBasedOnBeta.map(e => ({
      value: e,
      text: this.$props.packName + ' ' + e.name,
      badge: { text: e.type[0].toUpperCase() + e.type.slice(1), color: getColorForReleaseType(e.type) },
    }));
  }
}
</script>
<style></style>
