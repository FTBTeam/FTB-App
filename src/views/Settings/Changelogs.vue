<script lang="ts" setup>
import {JavaFetch} from '@/core/javaFetch';
import {alertController} from '@/core/controllers/alertController';
import {ChangelogData} from '@/components/groups/changelogs/Changelog.vue';
import { UiButton, Message } from '@/components/ui';
import ChangelogEntry from '@/components/groups/changelogs/ChangelogEntry.vue';
import {constants} from '@/core/constants';
import { computed, onMounted, ref } from 'vue';
import { toggleBeforeAndAfter } from '@/utils/helpers/asyncHelpers.ts';

type ChangelogList = {
  hash: string;
  versions: string[];
}

const shift = ref(0);
const perShift = ref(10);
const changelogs = ref<string[]>([]);
const changelogData = ref<Record<string, ChangelogData>>({});
const loading = ref(false);
const error = ref("")

onMounted(() => {
  toggleBeforeAndAfter(getChangelogs, v => loading.value = v);
})

async function getChangelogs() {
  try {
    const changelogNames = await JavaFetch.create(`${constants.metaApi}/changelogs/app`).execute();
    if (!changelogNames) {
      alertController.error('Failed to load changelogs');
      return;
    }

    const changelogNamesData = changelogNames?.json<ChangelogList>();
    if (!changelogNamesData || !changelogNamesData.versions) {
      alertController.error('Failed to load changelogs');
      return;
    }

    changelogs.value = changelogNamesData.versions.reverse();
    await loadInView();
  } catch (e) {
    console.error(e);
    error.value = 'Failed to load changelogs';
  }
}

async function loadInView() {
  const logs = changelogs.value.slice(shift.value, shift.value + perShift.value);
  for (const changelog of logs) {
    await loadChangelog(changelog);
  }
}

async function loadMore() {
  shift.value += perShift.value;
  if (shift.value >= changelogs.value.length) {
    return;
  }
  await loadInView();
}

async function loadChangelog(id: string) {
  try {
    const changelog = await JavaFetch.create(`${constants.metaApi}/changelogs/app/${id}`).execute();
    if (!changelog) {
      alertController.error('Failed to load changelog');
      return;
    }

    const moreChangelogData = changelog?.json<ChangelogData>();
    if (!moreChangelogData) {
      alertController.error('Failed to load changelog');
      return;
    }
    
    changelogData.value[id] = moreChangelogData;
  } catch (e) {
    console.error(e);
    alertController.error('Failed to load changelog');
    return;
  }
}

const allLogs = computed(() => Object.values(changelogData.value));
const allLoaded = computed(() => changelogs.value.length === Object.keys(changelogData.value).length);
</script>

<template>
  <div class="changelogs" v-if="!error">
    <div class="changelogs">
      <div class="log mb-8" v-for="(changelog, index) in allLogs">
        <h3 class="mb-4 text-xl font-bold">{{changelog.version}}{{ changelog.title != changelog.version ? ` - ${changelog.title}` : "" }}</h3>
        <ChangelogEntry :key="index" :changelog="changelog" />
      </div>
    </div>
    
    <div class="flex justify-center">
      <ui-button type="info" @click="loadMore" v-if="!allLoaded">Load more</ui-button>
    </div>
  </div>
  <div v-else>
    <Message type="warning">
      <p>{{error}}</p>
    </Message>
  </div>
</template>