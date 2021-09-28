<template>
  <div class="pack-container">
    <div class="pack-page">
      <div
        class="pack-page-contents"
        v-if="!loading && currentModpack"
        :style="{
          'background-image': `url(${packSplashArt})`,
        }"
      >
        <header>
          <!--          TODO: fix isForgePack-->
          <pack-meta-heading
            @back="$router.back()"
            :hidePackDetails="false"
            versionType="release"
            :instance="currentModpack"
            :isForgePack="true"
          />

          <pack-title-header :pack-instance="currentModpack" :pack-name="currentModpack.name" />
        </header>

        <div class="body">
          <!--          TODO: fix mod list-->
          <pack-tabs-body
            @mainAction="showInstallBox = true"
            @update="() => {}"
            @getModList="() => {}"
            @searchForMods="() => {}"
            @tabChange="e => (activeTab = e)"
            @showVersion="showVersions = true"
            :searchingForMods="false"
            :active-tab="activeTab"
            :isInstalled="false"
            :instance="null"
            :mods="mods"
            :pack-instance="currentModpack"
            :isLatestVersion="true"
            :updating-mod-list="false"
          />
        </div>
      </div>
      <ftb-modal :isLarge="true" :visible="showVersions" @dismiss-modal="showVersions = false">
        <modpack-versions
          :versions="currentModpack.versions"
          :pack-instance="currentModpack"
          :instance="null"
          :current="null"
        />
      </ftb-modal>
    </div>
    <div class="flex flex-1 flex-col h-full overflow-hidden">
      <div class="flex flex-col h-full" v-if="!loading && currentModpack !== null" key="main-window">
        <div>
          <div
            class="header-image"
            v-bind:style="{
              'background-image': `url(${
                currentModpack.art.filter(art => art.type === 'splash').length > 0
                  ? currentModpack.art.filter(art => art.type === 'splash')[0].url
                  : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png'
              })`,
            }"
          >
            <span class="instance-name"
              ><ftb-button
                class=""
                color=""
                css-class="text-center backbtn py-2 rounded"
                :disabled="modpacks.installing !== null"
                @click="$router.back()"
                ><font-awesome-icon icon="arrow-left" size="1x"/></ftb-button
            ></span>

            <span class="instance-name text-4xl">{{ currentModpack.name }}</span>
            <span class="instance-info">
              <small>
                <em>{{ currentModpack.synopsis }}</em>
              </small>
              <div v-if="currentModpack.tags" class="flex flex-row items-center">
                <div class="flex flex-row">
                  <span
                    v-if="currentModpack.tags.length > 5"
                    :key="`tag-more`"
                    class="rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light"
                    style="font-variant: small-caps;"
                    >+{{ currentModpack.tags.length - 5 }}</span
                  >
                </div>
              </div>
            </span>
            <div class="update-bar" v-if="currentModpack && currentModpack.notification">
              {{ currentModpack.notification }}
            </div>
            <div class="instance-buttons flex flex-row frosted-glass">
              <div class="instance-button mr-1">
                <ftb-button
                  class="py-2 px-4"
                  color="primary"
                  css-class="text-center text-l"
                  @click="install(currentModpack.versions[0].id)"
                  :disabled="modpacks.installing !== null"
                >
                  <font-awesome-icon icon="download" size="1x" />
                  Install
                </ftb-button>
                <!--              <button-->
                <!--                  class="bg-green-500 hover:bg-green-400 text-white-600 font-bold py-2 px-4 inline-flex items-center"-->
                <!--                  @click="install(currentModpack.versions[0].id)"-->
                <!--              >-->
                <!--                <span class="cursor-pointer"><font-awesome-icon icon="download" size="1x"/>&nbsp;Install</span>-->
                <!--              </button>-->
              </div>
              <div class="instance-button mr-1">
                <div class="text-white-500 py-2 px-4 inline-flex items-center" title="Install count">
                  <font-awesome-icon icon="arrow-down" size="1x" />&nbsp;
                  <small class="ml-2 text-gray-400">{{ currentModpack.installs | formatNumber }}</small>
                </div>
              </div>
              <div class="instance-button mr-1">
                <div class="text-white-500 py-2 px-4 inline-flex items-center" title="Play count">
                  <font-awesome-icon icon="gamepad" size="1x" />&nbsp;
                  <small class="ml-2 text-gray-400">{{ currentModpack.plays | formatNumber }}</small>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div style="height: auto; flex:1; overflow-y: auto;" class="flex flex-col">
          <ul
            class="flex p-2 pb-0"
            style="position: sticky; top: 0; background: #2A2A2A; padding-bottom: 0;"
            v-if="modpacks.installing === null"
          >
            <li class="-mb-px mr-1">
              <a
                class="bg-sidebar-item inline-block py-2 px-4 font-semibold cursor-pointer"
                @click.prevent="setActiveTab('overview')"
                :class="{
                  'border-l border-t border-r border-navbar': isTabActive('overview'),
                  'text-gray-600 hover:text-gray-500': !isTabActive('overview'),
                }"
                href="#overview"
              >
                Overview
              </a>
            </li>
            <li class="-mb-px mr-1">
              <a
                class="bg-sidebar-item inline-block py-2 px-4 font-semibold cursor-pointer"
                @click.prevent="setActiveTab('versions')"
                :class="{
                  'border-l border-t border-r border-navbar': isTabActive('versions'),
                  'text-gray-600 hover:text-gray-500': !isTabActive('versions'),
                }"
                href="#versions"
              >
                Versions
              </a>
            </li>
            <li class="-mb-px mr-1">
              <a
                class="bg-sidebar-item inline-block py-2 px-4 font-semibold cursor-pointer"
                @click.prevent="setActiveTab('multiplayer')"
                :class="{
                  'border-l border-t border-r border-navbar': isTabActive('multiplayer'),
                  'text-gray-600 hover:text-gray-500': !isTabActive('multiplayer'),
                }"
                href="#multiplayer"
              >
                Multiplayer
              </a>
            </li>
          </ul>

          <div class="tab-content bg-navbar flex-1 p-2 py-4 mx-2" style="overflow-y: auto;">
            <div class="tab-pane" v-if="isTabActive('overview')" id="overview">
              <div class="flex flex-wrap" v-if="currentModpack != null">
                <VueShowdown
                  :markdown="currentModpack.description"
                  :extensions="['classMap', 'attribMap', 'newLine']"
                />
              </div>

              <!-- <div class="versions" v-if="currentModpack != null && currentModpack.versions != null">
            <div v-for="(version, index) in currentModpack.versions" v-bind:key="index">
              {{version.name}}
  
              <span class="inline-flex bg-blue-600 text-white rounded-full h-6 px-3 justify-center items-center" v-if="version.type == 'Beta'">{{version.type}}</span>
              <span class="inline-flex bg-green-600 text-white rounded-full h-6 px-3 justify-center items-center" v-if="version.type == 'Release'">{{version.type}}</span>
  
              </div>-->
              <!-- </div> -->
            </div>
            <div class="tab-pane" v-if="isTabActive('versions')" id="versions">
              <div v-for="(version, index) in currentModpack.versions" :key="index">
                <div class="flex flex-row bg-sidebar-item p-5 my-4 items-center">
                  <p>{{ currentModpack.name }} - {{ version.name }}</p>
                  <span @click="toggleChangelog(version.id)" class="pl-5 cursor-pointer"
                    ><font-awesome-icon
                      :icon="activeChangelog === version.id ? 'chevron-down' : 'chevron-right'"
                      class="cursor-pointer"
                      size="1x"
                    />
                    Changelog</span
                  >
                  <ftb-button
                    class="py-2 px-4 ml-auto"
                    color="primary"
                    css-class="text-center text-l"
                    @click="install(version.id)"
                    :disabled="modpacks.installing !== null"
                  >
                    <font-awesome-icon icon="download" size="1x" />
                    Install
                  </ftb-button>
                  <!--                <button-->
                  <!--                    class="bg-blue-500 hover:bg-blue-400 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-auto cursor-pointer"-->
                  <!--                    @click="install(version.id)"-->
                  <!--                >-->
                  <!--                  <span class="cursor-pointer"><font-awesome-icon icon="download" size="1x"/>&nbsp;Install</span>-->
                  <!--                </button>-->
                  <!--                <button @click="downloadServer"-->
                  <!--                        class="bg-orange-500 hover:bg-blue-400 text-white-600 font-bold py-2 px-4 inline-flex items-center ml-5 cursor-pointer">-->
                  <!--                  <span class="cursor-pointer"><font-awesome-icon icon="download" size="1x"/> Download Server</span>-->
                  <!--                </button>-->
                </div>
                <div class="pl-5 bg-background" v-if="activeChangelog === version.id">
                  <!--                <code class="p-0">-->
                  <!--                  {{changelogs[version.id]}}-->
                  <!--                </code>-->
                  <VueShowdown
                    v-if="changelogs[version.id]"
                    :markdown="changelogs[version.id]"
                    :extensions="['classMap', 'newLine']"
                  />
                  <p v-else>No changelog available</p>
                </div>
              </div>
            </div>

            <div class="tab-pane" v-if="isTabActive('multiplayer')" id="multiplayer">
              <div v-if="currentVersionObject !== null && shuffledServers.length > 0">
                <server-card
                  v-for="server in shuffledServers"
                  :key="server.id"
                  :server="server"
                  :art="
                    currentModpack.art.length > 0 ? currentModpack.art.filter(art => art.type === 'square')[0].url : ''
                  "
                ></server-card>
              </div>
              <div class="flex flex-1 pt-1 flex-wrap overflow-x-auto justify-center flex-col items-center" v-else>
                <font-awesome-icon icon="heart-broken" style="font-size: 25vh"></font-awesome-icon>
                <h1 class="text-5xl">Oh no!</h1>
                <span>It doesn't looks like there are any public MineTogether servers</span>
              </div>
            </div>
          </div>
        </div>
        <ftb-modal :visible="showInstallBox" @dismiss-modal="hideInstall">
          <InstallModal
            :pack-name="currentModpack.name"
            :doInstall="install"
            :pack-description="currentModpack.synopsis"
            :versions="currentModpack.versions"
            :selectedVersion="installSelectedVersion"
          />
        </ftb-modal>
      </div>
    </div>
  </div>
