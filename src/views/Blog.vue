<script lang="ts" setup>
import {Loader, Message, UiBadge} from '@/components/ui';
import {BlogPost, Pagination as BlogPagination} from '@/core/types/external/metaApi.types';
import {standardDate} from '@/utils/helpers/dateHelpers';
import {constants} from '@/core/constants';
import {onMounted, ref, watch} from 'vue';
import { toggleBeforeAndAfter } from '@/utils/helpers/asyncHelpers.ts';
import { safeLinkOpen } from '@/utils';
import {blogController} from "@/core/controllers/blogController.ts";
import Pagination from "@/components/ui/Pagination.vue"

const loading = ref(false);
const news = ref<BlogPost[]>([]);
const page = ref(1);
const pagination = ref<BlogPagination | null>(null);

onMounted(async () => {
  loadPage(page.value)
    .catch(() => {});
})

async function loadPage(pageNumber: number) {
  await toggleBeforeAndAfter(async () => {
    const posts = await blogController.getPosts(pageNumber);
    if (!posts) {
      return;
    }
    
    news.value = posts.posts;
    pagination.value = posts.pagination;
  }, v => loading.value = v);
}

watch(page, (newPage) => {
  document.querySelector(".app-content")?.scrollTo(0, 0);
  loadPage(newPage)
    .catch(() => {});
});

const domain = constants.ftbDomain;
</script>

<template>
  <div class="px-6 py-4">
    <div class="heading-image absolute left-0 top-0 w-full h-50" :style="`background-image: url(https://cdn.feed-the-beast.com/assets/website/headers/autumn-26.webp)`"></div>
    <div class="h-[150px] flex flex-col items-center justify-center z-10 relative">
        <h1 class="text-4xl font-black mb-2">Blog</h1>
        <p class="text-lg">Get the latest updates from the FTB Team</p>
    </div>
    
    <template v-if="news.length">
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
      
      <div class="mt-6 flex justify-center">
        <Pagination v-if="pagination" v-model="page" :per-page="pagination.limit" :total="pagination.total" />
      </div>
    </template>

    <loader v-if="loading" />
    
    <div v-else-if="!news.length">
      <h2 class="text-lg font-bold mb-6">Oh no... 🔥 Something's not right!</h2>
      <Message type="warning">
        <p>Something went wrong while loading the news.</p>
        <p class="mb-4">Please try again later.</p>
        
        <b>You can find our latest blog posts on our <a :href="`${domain}/blog`" @click="safeLinkOpen">website</a></b>
      </Message>
    </div>
  </div>
</template>

<style scoped lang="scss">
.heading-image {
  background-size: cover;
  background-position: center center;
  
  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(to top, #2a2a2a, transparent);
  }
}

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
