<template>
  <div class="pack-page">
    <div class="pack-page-contents" v-if="instance">
      <header>
        <div
          class="background"
          :style="{
            'background-image': `url(${packSplashArt})`,
          }"
        />
        <pack-meta-heading
          @back="goBack"
          :hidePackDetails="hidePackDetails"
          :versionType="versionType"
          :api-pack="apiPack ? apiPack : undefined"
          :instance="instance"
        />

        <pack-title-header
          v-if="!hidePackDetails"
          :pack-instance="apiPack"
          :instance="instance"
          :isInstalled="true"
          :pack-name="instance.name"
        />
      </header>

      <div class="body" :class="{ 'settings-open': activeTab === tabs.SETTINGS }">
        <pack-body
          @mainAction="launchModPack()"
          @update="update"
          @tabChange="(e) => (activeTab = e)"
          @showVersion="showVersions = true"
          @backupsChanged="loadBackups"
          :pack-loading="packLoading"
          :active-tab="activeTab"
          :isInstalled="true"
          :instance="instance"
          :pack-instance="apiPack"
          :backups="instanceBackups"
          :allow-offline="offlineAllowed"
          @playOffline="offlineMessageOpen = true"
        />
      </div>

      <closable-panel :open="showVersions" @close="showVersions = false" title="Versions" subtitle="Upgrade or downgrade your pack version">
        <modpack-versions
          v-if="apiPack"
          :versions="apiPack.versions"
          :pack-instance="apiPack"
          :instance="instance"
          @close="showVersions = false"
        />
      </closable-panel>
    </div>
    <p v-else>No modpack found...</p>
    
    <modal
      :open="offlineMessageOpen"
      :title="$route.query.presentOffline ? 'Unable to update your profile' : 'Play offline'"
      subTitle="Would you like to play in Offline Mode?"
      @closed="() => closeOfflineModel()"
    >
      <message type="warning" class="mb-6 wysiwyg">
        <p>Please be aware, running in offline mode will mean you can not:</p>
        <ul>
          <li>Play on online servers</li>
          <li>Have a custom skin</li>
          <li>Have any profile specific content in-game</li>
        </ul> 
      </message>

      <ftb-input placeholder="Steve" label="Username" v-model="offlineUserName" class="text-base" />

      <template #footer>
        <div class="flex justify-end">
          <ui-button icon="play" type="success" @click="playOffline">Play offline</ui-button>
        </div>
      </template>
    </modal>

    <modal
      :open="borkedVersionNotification != null"
      @closed="closeBorked"
      title="Version archived"
      sub-title="You will need to update or downgrade the pack"
      size="medium"
      :external-contents="true"
    >
      <versions-borked-modal
        @closed="closeBorked"
        @action="(e) => updateOrDowngrade(e)"
        :is-downgrade="borkedVersionIsDowngrade"
        :fixed-version="borkedVersionDowngradeId"
        :notification="borkedVersionNotification"
      />
    </modal>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {ModPack, Versions} from '@/modules/modpacks/types';
import {Action, Getter, State} from 'vuex-class';
import {AuthState} from '@/modules/auth/types';
import ModpackVersions from '@/components/templates/modpack/ModpackVersions.vue';
import ModpackSettings from '@/components/templates/modpack/ModpackSettings.vue';
import PackMetaHeading from '@/components/molecules/modpack/PackMetaHeading.vue';
import PackTitleHeader from '@/components/molecules/modpack/PackTitleHeader.vue';
import PackBody from '@/components/molecules/modpack/PackBody.vue';
import {AuthProfile} from '@/modules/core/core.types';
import {RouterNames} from '@/router';
import ClosablePanel from '@/components/molecules/ClosablePanel.vue';
import VersionsBorkedModal from '@/components/organisms/modals/VersionsBorkedModal.vue';
import {ns} from '@/core/state/appState';
import {Backup, SugaredInstanceJson} from '@/core/@types/javaApi';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {GetModpack} from '@/core/state/modpacks/modpacksState';
import {resolveArtwork, typeIdToProvider} from '@/utils/helpers/packHelpers';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {alertController} from '@/core/controllers/alertController';
import {dialogsController} from '@/core/controllers/dialogsController';
import {modpackApi} from '@/core/pack-api/modpackApi';
import UiButton from '@/components/core/ui/UiButton.vue';
import {consoleBadButNoLogger} from '@/utils';

export enum ModpackPageTabs {
  OVERVIEW,
  MODS,
  SETTINGS,
  BACKUPS,
}

@Component({
  name: 'InstancePage',
  components: {
    UiButton,
    VersionsBorkedModal,
    ClosablePanel,
    PackTitleHeader,
    PackMetaHeading,
    ModpackSettings,
    ModpackVersions,
    PackBody,
  },
})
export default class InstancePage extends Vue {
  @Getter('instances', ns("v2/instances")) public instances!: SugaredInstanceJson[];
  @Action("getModpack", ns("v2/modpacks")) getModpack!: GetModpack;
  
  @State('auth') public auth!: AuthState;

  @Getter('getProfiles', { namespace: 'core' }) public authProfiles!: AuthProfile[];
  @Getter('getActiveProfile', { namespace: 'core' }) private getActiveProfile!: any;

  @Action('openSignIn', { namespace: 'core' }) public openSignIn: any;
  @Action('startInstanceLoading', { namespace: 'core' }) public startInstanceLoading: any;
  @Action('stopInstanceLoading', { namespace: 'core' }) public stopInstanceLoading: any;

  packLoading = false;

  // New stuff
  tabs = ModpackPageTabs;
  activeTab: ModpackPageTabs = ModpackPageTabs.OVERVIEW;

  apiPack: ModPack | null = null;
  