</template>

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

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import { ModpackState, Versions } from '@/modules/modpacks/types';
import { Action, State } from 'vuex-class';
import FTBToggle from '@/components/FTBToggle.vue';
import MessageModal from '@/components/modals/MessageModal.vue';
import FTBModal from '@/components/FTBModal.vue';
import { shuffle } from '../utils';
import { SettingsState } from '../modules/settings/types';
import { ServersState } from '@/modules/servers/types';
import ServerCard from '@/components/ServerCard.vue';
import InstallModal from '@/components/modals/InstallModal.vue';
import { PackConst } from '@/utils/contants';
import PackMetaHeading from '@/components/modpack/modpack-elements/PackMetaHeading.vue';
import PackTitleHeader from '@/components/modpack/modpack-elements/PackTitleHeader.vue';
import PackTabsBody from '@/components/modpack/modpack-elements/PackTabsBody.vue';
import { ModpackPageTabs } from '@/views/InstancePage.vue';
import { AuthState } from '@/modules/auth/types';
import ModpackVersions from '@/components/modpack/ModpackVersions.vue';

interface Changelogs {
  [id: number]: string;
}

@Component({
  name: 'ModpackPage',
  components: {
    ModpackVersions,
    PackTabsBody,
    PackTitleHeader,
    PackMetaHeading,
    'ftb-toggle': FTBToggle,
    InstallModal,
    'ftb-modal': FTBModal,
    'message-modal': MessageModal,
    ServerCard,
  },
})
export default class ModpackPage extends Vue {
  activeTab: ModpackPageTabs = ModpackPageTabs.OVERVIEW;
  showVersions = false;

