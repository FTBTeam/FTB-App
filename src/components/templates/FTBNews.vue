<template>
  <div class="news-item mb-8">
    <header class="mb-4 flex">
      <div class="title">
        <a @click="openExternal" :href="newsItem.url" class="text-2xl font-bold mb-1 leading-none cursor-pointer block">{{ newsItem.title }}</a>
        <small class="text-sm text-gray-400">Created {{ newsItem.createdAt | dayjsFromNow }}</small>
      </div>
      <aside>
        <span class="text-2xl font-bold" :title="newsItem.views.toLocaleString()">{{prettyNumber(newsItem.views)}}</span>
        <span class="block">Views</span>
      </aside>
    </header>
    <div class="post-body">
      <div class="post-contents-raw-html" v-html="newsItem.postData" />
      <a @click="openExternal" :href="newsItem.url" class="text-gray-400 hover:text-white leading-none cursor-pointer hover:underline block">
        Continue Reading...
      </a>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import {NewsItem} from '@/modules/news/news.types';
import {prettyNumber} from '@/utils';

@Component({
  methods: {prettyNumber}
})
export default class FTBNews extends Vue {
  @Prop() newsItem!: NewsItem;
}
</script>

<style lang="scss" scoped>
.post {
  img {
    max-width: 100%;
    height: auto;
    border-radius: 5px;
  }
}

.news-item {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol';
  
  header {
    align-items: center;
    gap: 2rem;
    
    .title {
      flex: 1;
    }
    
    aside {
      text-align: right;
    }
  }
  
  .post-body {
    background-color: var(--color-background-lighten);
    border-radius: 5px;
    padding: 1.5rem;
  }
  
  .post-contents-raw-html {
    margin-bottom: 1rem;
  }
}
</style>
