<script lang="ts" setup>
import {Loader, Message, UiBadge} from '@/components/ui';
import {BlogPost} from '@/core/types/external/metaApi.types';
import {standardDate} from '@/utils/helpers/dateHelpers';
import {constants} from '@/core/constants';
import {createLogger} from '@/core/logger';
import { onMounted, ref } from 'vue';
import { JavaFetch } from '@/core/javaFetch.ts';
import { toggleBeforeAndAfter } from '@/utils/helpers/asyncHelpers.ts';
import { safeLinkOpen } from '@/utils';

const logger = createLogger("blog.vue");
const loading = ref(false);
const news = ref<BlogPost[]>([]);

onMounted(async () => {
  const storeKey = "news";

  if (localStorage.getItem(storeKey)) {
    const data = JSON.parse(localStorage.getItem(storeKey) || "{}");
    if (data.posts) {
      // Check if the data we hold is up-to-date enough
      if (Date.now() - data.storedAt < 1000 * 60 * 10) { // 10 minutes
        news.value = data.posts;
        return;
      }
    }
  }
  
  // Otherwise, fetch the news
  const newsRes = await toggleBeforeAndAfter(async () => {
    try {
      const newsReq = await JavaFetch.create(`${constants.metaApi}/blog/posts`)
        .execute();

      if (!newsReq) {
        return null;
      }

      return newsReq.json<{posts: BlogPost[]}>();
    } catch (e) {
      logger.error("Failed to load news", e);
      return null;
    }
  }, v => loading.value = v);
  
  if (newsRes) {
    localStorage.setItem(storeKey, JSON.stringify({
      posts: newsRes.posts,
      storedAt: Date.now()
    }));
    
    news.value = newsRes.posts;
  }
})

const domain = constants.ftbDomain;
</script>

<template>
  <div class="px-6 py-4" v-if="!loading">
    <template v-if="news.length">
      <h2 class="text-lg font-bold mb-6">Get the latest news from FTB</h2>
      <div class="grid xl:grid-cols-2 gap-6">
        <div class="news-item" v-for="(newsItem, index) in news" :key="index">
          <a :href="`${domain}/blog/p/${newsItem.slug}`" @click="safeLinkOpen" class="feature-image mb-4 block" :style="`background-image: url(${newsItem.feature_image ?? 'https://cdn.feed-the-beast.com/assets/blog/headers/placeholder-1.png'})`"></a>
          <div class="flex flex-wrap gap-2 mb-4">
            <a v-for="(tag, index) in newsItem.tags" :key="index" :href="`${domain}/blog/t/${tag.slug}`" @click="safeLinkOpen">
              <UiBadge>
                {{ tag.name }}
              </UiBadge>
            </a>
            <UiBadge v-if="newsItem.published_at">
              {{ standardDate(newsItem.published_at) }}
            </UiBadge>
          </div>
          
          <div class="about">
            <a :href="`${domain}/blog/p/${newsItem.slug}`" @click="safeLinkOpen" class="title block mb-2 font-bold text-2xl">{{ newsItem.title }}</a>
            <p class="mb-4 max-lines-4">{{ newsItem.custom_excerpt ? newsItem.custom_excerpt : newsItem.excerpt }}</p>
          </div>
        </div>
      </div>
    </template>
    <div v-else>
      <h2 class="text-lg font-bold mb-6">Oh no... 🔥 Something's not right!</h2>
      <Message type="warning">
        <p>Something went wrong while loading the news.</p>
        <p class="mb-4">Please try again later.</p>
        
        <b>You can find our latest blog posts on our <a :href="`${domain}/blog`" @click="safeLinkOpen">website</a></b>
      </Message>
    </div>
  </div>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full" v-else>
    <loader />
  </div>
</template>

<style scoped lang="scss">
.news-item {
  .feature-image {
    width: 100%;
    height: 190px;
    margin-bottom: 1rem;
    display: block;
    position: relative;
    background-size: cover;
    background-position: center center;
    border-radius: var(--border-radius);
  }
  
  img {
    transition: transform 0.25s ease-in-out;
  }
  
  &:hover img {
    transform: scale(1.008);
  }
}

.max-lines-4 {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
