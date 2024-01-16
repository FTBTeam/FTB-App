<template>
  <div class="pack-versions">
    <div class="aside mb-6">
      <selection2
        :badge-end="true"
        :options="packVersions"
        v-model="version"
      />
    </div>
    <div class="main flex pb-8 flex-col" :key="activeLog">
      <div class="heading flex items-center mb-4" v-if="currentVersion">
        <div class="content flex-1 mr-4">
          <p class="opacity-50">Changelog for</p>
          <div class="font-bold name text-xl">{{ currentVersion.name }}</div>
        </div>
        <div class="buttons flex text-sm gap-2">
          <ui-button type="info" size="small" icon="server" @click="() => platform.get.utils.openUrl(`https://go.ftb.team/serverfiles`)">Server files</ui-button>
          <ui-button
            :wider="true"
            v-if="instance && currentVersion && instance.versionId !== activeLog && currentVersion.type.toLowerCase() !== 'archived'" 
            :type="isOlderVersion(currentVersion.id) ? 'warning' : 'success'" size="small" icon="download" @click="update">
            {{ isOlderVersion(currentVersion.id) ? 'Downgrade' : 'Update' }}
          </ui-button>
        </div>
      </div>
      <div class="body-contents flex-1 select-text">
        <div v-if="loading && !isCursePack" class="loading"><font-awesome-icon icon="spinner" class="mr-2" spin /> Loading...</div>
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
        <div v-if="isCursePack">
          <message type="info">Changelogs are not yet supported for non-FTB modpacks.</message>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {ModPack, Versions} from '@/modules/modpacks/types';
import {Component, Prop, Vue, Watch} from 'vue-property-decorator';
import platform from '@/utils/interface/electron-overwolf';
import {getColorForReleaseType, parseMarkdown} from '@/utils';
import {typeIdToProvider} from '@/utils/helpers/packHelpers';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {InstanceJson} from '@/core/@types/javaApi';
import {RouterNames} from '@/router';
import {modpackApi} from '@/core/pack-api/modpackApi';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import Selection2, {SelectionOptions} from '@/components/core/ui/Selection2.vue';
import dayjs from 'dayjs';
import {alertController} from '@/core/controllers/alertController';
import UiButton from '@/components/core/ui/UiButton.vue';
import {createLogger} from '@/core/logger';

@Component({
  components: {UiButton, Selection2}
})
export default class ModpackVersions extends Vue {
  private logger = createLogger(ModpackVersions.name + ".vue")
  
  @Prop() versions!: Versions[];
  @Prop() packInstance!: ModPack;
  @Prop() instance!: InstanceJson;

  platform = platform;

  changelogs: Record<string, string> = {};
  currentVersion: Versions | null = null;
  activeLog: number = -1;
  loading = true;

  version: number | string = -1;
  
  parseMarkdown = parseMarkdown;

  mounted() {
    const sortedVersion = this.versions.sort((a, b) => b.id - a.id);
    
    const currentId = this.instance?.versionId ?? this.packInstance?.versions[0]?.id ?? -1;
    const lcurrent = sortedVersion.find(e => e.id === currentId)?.id ?? sortedVersion[0].id;
    this.version = lcurrent;

    // TODO: (M#01) Fix this once the api has been updated to support a `provider` field
    if (this.isCursePack) {
      return;
    }
    
    // get the first log
    this.fetchLog(lcurrent)
      .then((data) => {
        this.changelogs['' + lcurrent] = data;
        this.setActive(lcurrent);
      })
      .catch(e => this.logger.error(e))
  }
  
  destoryed() {
    this.changelogs = {};
  }

  @Watch('version')
  async loadChanges(versionId: number) {
    if (this.changelogs['' + versionId]) {
      this.setActive(versionId);
      return;
    }
    
    if (!this.isCursePack) {
      this.changelogs['' + versionId] = await this.fetchLog(versionId);
    }
    
    this.setActive(versionId);
  }

  setActive(versionId: number) {
    this.activeLog = versionId;
    this.currentVersion = this.versions.find((e) => e.id === this.activeLog) ?? null;
  }

  async fetchLog(versionId: number) {
    if (this.isCursePack) {
      return "";
    }
    
    try {
      const changelog = await toggleBeforeAndAfter(
        () => modpackApi.modpacks.getChangelog(this.packInstance.id, versionId, this.isCursePack ? "curseforge" : "modpacksch"),
        state => this.loading = state
      )

      return changelog?.content ?? `No changelog available for this version`;
    } catch (e) {
      this.logger.error(e)
      alertController.warning("Unable to load changelog")
      return "Unable to locate changelog for this version";
    }
  }

  public isOlderVersion(version: number) {
    return this.instance?.versionId > version ?? false;
  }

  public update(): void {
    if (!this.instance || !this.currentVersion) {
      // Non-instances can't be updated
      return;
    }
    
    instanceInstallController.requestUpdate(this.instance, this.currentVersion, typeIdToProvider(this.instance.packType));
    this.$router.push({
      name: RouterNames.ROOT_LIBRARY
    })
  }
  
  get packVersions(): SelectionOptions {
    return [...this.versions].sort((a, b) => b.id - a.id).map(e => ({
      value: e.id, 
      label: e.name + (this.instance?.versionId === e.id ? ' (Current)' : ''), 
      meta: dayjs.unix(e.updated).format("DD MMMM YYYY, HH:mm"), 
      badge: {color: getColorForReleaseType(e.type), text: e.type} 
    })) ?? []
  }
  
  get isCursePack() {
    // TODO: (M#01) Fix this once the api has been updated to support a `provider` field
    // TODO: (M#01) > 1000 isn't a great option here but it's the only way to ensure this still works once the
    //       provider field is added and the type is set correctly
    return this.packInstance.type.toLowerCase() == "curseforge" || this.packInstance.id > 1000;
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
