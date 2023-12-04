<template>
  <div class="pack-info">
    <div class="info">
      <div class="name select-text">{{ packName }}</div>
      <div class="desc select-text" v-if="(!instance || !instance.isImport) && !isInstalled">
        {{ packInstance.name }}
        <template v-if="packInstance && packInstance.authors && packInstance.authors.length">
          by 
          <span v-if='provider === "modpacksch" && packInstance.tags.findIndex(e => e.name.toLowerCase() === "ftb") !== -1'>FTB Team</span>
          <span v-else v-for="(author, i) in packInstance.authors" :key="'athrs' + i">{{ author.name }}</span>
        </template>
        -
        {{ packInstance.synopsis }}
      </div>
      <div class="select-text pack-version text-xs px-4 py-1 rounded bg-transparent-black inline-block" v-if="instance && !instance.isImport">
        Version {{ instance.version }}
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import {Prop} from 'vue-property-decorator';
import {ModPack} from '@/modules/modpacks/types';
import {InstanceJson, SugaredInstanceJson} from '@/core/@types/javaApi';
import {typeIdToProvider} from '@/utils/helpers/packHelpers';

@Component
export default class PackTitleHeader extends Vue {
  @Prop() packInstance?: ModPack | null;
  @Prop() packName!: string;
  @Prop({ default: false }) isInstalled!: boolean;

  @Prop() instance!: SugaredInstanceJson | InstanceJson;
  
  get provider() {
    return typeIdToProvider(this.packInstance?.id ?? 0);
  }
}
</script>

<style lang="scss" scoped>
.pack-info {
  padding: 2rem 1rem;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  text-shadow: 0 0 5px rgba(black, 0.6);
  background-color: rgba(black, 0.2);

  .info {
    padding: 1rem 4rem;
    @media (min-width: 1300px) {
      max-width: 90%;
    }
  }

  .name {
    font-size: 2.2rem;
    font-weight: bold;
    line-height: 1.2em;
    margin-bottom: 0.5rem;

    @media (min-width: 1300px) {
      font-size: 2.8rem;
    }
  }
}
</style>
