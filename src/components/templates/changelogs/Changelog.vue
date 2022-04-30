<template>
  <modal
    v-if="changelogData"
    :open="changelogData"
    title="Latest updates"
    :subTitle="changelogData.title"
    size="medium"
    class="wysiwyg"
    @closed="changelogData = null"
  >
    <img :src="headingImage" class="heading-image" alt="Heading image" v-if="headingImage" />
    <p v-if="changelogData.header">
      <vue-showdown>
        {{ changelogData.header }}
      </vue-showdown>
    </p>

    <template v-for="(heading, key) in headings">
      <template v-if="changelogData.changes[key]">
        <h3 :style="{ color: heading.color, 'word-spacing': '.5rem' }">{{ heading.heading | title }}</h3>
        <vue-showdown :markdown="changelogData.changes[key].map((e) => `- ${e}`).join('\n')" />
      </template>
    </template>

    <p v-if="changelogData.footer" class="mt-4">
      <vue-showdown>
        {{ changelogData.footer }}
      </vue-showdown>
    </p>

    <!--    <template #footer></template>-->
  </modal>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Action } from 'vuex-class';
import { wsTimeoutWrapper } from '@/utils/helpers';
import FTBModal from '@/components/atoms/FTBModal.vue';
import platform from '@/utils/interface/electron-overwolf';
import EventBus from '@/utils/event-bus';

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

@Component({
  components: {
    'ftb-modal': FTBModal,
  },
})
export default class Changelog extends Vue {
  @Action('sendMessage') public sendMessage: any;

  changelogData: ChangelogEntry | null = null;

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

  mounted() {
    setTimeout(() => {
      this.checkForUpdate().catch((e) => {
        console.log('Unable to find any changelog data, maybe the servers down?', e);
      });
    }, 2_000);

    EventBus.$on('changelog-open', (data: any) => {
      console.log('changelog open', data);
    });
  }

  destroyed() {
    EventBus.$off('changelog-open');
  }

  async checkForUpdate() {
    const data = await wsTimeoutWrapper({
      type: 'storage.get',
      key: 'lastVersion',
    });

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
          await wsTimeoutWrapper({
            type: 'storage.put',
            key: 'lastVersion',
            value: this.getCurrentVersion(),
          });
        }
      } catch (e) {
        console.error('caught error', e);
        // Stop here, don't do anything, something is wrong, we'll try again next launch.
        return;
      }
    } else {
      console.error('Error reading response from version call', data.response);
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
