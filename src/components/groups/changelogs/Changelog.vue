<script lang="ts" setup>
import appPlatform from '@platform';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {constants} from '@/core/constants';
import ChangelogEntry from '@/components/groups/changelogs/ChangelogEntry.vue';
import {createLogger} from '@/core/logger';
import { onMounted, onUnmounted, ref, watch } from 'vue';
import {Modal, UiMessage} from '@/components/ui';
import { useWsStore } from '@/store/wsStore.ts';
import { useAppStore } from '@/store/appStore.ts';
import { alertController } from '@/core/controllers/alertController.ts';

const wsStore = useWsStore();
const appStore = useAppStore();
const logger = createLogger("Changelog.vue")

const changelogData = ref<ChangelogData | null>(null);

onMounted(() => {
  appStore.emitter.on("action/force-changelog-open", forceChangelog);
})

onUnmounted(() => {
  appStore.emitter.off("action/force-changelog-open", forceChangelog);
})

function forceChangelog(version: string) {
  showChangelog(version, true).catch(console.error)
}

watch(() => wsStore.ready, async (newValue) => {
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
    await showChangelog(currentVersion)
  } else {
    logger.debug('No changelog to show, already seen it')
  }
}

async function showChangelog(currentVersion: string, forced = false) {
  try {
    const changelogsReq = await fetch(`${constants.metaApi}/changelogs/app`);
    const changelogs = await changelogsReq.json();

    if (changelogs?.versions?.includes(currentVersion)) {
      const changelogReq = await fetch(
        `${constants.metaApi}/changelogs/app/${currentVersion}`,
      );

      changelogData.value = await changelogReq.json();

      if (forced) {
        return;
      }
      
      // Attempt to update the lastVersion to prevent the modal showing again
      await sendMessage("storage.put", {
        key: 'lastVersion',
        value: currentVersion,
      })
    } else {
      if (forced) {
        alertController.error(`No changelog available for version ${currentVersion}`);
      }
    }
  } catch (e) {
    logger.debug('caught error', e);
    // Stop here, don't do anything, something is wrong, we'll try again next launch.
    return;
  }
}

function getCurrentVersion() {
    return appPlatform.config.version;
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
    <UiMessage v-else type="danger">
      Unable to find any changelog data... 
    </UiMessage>
  </Modal>
</template>