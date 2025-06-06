<script lang="ts" setup>
import {SearchResultPack} from '@/core/types/modpacks/packSearch';
import {resolveArtwork} from '@/utils/helpers/packHelpers';
import {stringOrDefault} from '@/utils/helpers/stringHelpers';
import ModpackInstallModal from '@/components/modals/ModpackInstallModal.vue';
import {dialogsController} from '@/core/controllers/dialogsController';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { computed, onMounted, ref } from 'vue';
import { useFetchingPack } from '@/components/groups/modpack/useFetchingPack.ts';
import { useInstallStore } from '@/store/installStore.ts';
import { PackProviders } from '@/core/types/appTypes.ts';
import { faDownload } from '@fortawesome/free-solid-svg-icons';
import {useGlobalStore} from "@/store/globalStore.ts";

const {
  packId,
  partialPack,
  provider = "modpacksch"
} = defineProps<{
  packId?: number;
  partialPack?: SearchResultPack;
  provider?: PackProviders;
}>()

const showInstall = ref(false);
const { apiModpack, fetchModpack } = useFetchingPack();
const installStore = useInstallStore();
const globalStore = useGlobalStore();

onMounted(() => {
  if (!partialPack && !packId) {
    throw new Error("No packId or partialPack provided");
  }

  if (!partialPack && packId) {
    fetchModpack(packId, provider);
  }
})

const packData = computed<SearchResultPack | null>(() => {
  if (partialPack) {
    return partialPack;
  }

  if (!apiModpack.value) {
    return null;
  }

  return {
    platform: provider,
    name: apiModpack.value.name,
    art: apiModpack.value.art,
    authors: apiModpack.value.authors,
    tags: apiModpack.value.tags,
    synopsis: apiModpack.value.synopsis,
    id: apiModpack.value.id,
    updated: apiModpack.value.updated,
    private: apiModpack.value.private ?? false
  } as SearchResultPack;
});

function install() {
  if (isInstalling.value) {
    if (!dialogsController.createConfirmationDialog("Are you sure?", "This modpack is already installing, are you sure you want to install another one?")) {
      return;
    }
  }

  showInstall.value = true;
}

const artwork = computed(() => resolveArtwork(packData.value, "splash"));
const logo = computed(() => resolveArtwork(packData.value, "square"));
const packTags = computed(() => packData.value?.tags?.slice(0, 5) ?? []);
const isInstalling = computed(() => {
  if (!installStore.currentInstall) {
    return false;
  }

  return installStore.currentInstall.request.id === apiModpack.value?.id && !installStore.currentInstall.request.updatingInstanceUuid;
});
</script>

<template>
  <div class="pack-preview-container">
    <div class="pack-preview" v-if="packData" @click="() => globalStore.openModpackPreview(packData?.id ?? 0, packData?.platform ?? 'modpacksch')">
      <div class="splash-art" v-if="artwork" :style="{ backgroundImage: `url(${artwork})` }" />
      <div class="logo">
        <img :src="logo" :alt="`Pack art for ${packData.name}`" />
      </div>
      <div class="pack-main">
        <div class="name">{{ packData.name }} <span v-if="packData.authors.length">by</span> {{ provider === "modpacksch" && packTags.findIndex(e => e.name.toLowerCase() === "ftb") !== -1 ? "FTB Team" : packData.authors.map((e) => e.name).join(', ') }}</div>
        <div class="desc max-2-lines" :title="stringOrDefault(packData.synopsis, '')">
          {{ packData.synopsis }}
        </div>
        <div class="tags">
          <div class="tag" v-for="(tag, index) in packTags" :key="index">{{ tag.name }}</div>
        </div>
      </div>
      <div class="actions flex gap-2">        
        <div class="install-btn" @click.stop="install" aria-label="Install modpack" data-balloon-pos="up-right">
          <FontAwesomeIcon :icon="faDownload" />
        </div>
      </div>
    </div>
    <div class="pack-preview preview-shadow" v-else>
      <div class="logo-shadow shadow square" />
      <div class="main">
        <div class="shadow text shadow-small"></div>
        <div class="shadow text shadow-large"></div>
        <div class="tags-shadow">
          <div class="shadow text tag-shadow" v-for="i in 5" :key="i"></div>
        </div>
      </div>
    </div>
    <ModpackInstallModal v-if="packData?.id" :open="showInstall" @close="showInstall = false" :pack-id="packData?.id" :provider="provider" />
  </div>
</template>

<style lang="scss" scoped>
@use "sass:color";

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
        margin: 0 0.25rem;
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

  .fav-btn {
    padding: 0.5rem .8rem;
    background-color: var(--color-navbar);
    border-radius: 5px;
    box-shadow: 0 4px 15px rgba(black, 0.2);
    transition: background-color 0.25s ease-in-out, color 0.25s ease-in-out;
    
    &:hover, &.is-fav {
      background-color: #ffD700;
      color: color.adjust(#ffD700, $lightness: -30%)
    }
    
    &.is-fav:hover {
      background-color: var(--color-danger-button);
      color: white;
    }
  }
}

.pack-preview.preview-shadow {
  display: flex;
  align-items: center;
  gap: 2rem;

  &:hover {
    box-shadow: initial;
    transform: inherit;
  }
  
  .logo-shadow {
    width: 100px;
    height: 100px;
  }
  
  .main {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: .5rem;
  }

  .tags-shadow {
    display: flex;
    gap: .5rem;
    margin-top: .4rem;
    
    > div {
      width: 60px;
    }
  }
  
  .shadow {
    border-radius: 8px;
    background-color: rgba(white, .1);
    animation: skeleton-loading 1s ease-in-out infinite alternate;

    @keyframes skeleton-loading {
      0% {
        opacity: .4;
      }
      100% {
        opacity: 1;
      }
    }
    
    &.text {
      height: 1.4em;
    }
    
    &.shadow-large {
      width: 80%;
    }
    
    &.shadow-small {
      width: 40%;
    }
  }
}
</style>