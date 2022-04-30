<!--<template>-->
<!--  <div class="logs" @click="hay">-->
<!--    <div class="loading" v-if="loading">Loading...</div>-->
<!--    <div class="log" v-for="(version, key) in versions" :key="key" v-else>-->
<!--      {{ version }}-->
<!--    </div>-->
<!--  </div>-->
<!--</template>-->

<!--<script lang="ts">-->
<!--import Vue from 'vue';-->
<!--import Component from 'vue-class-component';-->
<!--import { Action } from 'vuex-class';-->
<!--import platform from '@/utils/interface/electron-overwolf';-->
<!--import EventBus from '@/utils/event-bus';-->

<!--@Component-->
<!--export default class ChangelogHistory extends Vue {-->
<!--  @Action('sendMessage') public sendMessage: any;-->

<!--  loading = true;-->
<!--  versions: string[] = [];-->

<!--  mounted() {-->
<!--    this.getVersions().finally(() => (this.loading = false));-->
<!--  }-->

<!--  hay() {-->
<!--    EventBus.$emit('changelog-open', { hello: 'google' });-->
<!--  }-->

<!--  async getVersions() {-->
<!--    // Get the available versions-->
<!--    try {-->
<!--      const changelogsReq = await fetch(`${process.env.VUE_APP_META_API}/v1/changelogs/app`);-->
<!--      this.versions = (await changelogsReq.json()).versions;-->
<!--    } catch (e) {-->
<!--      console.error('caught error', e);-->
<!--      return;-->
<!--    }-->
<!--  }-->

<!--  // Some magic to get the current version-->
<!--  getCurrentVersion() {-->
<!--    return platform.get.config.publicVersion;-->
<!--  }-->
<!--}-->
<!--</script>-->

<!--<style lang="scss" scoped>-->
<!--.heading-image {-->
<!--  display: block;-->
<!--  margin-bottom: 1rem;-->
<!--  border-radius: 5px;-->
<!--}-->
<!--</style>-->
