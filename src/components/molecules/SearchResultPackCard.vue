<template>
  <div class="search-result-pack">
    <div class="splash-art" v-if="splash" :style="{ backgroundImage: `url(${splash})` }"></div>
    <div class="logo">
      <img :src="squareArt" :alt="`Pack art for ${pack.name}`" />
    </div>
    <div class="pack-main">
      <div class="name">{{ pack.name }} <span>by</span> {{ pack.authors.map((e) => e.name).join(', ') }}</div>
      <div class="desc" :title="pack.synopsis">
        {{ pack.synopsis.length > 116 ? pack.synopsis.slice(0, 116) + '...' : pack.synopsis }}
      </div>
      <div class="tags">
        <div class="tag" v-for="(tag, index) in tags" :key="index">{{ tag.name }}</div>
      </div>
    </div>
    <div class="install-btn">
      <font-awesome-icon icon="download" />
    </div>
  </div>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import Vue from 'vue';
import { PackSearchEntries } from '@/typings/modpackch';
import { Prop } from 'vue-property-decorator';
import { getPackArt } from '@/utils';

@Component
export default class SearchResultPackCard extends Vue {
  @Prop() pack!: PackSearchEntries.Packs;

  get squareArt() {
    return getPackArt(this.pack.art);
  }

  get splash() {
    return this.pack.art.find((e) => e.type === 'splash')?.url;
  }

  get tags() {
    return this.pack.tags.slice(0, 5);
  }
}
</script>

<style lang="scss" scoped>
.search-result-pack {
  position: relative;
  overflow: hidden;
  display: flex;
  padding: 1.5rem;
  border-radius: 5px;
  margin-bottom: 1.5rem;
  align-items: center;
  font-size: 14px;
  background-color: rgba(white, 0.05);
  cursor: pointer;
  box-shadow: 0 5px 20px rgba(black, 0.2);

  .splash-art {
    position: absolute;
    z-index: -1;
    top: -2.5%;
    left: -2.5%;
    width: 110%;
    height: 110%;
    filter: blur(4px);

    &::after {
      position: absolute;
      background-color: rgba(black, 0.4);
      content: '';
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
    }
  }

  .logo {
    align-self: flex-start;
    margin-right: 2rem;

    img {
      width: 100px;
      height: 100px;
      border-radius: 5px;
      box-shadow: 0 4px 15px rgba(black, 0.2);
    }
  }

  .pack-main {
    flex: 1;
    margin-right: 1rem;

    .name {
      font-weight: bold;
      font-size: 1.1425em;
      margin-bottom: 0.35rem;

      span {
        margin-left: 0.5rem;
        font-weight: normal;
        font-style: italic;
      }
    }

    .desc {
      margin-bottom: 0.6rem;
    }

    .tags {
      display: flex;
      gap: 0.35rem;
      flex-wrap: wrap;

      .tag {
        background-color: rgba(black, 0.4);
        padding: 0.1rem 0.3rem;
        border-radius: 3px;
      }
    }
  }

  .install-btn {
    padding: 0.5rem 1rem;
    background-color: #27ae60;
    border-radius: 5px;
  }
}
</style>
