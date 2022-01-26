<template>
  <div class="px-6 py-4" v-if="!getNews.loading">
    <div class="new" v-if="getNews">
      <ftb-news
        v-for="(item, index) in getNews.news"
        :key="index"
        :title="item.title"
        :date="item.date"
        :link="item.link"
      >
        <span v-html="item.content"></span>
      </ftb-news>
    </div>
  </div>
  <div class="flex flex-1 flex-col lg:p-10 sm:p-5 h-full" v-else>
    <strong>Loading</strong>
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import FTBNews from '@/components/templates/FTBNews.vue';
import { Action, Getter } from 'vuex-class';

const namespace: string = 'news';

@Component({
  components: {
    'ftb-news': FTBNews,
  },
})
export default class Home extends Vue {
  @Action('fetchNews', { namespace }) public fetchNews: any;
  @Getter('getNews', { namespace }) public getNews: any;

  public mounted() {
    if (this.getNews == null || this.getNews.news.length < 1) {
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
