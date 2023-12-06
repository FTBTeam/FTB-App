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
import {consoleBadButNoLogger} from '@/utils/helpers';
import platform from '@/utils/interface/electron-overwolf';
import {SocketState} from '@/modules/websocket/types';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {constants} from '@/core/constants';
import ChangelogEntry from '@/components/templates/changelogs/ChangelogEntry.vue';

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
export default class Changelog extends Vue {
  @State('websocket') public websockets!: SocketState;

  changelogData: ChangelogData | null = null;
  checkIntervalRef: number | null = null;
  
  mounted() {
    // TODO: (legacy) All of this should be part of an initial handshake with the backend
    // Check if the websockets is connected each second. If it is, get the changelog
    this.checkIntervalRef = setInterval(() => {
      if (this.websockets.socket.isConnected) {
        if (this.checkIntervalRef) {
          clearInterval(this.checkIntervalRef);
        }
        
        this.checkForUpdate().catch((e) => {
          consoleBadButNoLogger("E", 'Unable to find any changelog data, maybe the servers down?', e);
        });
      }
    }, 1_000) as unknown as number; // Once a second
  }

  async checkForUpdate() {
    const data = await sendMessage("storage.get", {
      key: 'lastVersion'
    })

    const currentVersion = this.getCurrentVersion();
    const lastVersion = data.response;
    
    // No held last version meaning we should find a changelog
    if (!lastVersion || lastVersion !== currentVersion) {
      // Get the available versions
      try {
        const changelogsReq = await fetch(`${constants.metaApi}/v1/changelogs/app`);
        const changelogs = await changelogsReq.json();

        if (changelogs?.versions?.includes(currentVersion)) {
          const changelogReq = await fetch(
            `${constants.metaApi}/v1/changelogs/app/${currentVersion}`,
          );

          this.changelogData = await changelogReq.json();

          // Attempt to update the lastVersion to prevent the modal showing again
          await sendMessage("storage.put", {
            key: 'lastVersion',
            value: currentVersion,
          })
        }
      } catch (e) {
        consoleBadButNoLogger("E", 'caught error', e);
        // Stop here, don't do anything, something is wrong, we'll try again next launch.
        return;
      }
    } else {
      consoleBadButNoLogger("D", 'No changelog to show, already seen it')
    }
  }

  // Some magic to get the current version
  getCurrentVersion() {
    return platform.get.config.publicVersion;
  }
}
</script>

<style lang="scss" scoped>

</style>
