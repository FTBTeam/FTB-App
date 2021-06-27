<template>
  <div class="packs p-6">
    <div class="section">
      <h2 class="text-xl pb-4">Recently Played</h2>
    </div>
    <div class="section">
      <h2 class="text-xl pb-4">Featured Modpacks</h2>
      <modpack-card v-for="(pack, index) in modpacks" :key="index" :pack="pack" />
    </div>
  </div>
</template>

<script lang="ts">
import { Component } from 'vue-property-decorator';
import Vue from 'vue';
import { Modpack } from '@/v2/types';
import ModpackCard from '@/v2/components/packs/ModpackCard.vue';

@Component({
  components: {
    ModpackCard,
  },
})
export default class Home extends Vue {
  modpacks: Modpack[] = [{ name: 'hello' }, { name: 'hello' }, { name: 'hello' }, { name: 'hello' }, { name: 'hello' }];
}
</script>

<style lang="scss" scoped></style>
