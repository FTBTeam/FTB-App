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
      <ftb-modal size="large" v-if="currentModpack" :visible="showVersions" @dismiss-modal="showVersions = false">
        <modpack-versions
          :versions="currentModpack.versions"
          :pack-instance="currentModpack"
          :instance="null"
          :current="null"
        />
      </ftb-modal>
    </div>
    <ftb-modal :visible="showInstallBox" v-if="currentModpack" size="large-dynamic" @dismiss-modal="hideInstall">
      <InstallModal
        :pack-name="currentModpack.name"
        :doInstall="install"
        :pack-description="currentModpack.synopsis"
        :versions="currentModpack.versions"
        :selectedVersion="installSelectedVersion"
      />
    </ftb-modal>
  </div>
  <loading class="mt-20" v-else />
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import { ModPack, ModpackState, Versions } from '@/modules/modpacks/types';
import { Action, State } from 'vuex-class';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import MessageModal from '@/components/organisms/modals/MessageModal.vue';
import FTBModal from '@/components/atoms/FTBModal.vue';
import { createModpackchUrl, getPackArt, shuffle } from '../utils';
import { SettingsState } from '../modules/settings/types';
import { ServersState } from '@/modules/servers/types';
import ServerCard from '@/components/organisms/ServerCard.vue';
import InstallModal from '@/components/organisms/modals/InstallModal.vue';
import { PackConst } from '@/utils/contants';
import PackMetaHeading from '@/components/molecules/modpack/PackMetaHeading.vue';
import PackTitleHeader from '@/components/molecules/modpack/PackTitleHeader.vue';
import { ModpackPageTabs } from '@/views/InstancePage.vue';
import { AuthState } from '@/modules/auth/types';
import ModpackVersions from '@/components/templates/modpack/ModpackVersions.vue';
import PackBody from '@/components/molecules/modpack/PackBody.vue';
import { InstallerState } from '@/modules/app/appStore.types';
import Loading from '@/components/atoms/Loading.vue';

interface Changelogs {
  [id: number]: string;
}

@Component({
  name: 'ModpackPage',
  components: {
    Loading,
    ModpackVersions,
    PackTitleHeader,
    PackMetaHeading,
    'ftb-toggle': FTBToggle,
    InstallModal,
    'ftb-modal': FTBModal,
    'message-modal': MessageModal,
    ServerCard,
    PackBody,
  },
})
export default class ModpackPage extends Vue {
  activeTab: ModpackPageTabs = ModpackPageTabs.OVERVIEW;
  showVersions = false;

  @State('auth') public auth!: AuthState;
  @State('modpacks') public modpacks!: ModpackState;
  @State('settings') public settings!: SettingsState;
  @Action('fetchModpack', { namespace: 'modpacks' }) public fetchModpack!: any;
  @Action('fetchCursepack', { namespace: 'modpacks' }) public fetchCursepack!: any;
  @Action('storeInstalledPacks', { namespace: 'modpacks' }) public storePacks!: any;
  @Action('updateInstall', { namespace: 'modpacks' }) public updateInstall!: any;
  @Action('finishInstall', { namespace: 'modpacks' }) public finishInstall!: any;
  @Action('sendMessage') public sendMessage!: any;
  @Action('getChangelog', { namespace: 'modpacks' }) public getChangelog!: any;
  @State('servers') public serverListState!: ServersState;
  @Action('fetchServers', { namespace: 'servers' }) public fetchServers!: (projectid: string) => void;
  @Action('installModpack', { namespace: 'app' }) public installModpack!: (data: InstallerState) => void;

  private showInstallBox: boolean = false;
  private installSelectedVersion: number | null = null;
  private loading = true;
  private packType: number = 0;

  private activeChangelog: number | undefined = -1;
  private changelogs: Changelogs = [];

  mods: { version: string; size: string; name: string }[] = [];

  currentModpack: ModPack | null = null;
  error = '';

  public hideInstall(): void {
    this.showInstallBox = false;
  }

  public isTabActive(tabItem: string) {
    return this.activeTab === (tabItem as unknown as ModpackPageTabs);
  }

  public setActiveTab(tabItem: string) {
    this.activeTab = tabItem as unknown as ModpackPageTabs;
  }

  public install(version: number, versionName: string): void {
    this.installModpack({
      pack: {
        id: this.$route.query.modpackid as string,
        version: version,
        packType: this.$route.query.type as string,
        private: this.currentModpack?.private ?? false,
      },
      meta: {
        name: this.currentModpack?.name ?? '',
        version: versionName,
        art: getPackArt(this.currentModpack?.art),
      },
    });
    // if (this.showInstallBox) {
    //   this.showInstallBox = false;
    // }
    // this.$router.replace({
    //   name: 'installingpage',
    //   query: {
    //     modpackid: this.$route.query.modpackid,
    //     versionID: version.toString(),
    //     type: this.$route.query.type,
    //   },
    // });
  }