  showVersions = false;
  offlineMessageOpen = false;
  offlineUserName = '';
  offlineAllowed = false;

  instanceBackups: Backup[] = [];

  borkedVersionNotification: string | null = null;
  borkedVersionDowngradeId: number | null = null;
  borkedVersionIsDowngrade = false;

  async mounted() {
    if (this.instance == null) {
      await this.$router.push(RouterNames.ROOT_LIBRARY);
      return;
    }
    
    // TODO: (M#01) Allow to work without this.
    this.apiPack = await this.getModpack({
      id: this.instance.id,
      provider: typeIdToProvider(this.instance.packType)
    });
    
    if (!this.apiPack) {
      this.activeTab = ModpackPageTabs.MODS;
    }

    this.loadBackups().catch(e => consoleBadButNoLogger("E", e))

    // Throwaway error, don't block
    this.checkForBorkedVersion().catch(e => consoleBadButNoLogger("E", e))

    if (this.getActiveProfile) {
      this.offlineAllowed = true;
    }

    if (this.$route.query.presentOffline) {
      this.offlineMessageOpen = true;
    }
    this.offlineUserName = this.getActiveProfile?.username;
  }

  /**
   * Tries to fetch the current version data and warn the user if the version should be downgraded. Will only request
   * if the held version is set to `archived`
   */
  private async checkForBorkedVersion() {
    if (!this.instance?.versionId || !this.apiPack) {
      return;
    }

    const currentVersion = this.apiPack.versions.find((e) => e.id === this.instance?.versionId);
    if (!currentVersion || currentVersion.type.toLowerCase() !== 'archived') {
      return;
    }

    const apiData = await modpackApi.modpacks.getModpackVersion(this.instance.id, this.instance.versionId, typeIdToProvider(this.instance.packType))
    if (!apiData) {
      return;
    }
    
    if (apiData.notification) {
      this.borkedVersionNotification = apiData.notification;
    }

    // Find a downgrade / upgrade version
    const nextAvailableVersion = this.apiPack.versions.find((e) => e.type !== 'archived');
    if (nextAvailableVersion) {
      this.borkedVersionDowngradeId = nextAvailableVersion.id;
      if (nextAvailableVersion.id < this.instance.versionId) {
        this.borkedVersionIsDowngrade = true;
      }
    }
  }

  closeBorked() {
    this.borkedVersionNotification = null;
    this.borkedVersionDowngradeId = null;
    this.borkedVersionIsDowngrade = false;
  }

  public goBack(): void {
    if (!this.hidePackDetails) {
      this.$router.push({ name: RouterNames.ROOT_LIBRARY });
    } else {
      this.activeTab = this.apiPack ? ModpackPageTabs.OVERVIEW : ModpackPageTabs.MODS;
    }
  }

  public async launchModPack() {
    if (this.instance === null) {
      return;
    }
    
    if (this.instance.pendingCloudInstance) {
      const result = await sendMessage("syncInstance", {
        uuid: this.instance.uuid
      })
      
      // TODO: (M#01) Handle errors
      // TODO: (M#01) Handle some kinda visual state
      console.log(result)
      
      return;
    }

    if (this.instance.memory < this.instance.minMemory) {
      const result = await dialogsController.createConfirmationDialog("Low memory",
        `You are trying to launch the modpack with memory settings that are below the` +
        `minimum required.This may cause the modpack to not start or crash frequently.<br>We recommend that you` +
        `increase the assigned memory to at least **${this.instance?.minMemory}MB**\n\nYou can change the memory by going to the settings tab of the modpack and adjusting the memory slider`
      )
      
      if (!result) {
        return
      }
    }
    
    this.launch();
  }

  public launch() {
    this.$router.push({
      name: RouterNames.ROOT_LAUNCH_PACK,
      query: { uuid: this.instance?.uuid },
    });
  }

  public playOffline() {
    this.$router.push({
      name: RouterNames.ROOT_LAUNCH_PACK,
      query: { uuid: this.instance?.uuid, offline: 'true', username: this.offlineUserName },
    });
  }

  public update(version: Versions | null = null): void {
    const targetVersion = version ?? this.apiPack?.versions.sort((a, b) => b.id - a.id)[0];
    if (!targetVersion || !this.instance) {
      // How?
      return;
    }
    
    instanceInstallController.requestUpdate(this.instance, targetVersion, typeIdToProvider(this.instance.packType))
  }

  public updateOrDowngrade(versionId: number) {
    const pack = this.apiPack?.versions.find((e) => e.id === versionId);
    if (!pack) {
      alertController.error('The selected recovery pack version id was not available...')
      return;
    }

    // update
    this.update(pack);
    this.closeBorked();
  }

  public async loadBackups() {
    const backups = await sendMessage("instanceGetBackups", {
      uuid: this.instance?.uuid ?? '',
    })

    this.instanceBackups = backups.backups.sort((a, b) => b.createTime - a.createTime);
  }

  closeOfflineModel() {
    this.offlineMessageOpen = false;
    if (this.$route.query.presentOffline) {
      this.$router.push({ name: RouterNames.ROOT_LOCAL_PACK, query: { uuid: this.instance?.uuid } });
    }
  }
  
  get instance() {
    return this.instances.find(e => e.uuid === this.$route.params.uuid) ?? null;
  }

  get packSplashArt() {
    return resolveArtwork(this.apiPack, 'splash');
  }

  /**
   * Determines if we need to hide the bulk of the page
   */
  get hidePackDetails() {
    return this.activeTab === ModpackPageTabs.SETTINGS;
  }

  get versionType() {
    return this.apiPack?.versions?.find((e) => e.id === this.instance?.versionId)?.type.toLowerCase() ?? 'release';
  }
}
</script>
