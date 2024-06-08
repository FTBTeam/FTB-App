<template>
  <div class="changelogs" v-if="!error">
    <div class="changelogs">
      <div class="log mb-8" v-for="(changelog, index) in allLogs">
        <h3 class="mb-4 text-xl font-bold">{{changelog.version}}{{ changelog.title != changelog.version ? ` - ${changelog.title}` : "" }}</h3>
        <changelog-entry :key="index" :changelog="changelog" />
      </div>
    </div>
    
    <div class="flex justify-center">
      <ui-button type="info" @click="loadMore" v-if="!allLoaded">Load more</ui-button>
    </div>
  </div>
  <div v-else>
    <message type="warning">
      <p>{{error}}</p>
    </message>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {JavaFetch} from '@/core/javaFetch';
import {alertController} from '@/core/controllers/alertController';
import {ChangelogData} from '@/components/templates/changelogs/Changelog.vue';
import UiButton from '@/components/core/ui/UiButton.vue';
import ChangelogEntry from '@/components/templates/changelogs/ChangelogEntry.vue';
import {constants} from '@/core/constants';

type ChangelogList = {
  hash: string;
  versions: string[];
}

@Component({
  components: {ChangelogEntry, UiButton}
})
export default class Changelogs extends Vue {
  shift = 0;
  perShift = 10;
  changelogs: string[] = [];
  changelogData: Record<string, ChangelogData> = {};
  
  error = ""
  
  async mounted() {
    await this.getChangelogs();
  }
  
  async getChangelogs() {
    try {
      const changelogNames = await JavaFetch.create(`${constants.metaApi}/v1/changelogs/app`).execute();
      if (!changelogNames) {
        alertController.error('Failed to load changelogs');
        return;
      }
      
      const changelogNamesData = changelogNames?.json<ChangelogList>();
      if (!changelogNamesData || !changelogNamesData.versions) {
        alertController.error('Failed to load changelogs');
        return;
      }
      
      this.changelogs = changelogNamesData.versions.reverse();
      await this.loadInView();
    } catch (e) {
      console.error(e);
      this.error = 'Failed to load changelogs';
    }
  }
  
  async loadInView() {
    const changelogs = this.changelogs.slice(this.shift, this.shift + this.perShift);
    for (const changelog of changelogs) {
      await this.loadChangelog(changelog);
    }
  }
  
  async loadMore() {
    this.shift += this.perShift;
    if (this.shift >= this.changelogs.length) {
      return;
    }
    await this.loadInView();
  }
  
  async loadChangelog(id: string) {
    try {
      const changelog = await JavaFetch.create(`${constants.metaApi}/v1/changelogs/app/${id}`).execute();
      if (!changelog) {
        alertController.error('Failed to load changelog');
        return;
      }
      
      const changelogData = changelog?.json<ChangelogData>();
      if (!changelogData) {
        alertController.error('Failed to load changelog');
        return;
      }
      
      // Force vue to update
      Vue.set(this.changelogData, id, changelogData);
    } catch (e) {
      console.error(e);
      alertController.error('Failed to load changelog');
      return;
    }
  }
  
  get allLogs() {
    return Object.values(this.changelogData);
  }
  
  get allLoaded() {
    return this.changelogs.length === Object.keys(this.changelogData).length;
  }
}
</script>

<style lang="scss" scoped>

</style>