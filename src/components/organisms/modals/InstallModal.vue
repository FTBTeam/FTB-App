<template>
  <div class="flex flex-col">
    <h1 class="select-text text-2xl mt-2 mb-4 font-bold">{{ packName }}</h1>
    <p class="select-text mb-6 leading-6">{{ packDescription }}</p>

    <selection
      @selected="(e) => (version = !e ? null : e.id)"
      placeholder="Selection version"
      :inheritedSelection="versionOptions[0]"
      :options="versionOptions"
    />

    <div class="flex justify-between items-center">
      <label class="inline-flex items-center mt-4 cursor-pointer">
        <input
          type="checkbox"
          :disabled="!hasReleaseVersion"
          class="h-5 w-5"
          v-model="showBetaAndAlpha"
          :checked="showBetaAndAlpha"
        /><span class="ml-4" :class="{ 'opacity-25': !hasReleaseVersion }">Show unstable versions</span>
      </label>

      <FTBButton
        color="secondary"
        :disabled="!version"
        class="mt-8 mb-4 py-2 px-4 w-1/3 rounded text-center"
        @click="install"
        >Install</FTBButton
      >
    </div>
  </div>
</template>
<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import FTBButton from '@/components/atoms/input/FTBButton.vue';
import Selection from '@/components/atoms/input/Selection.vue';
import { Versions } from '@/modules/modpacks/types';
import { getColorForReleaseType } from '@/utils/colors';

@Component({
  components: {
    Selection,
    FTBButton,
  },
  name: 'InstallModal',
})
export default class InstallModal extends Vue {
  @Prop() packName!: string;
  @Prop() packDescription!: string;
  @Prop() versions!: Versions[];
  @Prop() doInstall!: (version: number, versionName?: string) => {};
  @Prop() selectedVersion!: number | null;

  showBetaAndAlpha = false;

  private version: number | null =
    this.selectedVersion === null || this.selectedVersion === undefined ? this.versions[0].id : this.selectedVersion;

  public install(): void {
    if (this.version === null) {
      return;
    }

    this.doInstall(this.version, this.versions.find((e) => e.id === this.version)?.name);
  }

  get hasReleaseVersion() {
    return this.versions.findIndex((e) => e.type.toLowerCase() === 'release') !== -1;
  }

  get versionsBasedOnBeta() {
    if (this.showBetaAndAlpha) {
      return this.versions;
    }

    const onlyReleaseVersions = this.versions.filter((e) => e.type.toLowerCase() === 'release');
    return !onlyReleaseVersions.length ? this.versions : onlyReleaseVersions;
  }

  get versionOptions() {
    return this.versionsBasedOnBeta.map((e) => ({
      value: e,
      text: this.$props.packName + ' ' + e.name,
      badge: { text: e.type[0].toUpperCase() + e.type.slice(1), color: getColorForReleaseType(e.type) },
    }));
  }
}
</script>
<style></style>