  async mounted() {
    const packID: number = parseInt(this.$route.query.modpackid as string, 10);
    this.packType = parseInt(this.$route.query.type as string, 10);

    let pack: ModPack;
    try {
      const packReq = await fetch(createModpackchUrl(`/${this.packType === 0 ? 'modpack' : 'curseforge'}/${packID}`));

      pack = await packReq.json();
      this.currentModpack = pack;
    } catch (error) {
      console.error(error);
      this.loading = false;
      this.error =
        "Unable to find this modpack, it's possible something has failed to load. Try again in a few minutes...";
      return;
    }

    if (this.$route.query.showInstall === 'true') {
      if (this.$route.query.version !== undefined) {
        this.installSelectedVersion = parseInt(this.$route.query.version as string, 10);
      }
      this.showInstallBox = true;
    }

    const res = await fetch(
      createModpackchUrl(
        `/${this.packType === 0 ? 'modpack' : 'curseforge'}/${packID}/${pack.versions[0].id}`,
        !pack.private && !pack.versions[0].private,
      ),
    );

    this.loading = false;

    const modsRaw = await res.json();
    this.mods = modsRaw.files
      ?.filter((e: any) => e.type === 'mod')
      .map((e: any) => ({ name: e.name, size: e.size, version: e.version }))
      .sort((a: any, b: any) =>
        a.name.toLowerCase() < b.name.toLowerCase() ? -1 : a.name.toLowerCase() > b.name.toLowerCase() ? 1 : 0,
      );

    this.toggleChangelog(this.currentModpack?.versions[0].id);
    if (this.currentVersionObject !== null) {
      if (this.currentVersionObject?.mtgID) {
        this.fetchServers(this.currentVersionObject?.mtgID ?? '');
      }
    }
  }

  get currentVersionObject(): Versions | null {
    if (this.currentModpack !== null) {
      const version = this.currentModpack?.versions.find((f: Versions) => f.id === this.latestRelease);
      if (version !== undefined) {
        return version;
      }
    }
    return null;
  }

  get shuffledServers() {
    if (this.currentVersionObject !== null && this.currentVersionObject.mtgID !== undefined) {
      return shuffle(this.serverListState.servers[this.currentVersionObject.mtgID]);
    }
    return [];
  }

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

  private async toggleChangelog(id: number | undefined) {
    if (typeof id === 'undefined') {
      return;
    }
    if (this.activeChangelog === id) {
      this.activeChangelog = -1;
      return;
    }
    if (!this.changelogs[id]) {
      const changelog = await this.getChangelog({
        packID: this.currentModpack?.id,
        versionID: id,
        type: this.packType,
      });
      this.changelogs[id] = changelog.content;
    }
    this.activeChangelog = id;
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
    if (this.currentModpack === null) {
      return PackConst.defaultPackSplashArt;
    }

    const splashArt = this.currentModpack.art?.filter((art) => art.type === 'splash');
    return splashArt?.length > 0 ? splashArt[0].url : PackConst.defaultPackSplashArt;
  }
}
</script>

<style lang="scss">
.header-image {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 200px;
  transition: all 0.2s ease-in-out;
}

.tab-pane {
  top: 0;
  height: 100%;
  overflow-y: auto;
}

.changelog-seperator {
  border: 1px solid var(--color-sidebar-item);
}

.short {
  width: 75%;
}

.instance-name {
  margin-top: auto;
  height: 45px;
  text-align: left;
  font-weight: 700;
  padding: 2px 2px 2px 6px;
}

.instance-info {
  bottom: 50px;
  text-align: left;
  font-weight: 400;
  padding: 2px 2px 2px 6px;
}

.instance-buttons {
  background: rgba(0, 0, 0, 0.7);
  width: 100%;
  height: 50px;
  text-align: left;
  font-weight: 700;
  padding: 2px 2px 2px 6px;
}

.instance-button {
  display: flex;
  justify-content: center;
  flex-direction: column;
  text-align: center;
}

.update-bar {
  background: rgba(255, 193, 7, 0.9);
  width: 100%;
  height: 25px;
  text-align: left;
  font-weight: 700;
  padding: 2px 2px 2px 6px;
  color: black;
}

.frosted-glass {
  backdrop-filter: blur(8px);
  background: linear-gradient(
    to top,
    rgba(36, 40, 47, 0) 0%,
    rgba(43, 57, 66, 0.2) calc(100% - 2px),
    rgba(193, 202, 207, 0.1) calc(100% - 1px),
    rgba(29, 29, 29, 0.3) 100%
  );
}
</style>
