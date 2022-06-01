<template>
  <pack-card v-if="listMode === undefined || listMode === 'false' || listMode === false" v-bind="$props"></pack-card>
  <pack-card-list v-else v-bind="$props"></pack-card-list>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import PackCard from './PackCard.vue';
import PackCardList from './PackCardList.vue';

@Component({
  components: {
    PackCard,
    PackCardList,
  },
  props: [
    'listMode',
    'packID',
    'art',
    'name',
    'authors',
    'installed',
    'version',
    'versionID',
    'minecraft',
    'description',
    'instanceID',
    'versions',
    'instance',
    'fake',
    'isDemo',
    'size',
    'tags',
    'postLaunch',
    'kind',
  ],
})
export default class PackCardWrapper extends Vue {}
</script>

<style lang="scss"></style>
