<template>
  <div class="pack-preview-container">
    <div class="pack-preview" v-if="packData" @click="$router.push({
      name: RouteNames.ROOT_PREVIEW_PACK,
      query: { modpackid: '' + packData.id, type: '' + (provider === 'curseforge' ? 1 : 0) },
    })">
      <div class="splash-art" v-if="artwork" :style="{ backgroundImage: `url(${artwork})` }" />
      <div class="logo">
        <img :src="logo" :alt="`Pack art for ${packData.name}`" />
      </div>
      <template v-if="!isInstalling">
        <div class="pack-main">
          <div class="name">{{ packData.name }} <span>by</span> {{ packData.authors.map((e) => e.name).join(', ') }}</div>
          <div class="desc max-2-lines" :title="stringOrDefault(packData.synopsis, '')">
            {{ packData.synopsis }}
          </div>
          <div class="tags">
            <div class="tag" v-for="(tag, index) in packTags" :key="index">{{ tag.name }}</div>
          </div>
        </div>
        <div class="install-btn" @click.stop="showInstall = true">
          <font-awesome-icon icon="download" />
        </div>
      </template>
      <template v-else>
        <div class="installing">
          <font-awesome-icon icon="spinner" spin />
        </div>
      </template>
    </div>
    <div v-else>
      Loading...
    </div>
    <modpack-install-modal :open="showInstall" @close="showInstall = false" :pack-id="packData?.id" :api-modpack="apiModpack ? apiModpack : undefined" />
  </div>
</template>

<script lang="ts">
import {Prop, Component} from 'vue-property-decorator';
import PackCardCommon from '@/components/core/modpack/PackCardCommon.vue';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {SearchResultPack} from '@/core/@types/modpacks/packSearch';
import {PackProviders} from '@/modules/modpacks/types';
import {resolveArtwork} from '@/utils/helpers/packHelpers';
import {stringIsEmpty, stringOrDefault, trimString} from '@/utils/helpers/stringHelpers';
import {RouterNames} from '@/router';
import ModpackInstallModal from '@/components/core/modpack/ModpackInstallModal.vue';

@Component({
  components: {ModpackInstallModal},
  methods: {
    stringOrDefault
  }
})
export default class PackPreview extends PackCardCommon {
  @Prop() packId?: number;
  @Prop() partialPack?: SearchResultPack;
  @Prop() provider!: PackProviders;
  
  RouteNames = RouterNames;
  showInstall = false;
  
  mounted() {
    if (!this.partialPack && !this.packId) {
      throw new Error("No packId or partialPack provided");
    }
    
    if (!this.partialPack && this.packId) {
      this.fetchModpack(this.packId);
    }
  }

  install() {
    const apiPack = this.apiModpack!;
    instanceInstallController.requestInstall({
      id: apiPack.id,
      version: apiPack.versions[0].id,
      name: apiPack.name,
      versionName: apiPack.versions[0].name,
      logo: "",
    })
  }
  
  get artwork() {
    return resolveArtwork(this.packData, "splash");
  }
  
  get logo() {
    return resolveArtwork(this.packData, "square");
  }

  /**
   * Provides a consistent data structure for the pack data
   */
  get packData(): SearchResultPack | null {
    if (this.partialPack) {
      return this.partialPack;
    }
    
    if (!this.apiModpack) {
      return null;
    }
    
    return {
      platform: this.provider,
      name: this.apiModpack.name,
      art: this.apiModpack.art,
      authors: this.apiModpack.authors,
      tags: this.apiModpack.tags,
      synopsis: this.apiModpack.synopsis,
      id: this.apiModpack.id,
      updated: this.apiModpack.updated,
      private: this.apiModpack.private ?? false
    }
  }

  get packTags() {
    return this.packData?.tags?.slice(0, 5) ?? [];
  }
  
  /**
   * This is mostly a visual thing so people don't install a modpack multiple times
   * because they think it's not installing.
   */
  get isInstalling() {
    return this.currentInstall?.request.id === this.apiModpack?.id && !this.currentInstall?.request.updatingInstanceUuid;
  }
}
</script>

<style lang="scss" scoped>
.pack-preview {
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
      transform: scale(1.15);
      filter: blur(0px);
      
      &::after {
        opacity: .95;
        transform: translateX(-10%);
      }
    }
  }

  .splash-art {
    position: absolute;
    z-index: -1;
    inset: 0;
    filter: blur(3px);
    background-position: center top;
    background-size: cover;
    background-repeat: no-repeat;
    transition: transform 0.25s ease-in-out, filter 0.25s ease-in-out;
    transform: scale(1.1);
    
    &::after {
      position: absolute;
      content: "";
      inset: 0;
      left: 0;
      width: calc(100% + 10%);
      transition: opacity 0.25s ease-in-out, transform 0.25s ease-in-out;
      background: linear-gradient(36deg, rgba(49,49,49,1) 50%, rgba(49,49,49,0) 100%);
    }
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