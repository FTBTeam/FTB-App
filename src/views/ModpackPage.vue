<template>
  <div class="pack-container" v-if="!loading">
    <div class="pack-page">
      <div v-if="!currentModpack && !loading">
        <message icon="warning" type="danger" class="m-6">
          {{ error ? error : 'Something has gone wrong...' }}
        </message>
      </div>
      <div class="pack-page-contents" v-if="currentModpack">
        <div
          class="background"
          :style="{
            'background-image': `url(${packSplashArt})`,
          }"
        ></div>
        <header>
          <pack-meta-heading
            @back="$router.back()"
            :hidePackDetails="false"
            versionType="release"
            :instance="currentModpack"
            :isForgePack="isForgePack"
          />

          <pack-title-header :pack-instance="currentModpack" :pack-name="currentModpack.name" />
        </header>

        <div class="body">
          <pack-body
            @mainAction="showInstallBox = true"
            @update="() => {}"
            @getModList="() => {}"
            @searchForMods="() => {}"
            @tabChange="(e) => (activeTab = e)"
            @showVersion="showVersions = true"
            :searchingForMods="false"
            :active-tab="activeTab"
            :isInstalled="false"
            :instance="null"
            :mods="mods"
            :pack-instance="currentModpack"
            :updating-mod-list="false"
          />
        </div>
      </div>
      <closable-panel :open="showVersions" @close="showVersions = false" title="Versions" subtitle="Upgrade or downgrade your pack version">
        <modpack-versions
          :versions="currentModpack.versions"
          :pack-instance="currentModpack"
          :instance="null"
          :current="null"
          @close="showVersions = false"
        />
      </closable-panel>
    </div>
    
    <modpack-install-modal v-if="currentModpack" :pack-id="currentModpack.id" :provider="packType" :open="showInstallBox" @close="showInstallBox = false" />
  </div>
  <loading2 class="mt-20" v-else />
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import {ModPack, PackProviders, Versions} from '@/modules/modpacks/types';
import { Action } from 'vuex-class';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import FTBModal from '@/components/atoms/FTBModal.vue';
import PackMetaHeading from '@/components/molecules/modpack/PackMetaHeading.vue';
import PackTitleHeader from '@/components/molecules/modpack/PackTitleHeader.vue';
import { ModpackPageTabs } from '@/views/InstancePage.vue';
import ModpackVersions from '@/components/templates/modpack/ModpackVersions.vue';
import PackBody from '@/components/molecules/modpack/PackBody.vue';
import ClosablePanel from '@/components/molecules/ClosablePanel.vue';
import ModpackInstallModal from '@/components/core/modpack/ModpackInstallModal.vue';
import {resolveArtwork, typeIdToProvider} from '@/utils/helpers/packHelpers';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {RouterNames} from '@/router';
import {ns} from '@/core/state/appState';
import {GetModpack, GetModpackVersion} from '@/core/state/modpacks/modpacksState';
import Loading2 from '@/components/atoms/Loading2.vue';

@Component({
  name: 'ModpackPage',
  components: {
    ModpackInstallModal,
    ClosablePanel,
    Loading2,
    ModpackVersions,
    PackTitleHeader,
    PackMetaHeading,
    'ftb-toggle': FTBToggle,
    'ftb-modal': FTBModal,
    PackBody,
  },
})
export default class ModpackPage extends Vue {
  @Action("getModpack", ns("v2/modpacks")) getModpack!: GetModpack;
  @Action("getVersion", ns("v2/modpacks")) getVersion!: GetModpackVersion;
  
  activeTab: ModpackPageTabs = ModpackPageTabs.OVERVIEW;
  showVersions = false;

  showInstallBox: boolean = false;
  loading = true;
  packTypeId: number = 0;

  mods: { version: string; size: string; name: string }[] = [];

  currentModpack: ModPack | null = null;
  error = '';

  public install(name: string, version: number, versionName: string): void {
    instanceInstallController.requestInstall({
      id: this.currentModpack?.id ?? 0,
      version: version,
      name: name,
      versionName: versionName,
      logo: resolveArtwork(this.currentModpack ?? null, 'square'),
      private: this.currentModpack?.private ?? false,
    })

    this.$router.push({
      name: RouterNames.ROOT_LIBRARY
    })
  }

  async mounted() {
    const packID: number = parseInt(this.$route.query.modpackid as string, 10);
    this.packTypeId = parseInt(this.$route.query.type as string, 10);

    let pack: ModPack;
    try {
      const modpack = await this.getModpack({
        id: packID,
        provider: typeIdToProvider(this.packTypeId)
      })
      
      if (modpack) {
        const copyModpack = Object.assign({}, modpack);

        if (copyModpack.versions) {
          copyModpack.versions = copyModpack.versions.sort((a, b) => b.id - a.id)
        }
        this.currentModpack = copyModpack;
      }
    } catch (error) {
      console.error(error);
      this.loading = false;
      this.error =
        "Unable to find this modpack, it's possible something has failed to load. Try again in a few minutes...";
      return;
    }

    if (this.$route.query.showInstall === 'true') {
      this.showInstallBox = true;
    }

    const version = await this.getVersion({
      id: packID,
      versionId: this.currentModpack?.versions[0].id ?? 0,
      provider: typeIdToProvider(this.packTypeId)
    })

    this.loading = false;
    
    if (version) {
      console.log(version.files.filter(e => !e.name))
      this.mods = version.files
        ?.filter((e: any) => e.type === 'mod')
        .map((e: any) => ({name: e.name, size: e.size, version: e.version}))
        .sort((a: any, b: any) =>
          a.name.toLowerCase() < b.name.toLowerCase() ? -1 : a.name.toLowerCase() > b.name.toLowerCase() ? 1 : 0,
        );
    }
    
    // if (this.currentVersionObject !== null) {
    //   if (this.currentVersionObject?.mtgID) {
    //     this.fetchServers(this.currentVersionObject?.mtgID ?? '');
    //   }
    // }
  }

  // get currentVersionObject(): Versions | null {
  //   if (this.currentModpack !== null) {
  //     const version = this.currentModpack?.versions.find((f: Versions) => f.id === this.latestRelease);
  //     if (version !== undefined) {
  //       return version;
  //     }
  //   }
  //   return null;
  // }
  
  get latestRelease() {
    if (this.currentModpack !== undefined) {
      const version = this.currentModpack?.versions.find((f: Versions) => f.type.toLowerCase() === 'release');
      if (version !== undefined) {
        return version.id;
      }
      return null;
    }
    return null;
  }

  get isForgePack() {
    const latestRelease = this.latestRelease ?? this.currentModpack?.versions[0]?.id ?? null;
    return (
      (this.currentModpack?.versions?.find((e) => e.id === latestRelease) as any)?.targets.find(
        (e: any) => e.type === 'modloader',
      )?.name === 'forge'
    );
  }

  get packSplashArt() {
    return resolveArtwork(this.currentModpack, 'splash');
  }
  
  get packType(): PackProviders {
    return typeIdToProvider(this.packTypeId)
  }
}
</script>