<template>
  <div class="pack-container" v-if="!loading">
    <div class="pack-page">
      <div v-if="!currentModpack && !loading">
        <message icon="warning" type="danger" class="m-6">
          {{ error ? error : 'Something has gone wrong...' }}
        </message>
      </div>
      <div class="pack-page-contents" v-if="currentModpack">
        <header>
          <div
            class="background"
            :style="{
            'background-image': `url(${packSplashArt})`,
          }"
          ></div>
          
          <pack-meta-heading
            @back="$router.back()"
            :hidePackDetails="false"
            versionType="release"
            :api-pack="currentModpack"
          />

          <pack-title-header :pack-instance="currentModpack" :pack-name="currentModpack.name" />
        </header>

        <div class="body">
          <pack-body
            @mainAction="showInstallBox = true"
            @update="() => {}"
            @tabChange="(e) => (activeTab = e)"
            @showVersion="showVersions = true"
            :searchingForMods="false"
            :active-tab="activeTab"
            :isInstalled="false"
            :instance="null"
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
  <loader class="mt-20" v-else />
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {ModPack, PackProviders} from '@/modules/modpacks/types';
import {Action} from 'vuex-class';
import PackMetaHeading from '@/components/molecules/modpack/PackMetaHeading.vue';
import PackTitleHeader from '@/components/molecules/modpack/PackTitleHeader.vue';
import {ModpackPageTabs} from '@/views/InstancePage.vue';
import ModpackVersions from '@/components/templates/modpack/ModpackVersions.vue';
import PackBody from '@/components/molecules/modpack/PackBody.vue';
import ClosablePanel from '@/components/molecules/ClosablePanel.vue';
import ModpackInstallModal from '@/components/core/modpack/modals/ModpackInstallModal.vue';
import {resolveArtwork, typeIdToProvider} from '@/utils/helpers/packHelpers';
import {ns} from '@/core/state/appState';
import {GetModpack, GetModpackVersion} from '@/core/state/modpacks/modpacksState';
import Loader from '@/components/atoms/Loader.vue';
import {createLogger} from '@/core/logger';

@Component({
  name: 'ModpackPage',
  components: {
    Loader,
    ModpackInstallModal,
    ClosablePanel,
    ModpackVersions,
    PackTitleHeader,
    PackMetaHeading,
    PackBody,
  },
})
export default class ModpackPage extends Vue {
  @Action("getModpack", ns("v2/modpacks")) getModpack!: GetModpack;
  @Action("getVersion", ns("v2/modpacks")) getVersion!: GetModpackVersion;
  
  private logger = createLogger("ModpackPage.vue")
  
  activeTab: ModpackPageTabs = ModpackPageTabs.OVERVIEW;
  showVersions = false;

  showInstallBox: boolean = false;
  loading = true;
  packTypeId: number = 0;
  
  currentModpack: ModPack | null = null;
  error = '';

  async mounted() {
    const packID: number = parseInt(this.$route.query.modpackid as string, 10);
    this.packTypeId = parseInt(this.$route.query.type as string, 10);
    
    try {
      this.logger.debug("Loading modpack", packID, this.packTypeId)
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
      this.logger.error("Failed to load modpack", error)
      this.loading = false;
      this.error =
        "Unable to find this modpack, it's possible something has failed to load. Try again in a few minutes...";
      return;
    }

    if (this.$route.query.showInstall === 'true') {
      this.logger.debug("Showing install box")
      this.showInstallBox = true;
    }

    this.loading = false;
  }
  

  get packSplashArt() {
    return resolveArtwork(this.currentModpack, 'splash');
  }
  
  get packType(): PackProviders {
    return typeIdToProvider(this.packTypeId)
  }
}
</script>