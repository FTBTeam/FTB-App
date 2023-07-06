<template>
  <div class="pack-info">
    <div class="info">
      <div class="name">{{ packName }}</div>
      <div class="desc" v-if="(!instance || !instance.isImport) && !isInstalled">
        {{ packInstance.name }}
        <template v-if="packInstance.authors && packInstance.authors.length"
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
import { Instance, ModPack } from '@/modules/modpacks/types';

@Component
export default class PackTitleHeader extends Vue {
  @Prop() packInstance!: ModPack;
  @Prop() packName!: string;
  @Prop({ default: false }) isInstalled!: boolean;

  @Prop() instance!: Instance;
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
    padding: 1rem 4rem;
    @media (min-width: 1300px) {
      max-width: 90%;
    }
  }

  .name {
    font-size: 2.2rem;
    font-family: "JetBrains Mono";
    //font-weight: 400;
    font-weight: bold;
    line-height: 1.2em;
    margin-bottom: 0.5rem;

    @media (min-width: 1300px) {
      font-size: 2.8rem;
    }
  }
}
</style>
