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
            <header class="flex justify-between flex-wrap">
              <div class="version">{{ version.name }}</div>
              <div class="updated">{{ version.updated | dayjsFromNow }}</div>
            </header>
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
                    packInstance.type.toLowerCase() === 'curseforge' ? '/curseforge' : ''
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
            v-if="instance && currentVersion && instance.versionId !== activeLog"
            class="py-2 px-4 ml-1"
            color="warning"
            css-class="text-center text-l"
            @click="update"
          >
            <font-awesome-icon icon="download" class="mr-2" />
            {{ isOlderVersion(currentVersion.id) ? 'Downgrade' : 'Update' }}
          </ftb-button>
        </div>
      </div>
      <!--      <div class="updated">{{ version.updated | momentFromNow }}</div>-->
      <div class="body-contents flex-1 overflow-y-auto">
        <div v-if="loading" class="loading"><font-awesome-icon icon="spinner" class="mr-2" spin /> Loading...</div>
        <div v-else class="bg-orange-400 text-orange-900 font-bold px-4 py-4 rounded mb-6">
          Warning! Always backup your worlds before updating.
        </div>
        <VueShowdown
          v-if="!loading && changelogs[activeLog] && changelogs[activeLog] !== ''"
          flavor="github"
          :markdown="changelogs[activeLog]"
          :extensions="['classMap']"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Instance, ModPack, Versions } from '@/modules/modpacks/types';
import { Component, Prop, Vue } from 'vue-property-decorator';
import { Action } from 'vuex-class';
import platform from '@/utils/interface/electron-overwolf';
import { InstallerState } from '@/modules/app/appStore.types';
import { getPackArt } from '@/utils';

@Component
export default class ModpackVersions extends Vue {
  @Action('getChangelog', { namespace: 'modpacks' }) public getChangelog!: any;
  @Action('installModpack', { namespace: 'app' }) public installModpack!: (data: InstallerState) => void;

  @Prop() versions!: Versions[];
  @Prop() packInstance!: ModPack;
  @Prop() instance!: Instance;
  @Prop() current!: number;

  platform = platform;

  changelogs: Record<string, string> = {};
  currentVersion: Versions | null = null;
  activeLog: number = -1;
  loading = true;

  mounted() {
    const lcurrent = this.current ?? this.versions[0].id;

    // get the first log
    this.fetchLog(lcurrent)
      .then((data) => {
        this.changelogs['' + lcurrent] = data;
        this.setActive(lcurrent);
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
    this.currentVersion = this.versions.find((e) => e.id === this.activeLog) ?? null;
  }

  async fetchLog(versionId: number) {
    this.loading = true;
    const changelog = await this.getChangelog({
      packID: this.packInstance.id,
      versionID: versionId,
      type: this.packInstance?.type?.toLowerCase() === 'curseforge' ? 1 : 0,
    });

    this.loading = false;
    return changelog.content ?? `No changelog available for this version`;
  }

  public isOlderVersion(version: number) {
    return this.instance?.versionId > version ?? false;
  }

  public update(): void {
    this.installModpack({
      pack: {
        id: this.instance.id,
        uuid: this.instance.uuid,
        version: this.currentVersion?.id,
        packType: this.instance.packType,
      },
      meta: {
        name: this.instance.name,
        version: this.currentVersion?.name ?? '',
        art: getPackArt(this.instance?.art),
      },
    });
    // this.$router.replace({
    //   name: 'installingpage',
    //   query: { modpackid: modpackID?.toString(), versionID: version?.toString(), uuid: this.instance?.uuid },
    // });
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
          font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif,
            'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol';
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
