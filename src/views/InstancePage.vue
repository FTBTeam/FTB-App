<template>
  <div class="pack-page">
    <div class="pack-page-contents" v-if="instance">
      <div
        class="background"
        :style="{
          'background-image': `url(${packSplashArt})`,
        }"
      ></div>
      <header>
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
          @searchForMods="searchingForMods = true"
          @getModList="(e) => getModList(e)"
          @backupsChanged="loadBackups"
          :pack-loading="packLoading"
          :active-tab="activeTab"
          :isInstalled="true"
          :instance="instance"
          :mods="modlist"
          :pack-instance="apiPack"
          :updating-mod-list="updatingModlist"
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
      :subTitle="
        $route.query.presentOffline
          ? 'Would you like to play in Offline Mode?'
          : 'Playing in offline mode will prevent you from being able to join servers.'
      "
      @closed="() => closeOfflineModel()"
    >
      <p v-if="$route.query.presentOffline" class="mb-4">
        Please be aware, running in offline mode will mean you can not play on servers.
      </p>

      <ftb-input placeholder="Steve" label="Username" v-model="offlineUserName" class="mb-4 text-base" />

      <div class="flex justify-end">
        <ftb-button color="primary" class="py-2 px-6 mt-2 inline-block" @click="playOffline">
          <font-awesome-icon icon="play" class="mr-2" size="1x" />
          Play offline
        </ftb-button>
      </div>
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

    <closable-panel
      :open="searchingForMods"
      @close="searchingForMods = false"
      :title="`Add mods to ${apiPack ? apiPack.name : ''}`"
      subtitle="You can find mods for this pack using the search area below"
    >
      <find-mods :instance="instance" @modInstalled="getModList" />
    </closable-panel>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {ModPack, Versions} from '@/modules/modpacks/types';
import {Action, Getter, State} from 'vuex-class';
import {AuthState} from '@/modules/auth/types';
import FindMods from '@/components/templates/modpack/FindMods.vue';
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
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {alertController} from '@/core/controllers/alertController';
import {dialogsController} from '@/core/controllers/dialogsController';
import {modpackApi} from '@/core/pack-api/modpackApi';

export enum ModpackPageTabs {
  OVERVIEW,
  MODS,
  SETTINGS,
  BACKUPS,
}

@Component({
  name: 'InstancePage',
  components: {
    VersionsBorkedModal,
    ClosablePanel,
    PackTitleHeader,
    PackMetaHeading,
    ModpackSettings,
    ModpackVersions,
    FindMods,
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

  private apiPack: ModPack | null = null;
  deleting: boolean = false;

  modlist: any = [];

  searchingForMods = false;
  updatingModlist = false;
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
    
    // TODO: Allow to work without this.
    this.apiPack = await this.getModpack({
      id: this.instance.id,
      provider: typeIdToProvider(this.instance.packType)
    });
    
    if (!this.apiPack) {
      this.activeTab = ModpackPageTabs.MODS;
    }

    this.getModList();
    this.loadBackups().catch(console.error);

    // Throwaway error, don't block
    this.checkForBorkedVersion().catch(console.error);

    this.getModList();
    this.loadBackups().catch(console.error);

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
      this.searchingForMods = false;
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
      
      // TODO: ([ipc]#1) Handle errors
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

  private async getModList(showAlert = false) {
    const mods = await toggleBeforeAndAfter(() => sendMessage("instanceMods", {
      uuid: this.instance?.uuid ?? "",
      _private: this.instance?._private ?? false,
    }), state => this.updatingModlist = state)

    // TODO: Catch errors
    this.modlist = mods.files;
    
    if (showAlert) {
      alertController.success('The mods list has been updated')
    }
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
