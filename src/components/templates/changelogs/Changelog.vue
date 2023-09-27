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
    <img :src="headingImage" class="heading-image" alt="Heading image" v-if="headingImage" />
    <div v-if="changelogData.header" v-html="parseMarkdown(changelogData.header)" />

    <template v-for="(heading, key) in headings">
      <template v-if="changelogData.changes[key]">
        <h3 :style="{ color: heading.color, 'word-spacing': '.5rem' }">{{ heading.heading | title }}</h3>
        <div v-html="parseMarkdown(changelogData.changes[key].map((e) => `- ${e}`).join('\n'))" />
      </template>
    </template>
    
    <p class="mt-4" v-if="changelogData.footer" v-html="parseMarkdown(changelogData.footer)" />
  </modal>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {State} from 'vuex-class';
import {parseMarkdown} from '@/utils/helpers';
import platform from '@/utils/interface/electron-overwolf';
import {SocketState} from '@/modules/websocket/types';
import { sendMessage } from '@/core/websockets/websocketsApi';

export type ChangelogEntry = {
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
};

@Component
export default class Changelog extends Vue {
  @State('websocket') public websockets!: SocketState;

  changelogData: ChangelogEntry | null = null;
  parseMarkdown = parseMarkdown;

  headings = {
    added: {
      heading: '🎉 Added',
      color: '#53cb6a',
    },
    changed: {
      heading: '🔧 Changed',
      color: '#fcae21',
    },
    fixed: {
      heading: '🐞 Fixed',
      color: '#ff5e5e',
    },
    removed: {
      heading: '🗑 Removed',
      color: 'inherit',
    },
  };

  checkIntervalRef: number | null = null;
  
  mounted() {
    // TODO: All of this should be part of an initial handshake with the backend
    // Check if the websockets is connected each second. If it is, get the changelog
    this.checkIntervalRef = setInterval(() => {
      if (this.websockets.socket.isConnected) {
        if (this.checkIntervalRef) {
          clearInterval(this.checkIntervalRef);
        }
        
        this.checkForUpdate().catch((e) => {
          console.log('Unable to find any changelog data, maybe the servers down?', e);
        });
      }
    }, 1_000) as unknown as number; // Once a second
  }

  async checkForUpdate() {
    const data = await sendMessage("storage.get", {
      key: 'lastVersion'
    })

    // No held last version meaning we should find a changelog
    if (!data.response || data.response !== this.getCurrentVersion()) {
      // Get the available versions
      try {
        const changelogsReq = await fetch(`${process.env.VUE_APP_META_API}/v1/changelogs/app`);
        const changelogs = await changelogsReq.json();

        if (changelogs?.versions?.includes(this.getCurrentVersion())) {
          const changelogReq = await fetch(
            `${process.env.VUE_APP_META_API}/v1/changelogs/app/${this.getCurrentVersion()}`,
          );

          this.changelogData = await changelogReq.json();

          // Attempt to update the lastVersion to prevent the modal showing again
          await sendMessage("storage.put", {
            key: 'lastVersion',
            value: this.getCurrentVersion(),
          })
        }
      } catch (e) {
        console.error('caught error', e);
        // Stop here, don't do anything, something is wrong, we'll try again next launch.
        return;
      }
    }
  }

  get headingImage() {
    const image = this.changelogData?.media?.find((e) => e.heading && e.type === 'image');
    return image ? image.source : null;
  }

  // Some magic to get the current version
  getCurrentVersion() {
    return platform.get.config.publicVersion;
  }
}
</script>

<style lang="scss" scoped>
.heading-image {
  display: block;
  margin-bottom: 1rem;
  border-radius: 5px;
}
</style>
