<template>
  <div class="flex flex-1 flex-col h-full overflow-hidden">
    <div class="flex flex-col h-full" v-if="modpack !== null && server !== null" key="main-window">
      <div>
        <div
          class="header-image"
          v-bind:style="{'background-image': `url(${modpack.art.filter((art) => art.type === 'splash').length > 0 ? modpack.art.filter((art) => art.type === 'splash')[0].url : 'https://dist.creeper.host/FTB2/wallpapers/alt/T_nw.png'})`}"
        >
          <span class="instance-name text-4xl">{{server.name}}</span>
          <span class="instance-info">
            <small v-if="server.protoResponse && server.protoResponse.description !== undefined">
              <em
                class="mb-auto max-2-lines"
              >{{server.protoResponse.description.text.replace(/\u00a7[0-9a-fk-or]/ig, '')}}</em>
            </small>
            <small v-else>
              <em>Loading....</em>
            </small>
            <div v-if="modpack.tags" class="flex flex-row items-center">
              <div class="flex flex-row">
                <span
                  v-for="(tag, i) in limitedTags"
                  :key="`tag-${i}`"
                  @click="clickTag(tag.name)"
                  class="cursor-pointer rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light"
                  style="font-variant: small-caps;"
                >{{tag.name}}</span>
                <span
                  v-if="modpack.tags.length > 5"
                  :key="`tag-more`"
                  class="rounded mr-2 text-sm bg-gray-600 px-2 lowercase font-light"
                  style="font-variant: small-caps;"
                >+{{modpack.tags.length - 5}}</span>
              </div>
            </div>
          </span>
          <div class="instance-buttons flex flex-row frosted-glass">
            <div class="instance-button mr-1" v-if="installedPacks.length === 0">
              <ftb-button
                class="py-2 px-4"
                color="primary"
                css-class="text-center text-l"
                @click="install(version.id)"
              >
                <font-awesome-icon icon="download" size="1x" />Install
              </ftb-button>
            </div>
            <div class="instance-button mr-1">
              <div
                class="text-white-500 py-2 px-4 inline-flex items-center"
                v-if="version !== null"
              >
                <small class="ml-2 text-gray-400">Version: {{version.name}}</small>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div style="height: auto; flex:1; overflow-y: auto;" class="flex flex-col">
        <div class="tab-content bg-navbar flex-1 p-2 py-4" style="overflow-y: auto;">
          <div class="tab-pane" id="overview">
            <div class="flex flex-wrap" v-if="modpack !== null && installedPacks.length > 0">
              <div class="flex-1 justify-center">
                <transition-group
                  name="list"
                  tag="div"
                  class="flex pt-1 flex-wrap overflow-x-auto px-auto"
                  appear
                >
                  <pack-card-wrapper
                    v-for="modpack in installedPacks"
                    :list-mode="settings.settings.listMode"
                    :key="modpack.uuid"
                    :art="modpack.art"
                    :installed="true"
                    :minecraft="modpack.minecraft"
                    :version="modpack.version"
                    :description="getModpack(modpack.id) !== null ? getModpack(modpack.id).synopsis : 'Unable to load synopsis'"
                    :tags="getModpack(modpack.id) !== null ? getModpack(modpack.id).tags : []"
                    :versions="modpack.versions"
                    :name="modpack.name"
                    :kind="modpack.kind"
                    :authors="modpack.authors"
                    :instance="modpack"
                    :instanceID="modpack.uuid"
                    :preLaunch="preLaunch"
                    :postLaunch="postLaunch"
                  ></pack-card-wrapper>
                </transition-group>
              </div>
            </div>
            <div v-else class="flex flex-wrap flex-col items-center">
              <font-awesome-icon icon="heart-broken" style="font-size: 20vh"></font-awesome-icon>
              <h1 class="text-5xl">Oh no!</h1>
              <span>Seems you don't have instances of this modpack</span>
              <div class="flex flex-row justify-between my-2">
                <ftb-button
                  class="py-2 px-4"
                  color="primary"
                  css-class="text-center text-l"
                  @click="install(version.id)"
                >
                  <font-awesome-icon icon="download" size="1x" />Install
                </ftb-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<style lang="scss">
