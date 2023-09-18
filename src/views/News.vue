<template>
  <div class="px-6 py-4" v-if="!loading">
    <template v-if="news.length">
      <div v-for="(newsItem, index) in news" :key="index">
        {{ newsItem.title }}
        {{ newsItem.excerpt }}
        {{ newsItem.custom_excerpt }}
        {{ newsItem.published_at }}
        {{ newsItem.slug }}
        {{ newsItem.feature_image }}
        {{ newsItem.primary_author }}
        <hr>
      </div>
    </template>
    <div v-else>
      No news found :cry:
    </div>
<!--    <div class="new" v-if="getNews != null && getNews.length > 0">-->
<!--      <ftb-news-->
<!--        v-for="(item, index) in getNews"-->
<!--        :key="index"-->
<!--        :news-item="item"-->
<!--      />-->
<!--    </div>-->
  </div>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full" v-else>
    <loading2 />
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import FTBNews from '@/components/templates/FTBNews.vue';
import { Action, Getter } from 'vuex-class';
import Loading2 from '@/components/atoms/Loading2.vue';
import {ns} from '@/core/state/appState';
import {AsyncFunction} from '@/core/@types/commonTypes';

const namespace: string = 'news';

@Component({
  components: {
    Loading2,
    'ftb-news': FTBNews,
  },
})
export default class Home extends Vue {
  @Action('loadNews', ns("v2/news")) loadNews!: AsyncFunction;
  @Getter('news', ns("v2/news")) news: any;
  @Getter('loading', ns("v2/news")) loading!: boolean;
  
  public mounted() {
    if (this.news == null || this.news.length < 1) {
      this.loadNews()
        .catch((err: any) => {
          console.error(err);
        });
    }
  }
}
</script>

<style lang="scss">
.internalLink {
  color: red;
}
</style>
