<template>
  <div class="pack-versions py-4">
    <div class="aside">
      <p>Versions</p>
      <div class="items">
        <div
          class="item"
          :class="{ active: version.id === activeLog }"
          v-for="(version, index) in versions"
          :key="index"
        >
          <div class="main" @click="() => loadChanges(version.id)">
            <div class="version">{{ version.name }}</div>
            <div class="type-data">
              <div
                class="type"
                :style="{
                  backgroundColor:
                    version.type.toLowerCase() === 'release'
                      ? '#27AE60'
                      : version.type.toLowerCase() === 'beta'
                      ? '#00a8FF'
                      : 'black',
                }"
              >
                {{ version.type }}
              </div>
              <div class="current" v-if="version.id === current">Current</div>
              <div class="latest" v-if="index === 0">Latest</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="main flex flex-col" :key="activeLog">
      <div class="heading flex items-center mb-4" v-if="currentVersion">
        <div class="content flex-1 mr-4">
          <p class="opacity-50">Changelog for</p>
          <div class="font-bold name text-xl">{{ currentVersion.name }}</div>
        </div>
        <div class="buttons flex text-sm">
          <ftb-button
            @click="
              () =>
                platform.get.utils.openUrl(
                  `https://feed-the-beast.com/modpacks/${packInstance.id}/server/${currentVersion.id}${
                    packInstance.packType !== 0 ? '/curseforge' : ''
                  }`,
                )
            "
            class="py-2 px-4 ml-auto mr-1"
            color="info"
            css-class="text-center text-l"
          >
            <font-awesome-icon icon="server" class="mr-2" />
            Server files
          </ftb-button>
          <ftb-button
            v-if="currentVersion && instance.versionId !== activeLog"
            class="py-2 px-4 ml-1"
            color="warning"
            css-class="text-center text-l"
            @click="update(currentVersion.id)"
          >
            <font-awesome-icon icon="download" class="mr-2" />
            {{ isOlderVersion(currentVersion.id) ? 'Downgrade' : 'Update' }}
          </ftb-button>
        </div>
      </div>
      <!--      <div class="updated">{{ version.updated | momentFromNow }}</div>-->
      <div class="body-contents flex-1 overflow-y-auto">
        <div class="loading" v-if="loading"><font-awesome-icon icon="spinner" class="mr-2" spin /> Loading...</div>
        <VueShowdown
          v-else-if="changelogs[activeLog] && changelogs[activeLog] !== ''"
          flavor="github"
          :markdown="changelogs[activeLog]"
          :extensions="['classMap']"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Instance, Versions } from '@/modules/modpacks/types';
import { Component, Prop, Vue } from 'vue-property-decorator';
import { Action } from 'vuex-class';
import platform from '@/utils/interface/electron-overwolf';

@Component
export default class ModpackVersions extends Vue {
  @Action('getChangelog', { namespace: 'modpacks' }) public getChangelog!: any;

  @Prop() versions!: Versions[];
  @Prop() packInstance!: Instance;
  @Prop() instance!: Instance;
  @Prop() current!: number;

  platform = platform;

  changelogs: Record<string, string> = {};
  currentVersion: Versions | null = null;
  activeLog: number = -1;
  loading = true;

  mounted() {
    // get the first log
    this.fetchLog(this.current)
      .then(data => {
        this.changelogs['' + this.current] = data;
        this.setActive(this.current);
      })
      .catch(console.error);
  }

  async loadChanges(versionId: number) {
    if (this.changelogs['' + versionId]) {
      this.setActive(versionId);
      return;
    }

    this.changelogs['' + versionId] = await this.fetchLog(versionId);
    this.setActive(versionId);
  }

  setActive(versionId: number) {
    this.activeLog = versionId;
    this.currentVersion = this.versions.find(e => e.id === this.activeLog) ?? null;
  }

  async fetchLog(versionId: number) {
    this.loading = true;
    const changelog = await this.getChangelog({
      packID: this.instance?.id,
      versionID: versionId,
      type: this.instance?.packType,
    });

    this.loading = false;
    return changelog.content ?? `No changelog available for this version`;
  }

  public isOlderVersion(version: number) {
    return this.instance.versionId > version ?? false;
  }

  public update(versionId?: number): void {
    const modpackID = this.instance?.id;
    const version = versionId || this.packInstance?.kind === 'modpack' ? versionId : this.versions[0].id;
    this.$router.replace({
      name: 'installingpage',
      query: { modpackid: modpackID?.toString(), versionID: version?.toString(), uuid: this.instance?.uuid },
    });
  }
}
</script>

<style scoped lang="scss">
.pack-versions {
  display: flex;
  height: 100%;

  .aside,
  .main {
    > p {
      margin-bottom: 1rem;
      font-size: 1.25rem;
    }
  }

  .aside {
    margin-right: 2rem;
    overflow-y: auto;
    height: 100%;

    .items {
      width: 280px;

      .item {
        display: flex;
        align-items: center;
        justify-content: space-between;

        margin-bottom: 1rem;
        padding: 0.8rem 1rem;
        border-radius: 5px;
        cursor: pointer;
        transition: background-color 0.25s ease-in-out;

        &.active,
        &:hover {
          background-color: var(--color-background);
        }

        .type-data {
          display: flex;

          %type {
            padding: 0.1rem 0.3rem;
            border-radius: 3px;
            background-color: #595959;
            font-size: 0.875rem;
            text-transform: capitalize;
            display: inline-block;
          }

          .type,
          .latest,
          .current {
            @extend %type;
            margin-right: 0.5rem;
          }

          .latest {
            background-color: var(--color-info-button);
          }

          .current {
            background-color: var(--color-warning-button);
          }
        }

        .version {
          font-weight: bold;
          font-family: Arial, Helvetica, sans-serif;
          margin-bottom: 0.4rem;
        }

        .updated {
          font-size: 0.875rem;
          opacity: 0.8;
        }
      }
    }
  }

  .main {
    flex: 1;
    overflow-x: auto;
    height: 100%;
  }
}
</style>
