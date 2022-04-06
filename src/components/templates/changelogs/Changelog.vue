<template>
  <div class="changelog-modal"></div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Action } from 'vuex-class';
import { wsTimeoutWrapper } from '@/utils/helpers';

@Component
export default class Changelog extends Vue {
  @Action('sendMessage') public sendMessage: any;

  mounted() {
    setTimeout(() => {
      this.checkForUpdate().catch((e) => {
        // Todo: soft crash
        console.log(e);
      });
    }, 5_000);
  }

  async checkForUpdate() {
    const data = await wsTimeoutWrapper({
      type: 'storage.get',
      key: 'lastVersion',
    });

    // No held last version meaning we should find a changelog
    if (data.response) {
      console.log('No last version');

      const update = await wsTimeoutWrapper({
        type: 'storage.put',
        key: 'lastVersion',
        value: 'hi',
      });
    } else {
      console.log(data.response);
    }
  }
}
</script>

<style lang="scss" scoped></style>
