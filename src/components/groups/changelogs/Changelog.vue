<template>
  <modal
    v-if="changelogData"
    :open="!!changelogData"
    title="Latest updates"
    :subTitle="changelogData.title"
    size="medium"
    class="wysiwyg select-text"
    @closed="changelogData = null"
  >
    <changelog-entry v-if="changelogData" :use-extended="true" :changelog="changelogData" />
    <message v-else type="danger">
      Unable to find any changelog data... 
    </message>
  </modal>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {State} from 'vuex-class';
import platform from '@/utils/interface/electron-overwolf';
import {SocketState} from '@/modules/websocket/types';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {constants} from '@/core/constants';
import ChangelogEntry from '@/components/groups/changelogs/ChangelogEntry.vue';
import {createLogger} from '@/core/logger';
import {waitForWebsockets} from '@/utils';

export type ChangelogData = {
  version: string;
  title: string;
  header: string;
  footer: string;
  released: string;
  media?: {
    type: 'image' | 'video';
    source: string;
    heading?: boolean;
  }[];
  changes: {
    added: string[];
    changed: string[];
    fixed: string[];
    removed: string[];
  };
  extends?: string[]
};

@Component({
  components: {ChangelogEntry}
})
export default class Changelog extends Vue  {
  @State('websocket') public websockets!: SocketState;
  private logger = createLogger("Changelog.vue")
  
  changelogData: ChangelogData | null = null;

  async mounted() {
    await waitForWebsockets("changelog", this.websockets.socket)

    this.checkForUpdate().catch((e) => {
      this.logger.error('Unable to find any changelog data, maybe the servers down?', e);
    });
  }

  async checkForUpdate() {
    this.logger.info("Checking for changelog")
    const data = await sendMessage("storage.get", {
      key: 'lastVersion'
    })

    const currentVersion = this.getCurrentVersion();
    this.logger.dd('currentVersion', currentVersion)
    const lastVersion = data.response;
    
    // No held last version meaning we should find a changelog
    if (!lastVersion || lastVersion !== currentVersion) {
      // Get the available versions
      try {
        const changelogsReq = await fetch(`${constants.metaApi}/changelogs/app`);
        const changelogs = await changelogsReq.json();

        if (changelogs?.versions?.includes(currentVersion)) {
          const changelogReq = await fetch(
            `${constants.metaApi}/changelogs/app/${currentVersion}`,
          );

          this.changelogData = await changelogReq.json();

          // Attempt to update the lastVersion to prevent the modal showing again
          await sendMessage("storage.put", {
            key: 'lastVersion',
            value: currentVersion,
          })
        }
      } catch (e) {
        this.logger.debug('caught error', e);
        // Stop here, don't do anything, something is wrong, we'll try again next launch.
        return;
      }
    } else {
      this.logger.debug('No changelog to show, already seen it')
    }
  }

  // Some magic to get the current version
  getCurrentVersion() {
    return platform.get.config.version;
  }
}
</script>

<style lang="scss" scoped>

</style>
