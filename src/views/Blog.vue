<script lang="ts" setup>
import Loader from '@/components/ui/Loader.vue';
import {ns} from '@/core/state/appState';
import {AsyncFunction} from '@/core/types/commonTypes';
import {BlogPost} from '@/core/types/external/metaApi.types';
import dayjs from 'dayjs';
import {standardDateTime} from '@/utils/helpers/dateHelpers';
import {constants} from '@/core/constants';
import {createLogger} from '@/core/logger';
import { onMounted, ref } from 'vue';
import { JavaFetch } from '@/core/javaFetch.ts';
import { toggleBeforeAndAfter } from '@/utils/helpers/asyncHelpers.ts';

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
      <div class="news-item" v-for="(newsItem, index) in news" :key="index">
        <a :href="`${domain}/blog/p/${newsItem.slug}`" @click="openExternal" v-if="newsItem.feature_image" class="feature-image mb-4 block">
          <img class="rounded shadow-xl" :src="newsItem.feature_image" alt="Feature image">
        </a>
        
        <div class="about">
          <a :href="`${domain}/blog/p/${newsItem.slug}`" @click="openExternal" class="title block mb-2 font-bold text-lg">{{ newsItem.title }}</a>
          <p class="mb-4 max-lines-4">{{ newsItem.custom_excerpt ? newsItem.custom_excerpt : newsItem.excerpt }}</p>
        </div>
        
        <div class="author-and-info flex items-center gap-4">
          <img class="avatar rounded shadow-xl" crossorigin="anonymous" width="50" :src="newsItem.primary_author.profile_image" alt="Avatar" />  
          <div class="info">
            <b class="opacity-75">{{ newsItem.primary_author.name }}</b>
            <div class="info opacity-75 text-sm" :title="standardDateTime(newsItem.published_at)">
              {{ dayjs(newsItem.published_at).fromNow() }}
            </div>
          </div>
        </div>
        <hr class="my-8 border-white border-opacity-25"/>
      </div>
    </template>
    <div v-else>
      <h2 class="text-lg font-bold mb-6">Oh no... 🔥 Something's not right!</h2>
      <message type="warning">
        <p>Something went wrong while loading the news.</p>
        <p class="mb-4">Please try again later.</p>
        
        <b>You can find our latest blog posts on our <a :href="`${domain}/blog`" @click="openExternal">website</a></b>
      </message>
    </div>
  </div>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full" v-else>
    <loader />
  </div>
</template>

<style scoped lang="scss">
.news-item {
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