  get currentModpack() {
    const id: number = parseInt(this.$route.query.modpackid as string, 10);
    if (this.modpacks.packsCache[id] === undefined) {
      return null;
    }
    return this.modpacks.packsCache[id];
  }

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

  private showInstallBox: boolean = false;
  private installSelectedVersion: number | null = null;
  private loading = true;
  private packType: number = 0;

  private activeChangelog: number | undefined = -1;
  private changelogs: Changelogs = [];

  mods: { version: string; size: string; name: string }[] = [];

  public hideInstall(): void {
    this.showInstallBox = false;
  }

  public isTabActive(tabItem: string) {
    return this.activeTab === ((tabItem as unknown) as ModpackPageTabs);
  }

  public setActiveTab(tabItem: string) {
    this.activeTab = (tabItem as unknown) as ModpackPageTabs;
  }

  public install(version: number): void {
    if (this.showInstallBox) {
      this.showInstallBox = false;
    }
    this.$router.replace({
      name: 'installingpage',
      query: {
        modpackid: this.$route.query.modpackid,
        versionID: version.toString(),
        packType: this.$route.query.type,
      },
    });
  }

  private async mounted() {
    const packID: number = parseInt(this.$route.query.modpackid as string, 10);
    this.packType = parseInt(this.$route.query.type as string, 10);
    this.packType == 0 ? await this.fetchModpack(packID) : await this.fetchCursepack(packID);
    if (this.modpacks.packsCache[packID] !== undefined) {
      this.loading = false;
    }
    if (this.$route.query.showInstall === 'true') {
      if (this.$route.query.version !== undefined) {
        this.installSelectedVersion = parseInt(this.$route.query.version as string, 10);
      }
      this.showInstallBox = true;
    }

    const res = await fetch(
      `${process.env.VUE_APP_MODPACK_API}/${this.auth.token?.attributes.modpackschkey ?? 'public'}/${
        this.currentModpack?.type?.toLowerCase() === 'curseforge' ? 'curseforge' : 'modpack'
      }/${this.currentModpack?.id}/${this.currentModpack?.versions[0]?.id}`,
    );

    const modsRaw = await res.json();
    this.mods = modsRaw.files
      ?.filter((e: any) => e.type === 'mod')
      .map((e: any) => ({ name: e.name, size: e.size, version: e.version }));

    this.toggleChangelog(this.currentModpack?.versions[0].id);
    if (this.currentVersionObject !== null) {
      if (this.currentVersionObject.mtgID) {
        this.fetchServers(this.currentVersionObject.mtgID);
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

  get packSplashArt() {
    if (this.currentModpack === null) {
      return PackConst.defaultPackSplashArt;
    }

    const splashArt = this.currentModpack.art?.filter(art => art.type === 'splash');
    return splashArt?.length > 0 ? splashArt[0].url : PackConst.defaultPackSplashArt;
  }
}
</script>
