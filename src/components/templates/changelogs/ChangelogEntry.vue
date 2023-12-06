<template>
  <div class="changelogEntry wysiwyg select-text">
    <h2 v-if="showVersion">{{ changelog.title ?? changelog.version }}</h2>
    <img :src="headingImage" class="heading-image" alt="Heading image" v-if="headingImage" />
    <div v-if="changelog.header" v-html="parseMarkdown(changelog.header)" />

    <template v-for="(heading, key) in headings">
      <template v-if="changelog.changes[key]">
        <h3 :style="{ color: heading.color, 'word-spacing': '.5rem' }">{{ heading.heading | title }}</h3>
        <div v-html="markdown(changelog.changes[key])" />
      </template>
    </template>

    <p class="mt-4" v-if="changelog.footer" v-html="parseMarkdown(changelog.footer)" />
    
    <div class="extendedLogs mt-4" v-if="useExtended && extendedLogs.length">
      <h3>Previously updated</h3>
      <changelog-entry class="mb-4" v-for="log in extendedLogs" :key="log.version" :changelog="log" :use-extended="false" :show-version="true" />
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Prop, Vue, Watch} from 'vue-property-decorator';
import {parseMarkdown} from '@/utils';
import {ChangelogData} from '@/components/templates/changelogs/Changelog.vue';
import {JavaFetch} from '@/core/javaFetch';
import {constants} from '@/core/constants';

@Component({
  components: {ChangelogEntry},
  methods: {parseMarkdown}
})
export default class ChangelogEntry extends Vue {
  @Prop() changelog!: ChangelogData;
  @Prop({default: false}) useExtended!: boolean;
  @Prop({default: false}) showVersion!: boolean;
  
  extendedLogs: ChangelogData[] = [];
  loadingExtendedLogs = false;
  
  async mounted() {
    await this.loadExtended();
  }
  
  @Watch('useExtended')
  async onUseExtendedChanged(value: boolean, oldValue: boolean) {
    if (value === oldValue) return;
    await this.loadExtended();
  }
  
  async loadExtended() {
    if (!this.changelog) return;
    if (!this.useExtended) return;
    
    if (!this.changelog.extends) return;

    await this.loadExtendedChangelogs();
  }

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
  
  markdown(changeList: string[]) {
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

  async loadExtendedChangelogs() {
    this.loadingExtendedLogs = true;
    
    for (const version of this.changelog.extends ?? []) {
      const request = await JavaFetch.create(`${constants.metaApi}/v1/changelogs/app/${version}`).execute();
      const changelog = request?.json<ChangelogData>();
      
      if (changelog) this.extendedLogs.push(changelog);
    }
    
    this.loadingExtendedLogs = false;
  }
  
  get headingImage() {
    const image = this.changelog?.media?.find((e) => e.heading && e.type === 'image');
    return image ? image.source : null;
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