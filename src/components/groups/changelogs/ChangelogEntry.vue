<script lang="ts" setup>
import {parseMarkdown} from '@/utils';
import {ChangelogData} from '@/components/groups/changelogs/Changelog.vue';
import {JavaFetch} from '@/core/javaFetch';
import {constants} from '@/core/constants';
import NestedChangelogEntry from '@/components/groups/changelogs/ChangelogEntry.vue';
import { onMounted, watch, ref, computed } from 'vue';
import { toTitleCase } from '@/utils/helpers/stringHelpers.ts';

const {
  changelog,
  useExtended = false,
  showVersion = false,
} = defineProps<{
  changelog: ChangelogData;
  useExtended?: boolean;
  showVersion?: boolean;
}>()

const extendedLogs = ref<ChangelogData[]>([]);
const loadingExtendedLogs = ref(false);

onMounted(() => {
  loadExtended()
    .catch((e) => {
      console.error('Failed to load extended changelogs', e);
    });
})

watch(() => useExtended, (newValue, oldValue) => {
  if (newValue === oldValue) return;
  loadExtended()
    .catch((e) => {
      console.error('Failed to load extended changelogs', e);
    });
})

async function loadExtended() {
  if (!changelog) return;
  if (!useExtended) return;

  if (!changelog.extends) return;

  await loadExtendedChangelogs();
}

const headings = {
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

function markdown(changeList: string[]) {
  let markdown = '';

  for (const change of changeList) {
    if (change.startsWith('-')) {
      markdown += change;
    } else {
      markdown += `- ${change}\n`;
    }
  }

  return parseMarkdown(markdown);
}

async function loadExtendedChangelogs() {
  loadingExtendedLogs.value = true;

  for (const version of changelog.extends ?? []) {
    const request = await JavaFetch.create(`${constants.metaApi}/changelogs/app/${version}`).execute();
    const changelog = request?.json<ChangelogData>();

    if (changelog) extendedLogs.value.push(changelog);
  }

  loadingExtendedLogs.value = false;
}

const headingImage = computed(() => {
  const image = changelog?.media?.find((e) => e.heading && e.type === 'image');
  return image ? image.source : null;
})
</script>

<template>
  <div class="changelogEntry wysiwyg select-text">
    <h2 v-if="showVersion">{{changelog.version}}{{ changelog.title != changelog.version ? ` ${changelog.title}` : "" }}</h2>
    <img :src="headingImage" class="heading-image" alt="Heading image" v-if="headingImage" />
    <div v-if="changelog.header" v-html="parseMarkdown(changelog.header)" />

    <template v-for="(heading, key) in headings">
      <template v-if="changelog.changes[key]">
        <h3 :style="{ color: heading.color, 'word-spacing': '.5rem' }">{{ toTitleCase(heading.heading) }}</h3>
        <div v-html="markdown(changelog.changes[key])" />
      </template>
    </template>

    <p class="mt-4" v-if="changelog.footer" v-html="parseMarkdown(changelog.footer)" />
    
    <div class="extendedLogs mt-4" v-if="useExtended && extendedLogs.length">
      <h3>Previously updated</h3>
      <NestedChangelogEntry class="mb-4" v-for="log in extendedLogs" :key="log.version" :changelog="log" :use-extended="false" :show-version="true" />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.heading-image {
  display: block;
  margin-bottom: 1rem;
  border-radius: 5px;
}
</style>