<template>
  <div class="search-result-pack" @click.prevent="openInfo">
    <div class="splash-art" v-if="splash" :style="{ backgroundImage: `url(${splash})` }"></div>
    <div class="logo">
      <img :src="squareArt" :alt="`Pack art for ${pack.name}`" />
    </div>
    <div class="pack-main">
      <div class="name">{{ pack.name }} <span>by</span> {{ pack.authors.map((e) => e.name).join(', ') }}</div>
      <div class="desc max-2-lines" :title="pack.synopsis ? pack.synopsis : ''">
        {{ pack.synopsis ? (pack.synopsis.length > 500 ? pack.synopsis.slice(0, 116) + '...' : pack.synopsis) : '' }}
      </div>
      <div class="tags">
        <div class="tag" v-for="(tag, index) in tags" :key="index">{{ tag.name }}</div>
      </div>
    </div>
    <div class="install-btn" @click.stop="install">
      <font-awesome-icon icon="download" />
    </div>
  </div>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import Vue from 'vue';
import { Emit, Prop } from 'vue-property-decorator';
import { getPackArt } from '@/utils';
import { RouterNames } from '@/router';
import {SearchResultPack} from '@/core/@types/modpacks/packSearch';

@Component
export default class SearchResultPackCard extends Vue {
  @Prop() pack!: SearchResultPack;
  @Prop() type!: number; // 0 == FTB, 1 == Curseforge

  @Emit()
  install() {
    return this.pack;
  }

  openInfo() {
    this.$router.push({
      name: RouterNames.ROOT_PREVIEW_PACK,
      query: { modpackid: '' + this.pack.id, type: '' + (this.type ?? 0) },
    });
  }

  get squareArt() {
    return getPackArt(this.pack.art);
  }

  get splash() {
    const splash = this.pack.art.find((e) => e.type === 'splash');
    if (splash) {
      return encodeURI(splash.url);
    }
    
    return undefined;
  }

  get tags() {
    return this.pack.tags.slice(0, 5);
  }
}
</script>

<style lang="scss" scoped>
.search-result-pack {
  position: relative;
  z-index: 1;
  overflow: hidden;
  display: flex;
  padding: 1rem;
  border-radius: 5px;
  margin-bottom: 1.5rem;
  align-items: center;
  font-size: 14px;
  background: var(--color-sidebar-item);
  cursor: pointer;
  box-shadow: 0 3px 15px rgba(black, 0.2);
  transform-origin: top center;
  transition: transform 0.25s ease-in-out, box-shadow 0.25s ease-in-out;

  &:hover {
    box-shadow: 0 8px 25px rgba(black, 0.4);
    transform: translateY(-0.3rem);

    .splash-art {
      background-size: 105%;
    }
  }

  .splash-art {
    position: absolute;
    z-index: -1;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    filter: blur(5px) brightness(60%);
    background-position: top center;
    background-size: 100%;
    transition: background-size 0.25s ease-in-out;
    transform: scale(1.2);
  }

  .logo {
    align-self: flex-start;
    margin-right: 2rem;
    width: 100px;
    height: 100px;
    display: flex;
    justify-content: center;

    img {
      height: 100%;
      border-radius: 5px;
      box-shadow: 0 4px 15px rgba(black, 0.2);
    }
  }

  .max-2-lines {
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 1;
    -webkit-box-orient: vertical;
  }

  .pack-main {
    flex: 1;
    margin-right: 1rem;

    .name {
      font-weight: bold;
      font-size: 1.1425em;
      margin-bottom: 0.35rem;

      span {
        margin: 0 0.5rem;
        font-weight: normal;
        font-style: italic;
      }
    }

    .desc {
      padding-right: 1rem;
    }

    .tags {
      margin-top: 0.6rem;
      display: flex;
      gap: 0.35rem;
      flex-wrap: wrap;

      .tag {
        background-color: rgba(black, 0.4);
        padding: 0.2rem 0.5rem;
        border-radius: 3px;
      }
    }
  }

  .install-btn {
    padding: 0.5rem 1rem;
    background-color: #27ae60;
    border-radius: 5px;
    box-shadow: 0 4px 15px rgba(black, 0.2);
    transition: background-color 0.25s ease-in-out;

    &:hover {
      background-color: #39d05f;
    }
  }
}
</style>
