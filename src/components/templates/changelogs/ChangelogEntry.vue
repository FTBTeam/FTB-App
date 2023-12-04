<template>
  <div class="changelogEntry wysiwyg">
    <img :src="headingImage" class="heading-image" alt="Heading image" v-if="headingImage" />
    <div v-if="changelog.header" v-html="parseMarkdown(changelog.header)" />

    <template v-for="(heading, key) in headings">
      <template v-if="changelog.changes[key]">
        <h3 :style="{ color: heading.color, 'word-spacing': '.5rem' }">{{ heading.heading | title }}</h3>
        <div v-html="markdown(changelog.changes[key])" />
      </template>
    </template>

    <p class="mt-4" v-if="changelog.footer" v-html="parseMarkdown(changelog.footer)" />
  </div>
</template>

<script lang="ts">
import {Component, Prop, Vue} from 'vue-property-decorator';
import {parseMarkdown} from '@/utils';
import {ChangelogData} from '@/components/templates/changelogs/Changelog.vue';

@Component({
  methods: {parseMarkdown}
})
export default class ChangelogEntry extends Vue {
  @Prop() changelog!: ChangelogData;

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