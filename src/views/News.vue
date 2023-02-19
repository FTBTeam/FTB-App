<template>
  <div class="px-6 py-4" v-if="!newsLoading">
    <div class="new" v-if="getNews != null && getNews.length > 0">
      <ftb-news
        v-for="(item, index) in getNews"
        :key="index"
        :news-item="item"
      />
    </div>
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

const namespace: string = 'news';

@Component({
  components: {
    Loading2,
    'ftb-news': FTBNews,
  },
})
export default class Home extends Vue {
  @Action('fetchNews', { namespace }) public fetchNews: any;
  @Getter('getNews', { namespace }) public getNews: any;
  @Getter('newsLoading', {namespace}) newsLoading!: boolean;
  
  public mounted() {
    if (this.getNews == null || this.getNews.length < 1) {
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
