<template>
  <div class="flex flex-1 flex-col justify-center align-center lg:p-10 sm:p-5 h-full text-center" >
    <font-awesome-icon icon="clock" class="mx-auto" size="10x"></font-awesome-icon>
    <h1 class="text-4xl">Coming {{time}}</h1>
    <h1 class="text-2xl">Third party modpacks have not been finalised yet, this page is only here to show this notice</h1>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import FTBNews from '@/components/FTBNews.vue';
import {State, Action, Getter} from 'vuex-class';
import {NewsState, NewsItem} from '@/modules/news/types';

@Component({
})
export default class ComingSoon extends Vue {

  private possible: string[] = ['soon', 'eventually', 'soon™', 'in time', 'at some point', 'in the future', 'later', 'before long', 'in due time', 'in short order', 'in a little while'];

  get time() {
    return this.possible[Math.floor(Math.random() * this.possible.length)];
  }

}
</script>

<style lang="scss">
  .internalLink {
    color: red;
  }
</style>
