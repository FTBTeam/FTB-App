<script lang="ts" setup>
  import {useAds} from "@/composables/useAds.ts";
  import {ModPack} from "@/core/types/appTypes.ts";
  import {computed, onMounted, ref, watch} from "vue";
  import {useModpackStore} from "@/store/modpackStore.ts";
  import {Loader} from "@/components/ui";
  import {resolveArtwork} from "@/utils/helpers/packHelpers.ts";
  import Stat from "@/components/groups/global/modpackPreview/Stat.vue";
  import dayjs from "dayjs";
  import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
  import {faDownload, faTimes} from "@fortawesome/free-solid-svg-icons";
  import ModpackPreviewOverview from "@/components/groups/global/modpackPreview/ModpackPreviewOverview.vue";
  import ModpackPreviewMods from "@/components/groups/global/modpackPreview/ModpackPreviewMods.vue";
  import ModpackPreviewVersions from "@/components/groups/global/modpackPreview/ModpackPreviewVersions.vue";
  import ModpackPreviewGallery from "@/components/groups/global/modpackPreview/ModpackPreviewGallery.vue";
  import {useGlobalStore} from "@/store/globalStore.ts";
  import ModpackInstallModal from "@/components/modals/ModpackInstallModal.vue";

  const ads = useAds();
  const modpackStore = useModpackStore();
  const globalStore = useGlobalStore();
  
  type Tabs = "overview" | "mods" | "versions" | "gallery";
  
  const loading = ref(true);
  const tab = ref<Tabs>("overview");
  const showInstallModal = ref(false);
  const modpack = ref<ModPack | null>(null);
  
  onMounted(async () => {
    attemptToLoadModpack()
  })
  
  function attemptToLoadModpack() {
    if (!globalStore.modpackPreview) return;
    loading.value = true;
    modpackStore.getModpack(globalStore.modpackPreview.id, globalStore.modpackPreview.provider)
      .then((modpackData) => modpack.value = modpackData)
      .finally(() => loading.value = false)
      .catch((e) => console.error(e));
  }
  
  watch(() => globalStore.modpackPreview, (newValue, oldValue) => {
    if (newValue === null) {
      loading.value = true;
      modpack.value = null;
      tab.value = "overview";
      showInstallModal.value = false;
      return;
    }
    
    if (newValue === oldValue) return;

    attemptToLoadModpack();
  })
  
  const backgroundArtwork = computed(() => modpack && resolveArtwork(modpack.value, "splash"))
  const logoArtwork = computed(() => modpack && resolveArtwork(modpack.value, "square"))

  function previewImage(screenshot: string) {
    globalStore.openImagePreview({
      url: screenshot,
      name: `${modpack.value?.name} Logo Artwork`,
    })
  }
</script>

