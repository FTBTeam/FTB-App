<template>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full" v-if="!news.loading">
    <div class="new" v-if="news">
      <ftb-news v-for="(item, index) in news.news" :key="index" :title="item.title" :date="item.date" :link="item.link">
        <span v-html="item.content"></span>
      </ftb-news>
    </div>
  </div>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full" v-else>
    <strong>Loading</strong>
  </div>

</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import FTBNews from '@/components/FTBNews.vue';
import {State, Action, Getter} from 'vuex-class';
import {NewsState, NewsItem} from '@/modules/news/types';

const namespace: string = 'news';

@Component({
    components: {
        'ftb-news': FTBNews,
    },
})
export default class Home extends Vue {

    @State('news') public news: NewsState | undefined = undefined;
    @Action('fetchNews', {namespace}) public fetchNews: any;

    public mounted() {
        if (this.news == null || this.news.news.length < 1) {
          this.fetchNews();
        }
    }

}
</script>

<style lang="scss">
  .internalLink {
    color: red;
  }
</style>
