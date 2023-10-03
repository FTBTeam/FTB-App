<template>
  <div class="pack-versions">
    <div class="aside mb-6">
      <selection
        :badge-end="true"
        :options="packVersions"
        :inheritedSelection="packVersions[0]"
        @selected="(version) => loadChanges(version)"
      />
    </div>
    <div class="main flex pb-8 flex-col" :key="activeLog">
      <div class="heading flex items-center mb-4" v-if="currentVersion">
        <div class="content flex-1 mr-4">
          <p class="opacity-50">Changelog for</p>
          <div class="font-bold name text-xl">{{ currentVersion.name }}</div>
        </div>
        <div class="buttons flex text-sm">
          <ftb-button
            @click="() => platform.get.utils.openUrl(`https://feed-the-beast.com/modpacks/server-files`)"
            class="py-2 px-4 ml-auto mr-1"
            color="info"
            css-class="text-center text-l"
          >
            <font-awesome-icon icon="server" class="mr-2" />
            Server files
          </ftb-button>
          <ftb-button
            v-if="
              instance &&
              currentVersion &&
              instance.versionId !== activeLog &&
              currentVersion.type.toLowerCase() !== 'archived'
            "
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
      <div class="body-contents flex-1 select-text">
        <div v-if="loading" class="loading"><font-awesome-icon icon="spinner" class="mr-2" spin /> Loading...</div>
        <div
          v-else-if="currentVersion && currentVersion.type.toLowerCase() !== 'archived'"
          class="bg-orange-400 text-orange-900 font-bold px-4 py-4 rounded mb-6"
        >         
          <font-awesome-icon icon="triangle-exclamation" size="xl" class="mr-4" /> Always backup your worlds before updating.
        </div>
        <message class="mb-4 shadow-lg mr-4 mt-2" type="danger" v-if="currentVersion && currentVersion.type.toLowerCase() === 'archived'">
          <p>
            This version has been archived! This typically means the update contained a fatal error causing the version
            to be too unstable for players to use.
          </p>
        </message>
        
        <div class="wysiwyg" v-if="!loading && changelogs[activeLog] && changelogs[activeLog] !== ''" v-html="parseMarkdown(changelogs[activeLog])" />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Instance, ModPack, Versions } from '@/modules/modpacks/types';
import { Component, Emit, Prop, Vue } from 'vue-property-decorator';
import { Action } from 'vuex-class';
import platform from '@/utils/interface/electron-overwolf';
import { InstallerState } from '@/modules/app/appStore.types';
import {getColorForReleaseType, getPackArt, parseMarkdown} from '@/utils';
import Selection from '@/components/atoms/input/Selection.vue';
import dayjs from 'dayjs';

@Component({
  components: {Selection}
})
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

  parseMarkdown = parseMarkdown;

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
        private: this.packInstance.private ?? false,
      },
      meta: {
        name: this.instance.name,
        version: this.currentVersion?.name ?? '',
        art: getPackArt(this.instance?.art),
      },
    });
  }
  
  get packVersions() {
    return this.versions.map(e => ({
      value: e.id, 
      text: e.name + (this.instance?.versionId === e.id ? ' (Current)' : ''), 
      meta: dayjs.unix(e.updated).format("DD MMMM YYYY, HH:mm"), 
      badge: {color: getColorForReleaseType(e.type), text: e.type} 
    }))
  }
}
</script>

<style scoped lang="scss">
.pack-versions {
  height: 100%;
  position: relative;
  font-size: 0.875rem;

  line-height: 1.8em;

  .aside,
  .main {
    > p {
      font-size: 1.25rem;
    }
  }

  .main {
    flex: 1;
  }

  .closer {
    cursor: pointer;
    padding: 0.5rem 0.8rem;
    font-size: 1.2rem;
    position: absolute;
    top: -1rem;
    right: -2rem;
    opacity: 0.5;
    transition: opacity 0.2s ease-in-out;
    &:hover {
      opacity: 1;
    }
  }
}
</style>
