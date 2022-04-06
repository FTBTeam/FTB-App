<template>
  <ftb-modal :visible="changelogData" size="large">
    <div class="title"></div>
  </ftb-modal>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Action } from 'vuex-class';
import { wsTimeoutWrapper } from '@/utils/helpers';
import FTBModal from '@/components/atoms/FTBModal.vue';

export type ChangelogEntry = {
  version: string;
  title: string;
  head: string;
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

  changelogData = null;

  mounted() {
    setTimeout(() => {
      this.checkForUpdate().catch((e) => {
        // Todo: soft crash
        console.log(e);
      });
    }, 5_000);
  }

  async checkForUpdate() {
    const data = await wsTimeoutWrapper({
      type: 'storage.get',
      key: 'lastVersion',
    });

    // No held last version meaning we should find a changelog
    // if (!data.response || data.response !== this.getCurrentVersion()) {
    console.log('No last version');

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
    // } else {
    //   console.log(data.response);
    // }
  }

  // Some magic to get the current version
  getCurrentVersion() {
    return '1.0.0';
  }
}
</script>

<style lang="scss" scoped></style>