<template>
  <transition name="transition-fade">
    <div v-if="globalStore.modpackPreview" class="px-18 pt-20 absolute top-0 left-0 h-full bg-black/60 backdrop-blur z-50" :style="`width: ${ads.adsEnabled.value ? 'calc(100% - 400px)' : '100%'};`" @click="globalStore.closeModpackPreview()">
      <div class="h-full max-w-[1000px] mx-auto bg-[#2A2A2A] rounded-t-xl" @click.stop>
        <Loader v-if="loading" />
        <div class="flex flex-col h-full" v-else-if="modpack">
          <header class="p-5 relative z-[1] rounded-t-xl" :style="`background-image: url(${backgroundArtwork}); background-size: cover;`">
            <div class="absolute top-2 right-2 bg-black/70 backdrop-blur px-1 py-1 rounded-lg cursor-pointer" @click="globalStore.closeModpackPreview()">
              <FontAwesomeIcon :icon="faTimes" fixed-width />
            </div>
            <div class="absolute -z-[1] inset-0 rounded-t-xl bg-gradient-to-t to-[#2A2A2A]/50 from-[#2A2A2A]"></div>
            <div class=" flex gap-6">
              <img alt="Modpack Artwork" @click="previewImage(logoArtwork)" v-if="logoArtwork" :src="logoArtwork" class="w-32 h-32 rounded-2xl shadow-2xl outline-2 outline-white/20" />
              <div>
                <h1 class="text-shadow font-bold text-2xl pt-3">{{modpack.name}}</h1>
                <p :title="modpack.synopsis" class="text-shadow text-lg mt-2 line-clamp-3">{{modpack.synopsis}}</p>

                <div class="flex gap-7 text-shadow mt-3">
                  <Stat title="Installs" :value="modpack.installs" />
                  <Stat v-if="modpack.provider !== 'curseforge'" title="Plays" :value="modpack.plays" />
                  <Stat title="Released" :value="dayjs.unix(modpack.released === 'unknown' ? 0 : modpack.released).format('DD MMM, YYYY')" />
                </div>
              </div>
            </div>
            <nav class="flex gap-3 mt-6 mb-2 items-center text-[13px]">
              <div class="flex gap-4 cursor-pointer items-center bg-green-600 hover:bg-green-500 transition-colors duration-200 font-bold rounded-xl py-4 px-6" @click="showInstallModal = true"><FontAwesomeIcon :icon="faDownload" /> Install</div>
              <div class="flex gap-2 flex-1 rounded-xl p-2 bg-[#2A2A2A]/65 backdrop-blur">
                <div @click="() => tab = 'overview'" class="cursor-pointer hover:bg-white/10 transition-colors duration-300 rounded-lg py-2 px-5" :class="{'!bg-blue-600': tab === 'overview'}">Overview</div>
                <div @click="() => tab = 'mods'" class="cursor-pointer hover:bg-white/10 transition-colors duration-300 rounded-lg py-2 px-5" :class="{'!bg-blue-600': tab === 'mods'}">Mods</div>
                <div @click="() => tab = 'versions'" class="cursor-pointer hover:bg-white/10 transition-colors duration-300 rounded-lg py-2 px-5" :class="{'!bg-blue-600': tab === 'versions'}">Versions</div>
                <div @click="() => tab = 'gallery'" class="cursor-pointer hover:bg-white/10 transition-colors duration-300 rounded-lg py-2 px-5" :class="{'!bg-blue-600': tab === 'gallery'}">Gallery</div>
              </div>
              <a class="flex items-center gap-4 text-nowrap py-3.5 px-4 rounded-xl bg-[#2A2A2A]/65 hover:bg-[#1b57c4] transition-colors duration-200 backdrop-blur" target="_blank" :href="`https://bisecthosting.com/ftb?r=app-modpack-${modpack.provider === 'curseforge' ? 'curseforge' : modpack.slug}`">
                <img width="24" class="" src="../../../../assets/images/branding/bh-logo.svg" alt="" />
                <span :class="{'hidden xl:inline-block': ads.adsEnabled.value}">Host a server</span>
              </a>
            </nav>
          </header>

          <div class="p-5 flex-1 overflow-y-auto">
            <div v-show="tab === 'overview'"><ModpackPreviewOverview :modpack="modpack" /></div>
            <div v-show="tab === 'mods'"><ModpackPreviewMods :modpack="modpack" /></div>
            <div v-show="tab === 'versions'"><ModpackPreviewVersions :modpack="modpack" /></div>
            <div v-show="tab === 'gallery'"><ModpackPreviewGallery :modpack="modpack" /></div>
          </div>
        </div>
      </div>
    </div>
  </transition>
  <modpack-install-modal @close="(v) => {
    showInstallModal = false;
    if (v) globalStore.closeModpackPreview();
  }" v-if="modpack" :open="showInstallModal" :pack-id="modpack.id" :provider="modpack.provider === 'modpacks.ch' ? 'modpacksch' : 'curseforge'" />
</template>