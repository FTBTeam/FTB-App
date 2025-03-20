<script lang="ts" setup>
import platform from '@/utils/interface/electron-overwolf';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {constants} from '@/core/constants';
import ChangelogEntry from '@/components/groups/changelogs/ChangelogEntry.vue';
import {createLogger} from '@/core/logger';
import { ref, watch } from 'vue';
import {Modal, Message} from '@/components/ui';
import { useAppStore } from '@/store/appStore.ts';

const appStore = useAppStore();
const logger = createLogger("Changelog.vue")

const changelogData = ref<ChangelogData | null>(null);

watch(() => appStore.ready, async (newValue) => {
  if (!newValue) return;

  await checkForUpdate().catch((e) => {
    logger.error('Unable to find any changelog data, maybe the servers down?', e);
  });
})

async function checkForUpdate() {
  logger.info("Checking for changelog")
  const data = await sendMessage("storage.get", {
    key: 'lastVersion'
  })

  const currentVersion = getCurrentVersion();
  logger.dd('currentVersion', currentVersion)
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

        changelogData.value = await changelogReq.json();

        // Attempt to update the lastVersion to prevent the modal showing again
        await sendMessage("storage.put", {
          key: 'lastVersion',
          value: currentVersion,
        })
      }
    } catch (e) {
      logger.debug('caught error', e);
      // Stop here, don't do anything, something is wrong, we'll try again next launch.
      return;
    }
  } else {
    logger.debug('No changelog to show, already seen it')
  }
}

function getCurrentVersion() {
    return platform.get.config.version;
}
</script>

<script lang="ts">
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
</script>

<template>
  <Modal
    v-if="changelogData"
    :open="!!changelogData"
    title="Latest updates"
    :subTitle="changelogData.title"
    size="medium"
    class="wysiwyg select-text"
    @closed="changelogData = null"
  >
    <changelog-entry v-if="changelogData" :use-extended="true" :changelog="changelogData" />
    <Message v-else type="danger">
      Unable to find any changelog data... 
    </Message>
  </Modal>
</template>