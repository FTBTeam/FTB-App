<template>
  <div class="pack-info">
    <div class="info">
      <div class="name">{{ packName }}</div>
      <div class="desc">
        {{ packInstance.name }}
        <template v-if="packInstance.authors.length"
          >by <span v-for="(author, i) in packInstance.authors" :key="'athrs' + i">{{ author.name }}</span></template
        >
        -
        {{ packInstance.synopsis }}
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Prop } from 'vue-property-decorator';
import { ModPack } from '@/modules/modpacks/types';

@Component
export default class PackTitleHeader extends Vue {
  @Prop() packInstance!: ModPack;
  @Prop() packName!: string;
}
</script>

<style lang="scss" scoped>
.pack-info {
  padding: 2rem 1rem;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  text-shadow: 0 0 5px rgba(black, 0.5);
  background-color: rgba(black, 0.2);

  .info {
    max-width: 80%;
    padding: 1rem 4rem;
  }

  .name {
    font-size: 2.8rem;
    font-weight: bold;
  }
}
</style>