.header-image {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 300px;
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
import { State, Action, Getter } from 'vuex-class';
import { NewsState, NewsItem } from '@/modules/news/types';
import { ModpackState, ModPack, Versions, Instance } from '../modules/modpacks/types';
import { getAPIRequest } from '../modules/modpacks/actions';
import FTBButton from '@/components/FTBButton.vue';
import { queryServer, logVerbose } from '../utils';
import { SettingsState } from '../modules/settings/types';
import PackCardWrapper from '@/components/packs/PackCardWrapper.vue';

@Component({
  components: {
    'ftb-button': FTBButton,
    PackCardWrapper,
  },
})
export default class ServerLandingPage extends Vue {
  @State('modpacks') public modpacks!: ModpackState;
  @State('settings') public settings!: SettingsState;
  @Getter('packsCache', { namespace: 'modpacks' })
  public packsCache!: ModPack[];
  @Action('fetchModpack', { namespace: 'modpacks' }) public fetchModpack!: (
    packID: number,
  ) => Promise<ModPack>;
  @Action('updateInstall', { namespace: 'modpacks' })
  public updateInstall!: any;
  @Action('finishInstall', { namespace: 'modpacks' })
  public finishInstall!: any;
  @Action('storeInstalledPacks', { namespace: 'modpacks' })
  public storePacks!: any;
  @Action('sendMessage') public sendMessage!: any;

  private server: any = null;
  private modpack: ModPack | null = null;
  private version: Versions | null = null;
  private serverID: number = -1;

  get installedPacks() {
    return this.modpacks.installedPacks
      .filter((f) => btoa(`${f.id}${f.versionId}`) === this.server.project)
      .sort((a, b) => b.lastPlayed - a.lastPlayed);
  }

  public getModpack(id: number): ModPack | null {
    return this.packsCache[id] ? this.packsCache[id] : null;
  }

  get limitedTags() {
    if (this.modpack) {
      return this.modpack.tags.slice(0, 5);
    }
    return [];
  }

  public getCorrectVersion(mtgID: string){
    const result = this.modpack?.versions?.filter((obj: Versions) => {
      return obj.mtgID === mtgID
    })
    if(result !== undefined){
      return result[0]
    }
  }

  public install(version: number): void {
      if (this.modpack === null) {
          return;
      }
      this.$router.replace({name: 'installingpage', query: {modpackid: this.$props.packID, versionID: version.toString()}});
  }

  public preLaunch(instance: Instance){
    let newArgs = instance.jvmArgs;
    if(newArgs.indexOf("-Dmt.server") !== 1){
      const args = newArgs.split(" ");
      args.splice(args.findIndex(value => value.indexOf("-Dmt.server") !== -1), 1);
      newArgs = args.join(" ");
    }
    if(newArgs[newArgs.length - 1] === " " || newArgs.length === 0){
      newArgs += "-Dmt.server=" + this.serverID;
    } else {
      newArgs += " -Dmt.server=" + this.serverID;
    }
    return new Promise((res, rej) => {
      this.sendMessage({
          payload: {type: 'instanceConfigure', uuid: instance.uuid, instanceInfo: {jvmargs: newArgs}}, callback: (data: any) => {
              res();
          },
      });
    })
  }

  public postLaunch(instance: Instance){
    return new Promise((res, rej) => {
      let newArgs = instance.jvmArgs;
      if(newArgs.indexOf("-Dmt.server") !== 1){
        const args = newArgs.split(" ");
        args.splice(args.findIndex(value => value.indexOf("-Dmt.server") !== -1), 1);
        newArgs = args.join(" ");
      }
      this.sendMessage({
          payload: {type: 'instanceConfigure', uuid: instance.uuid, instanceInfo: {jvmargs: newArgs}}, callback: (data: any) => {
              res();
          },
      });
    })
  }

  public mounted() {
    this.serverID = parseInt(this.$route.query.serverid as string, 10);
    fetch(`https://api.creeper.host/minetogether/server`, {
      method: 'PUT',
      body: JSON.stringify({ serverid: this.serverID }),
      headers: { 'Content-Type': 'application/json' },
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.status === 'success') {
          this.server = data.server;
          queryServer(this.server.ip).then((protoResponse) => {
            Vue.set(this.server, 'protoResponse', protoResponse);
          });
          Object.values(this.modpacks.packsCache).forEach((pack: ModPack) => {
              if (pack.versions) {
                pack.versions.forEach((v: Versions) => {
                    if (v.mtgID === this.server.project) {
                        this.modpack = pack;
                    }
                });
              }
          });
          if (this.modpack === null) {
            fetch(
              `https://www.creeperhost.net/json/modpacks/modpacksch/${this.server.project}`,
            )
              .then((resp) => resp.json())
              .then((data) => {
                if (data.status === 'success') {
                  getAPIRequest(
                    this.$store.state,
                    `modpack/search/8?term=${data.name}`,
                  )
                    .then((response) => response.json())
                    .then(async (data) => {
                      if (data.status === 'error') {
                        return;
                      }
                      const packIDs = data.packs;
                      if (packIDs == null) {
                        return;
                      }
                      if (packIDs.length === 0) {
                        return;
                      }
                      const pack: ModPack = await this.fetchModpack(packIDs[0]);
                      pack.versions.forEach((v: Versions) => {
                        if (v.mtgID === this.server.project) {
                          this.modpack = pack;
                          this.version = v;
                        }
                      });
                    })
                    .catch((err) => {
                      console.error(err);
                    });
                }
              });
          }
          if(this.modpack !== null){
            const tmpVersion = this.getCorrectVersion(this.server.project);
            if(tmpVersion !== undefined){
              this.version = tmpVersion;
            }
          }
        }
      });
  }
}
</script>

