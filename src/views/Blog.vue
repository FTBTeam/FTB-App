<template>
  <div class="px-6 py-4" v-if="!loading">
    <template v-if="news.length">
      <h2 class="text-lg font-bold mb-6">Get the latest news from FTB</h2>
      <div class="news-item" v-for="(newsItem, index) in news" :key="index">
        <a :href="`${domain}/blog/p/${newsItem.slug}`" @click="openExternal" v-if="newsItem.feature_image" class="feature-image mb-4 block">
          <img crossorigin="anonymous" class="rounded shadow-xl" :src="newsItem.feature_image" alt="Feature image">
        </a>
        
        <div class="about">
          <a :href="`${domain}/blog/p/${newsItem.slug}`" @click="openExternal" class="title block mb-2 font-bold text-lg">{{ newsItem.title }}</a>
          <p class="mb-4 max-lines-4">{{ newsItem.custom_excerpt ? newsItem.custom_excerpt : newsItem.excerpt }}</p>
        </div>
        
        <div class="author-and-info flex items-center gap-4">
          <img class="avatar rounded shadow-xl" crossorigin="anonymous" width="50" :src="newsItem.primary_author.profile_image" alt="Avatar">
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

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {Action, Getter} from 'vuex-class';
import Loader from '@/components/ui/Loader.vue';
import {ns} from '@/core/state/appState';
import {AsyncFunction} from '@/core/@types/commonTypes';
import {BlogPost} from '@/core/@types/external/metaApi.types';
import dayjs from 'dayjs';
import {standardDateTime} from '@/utils/helpers/dateHelpers';
import {constants} from '@/core/constants';
import {createLogger} from '@/core/logger';

@Component({
  components: {
    Loader,
  },
  methods: {
    dayjs,
    standardDateTime
  }
})
export default class Blog extends Vue {
  @Action('loadNews', ns("v2/news")) loadNews!: AsyncFunction;
  @Getter('news', ns("v2/news")) news!: BlogPost[];
  @Getter('loading', ns("v2/news")) loading!: boolean;
  
  private logger = createLogger("Blog.vue");
  
  public mounted() {
    if (this.news === null || this.news.length < 1) {
      this.loadNews()
        .catch(e => this.logger.error("Failed to load news", e))
    }
  }
  
  get domain() {
    return constants.ftbDomain
  }
}
</script>

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
