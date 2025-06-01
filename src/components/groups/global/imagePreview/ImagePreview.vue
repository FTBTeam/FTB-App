<script lang="ts" setup>
  import {useGlobalStore} from "@/store/globalStore.ts";
  import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
  import {faGlobe} from "@fortawesome/free-solid-svg-icons";
  import {computed, onMounted, ref, watch} from "vue";
  import appPlatform from "@platform";
  import {Loader, Popover} from "@/components/ui";
  import {ImagePreview} from "@/core/types/appTypes.ts";

  const globalStore = useGlobalStore();
  const loading = ref(false);
  const computedAspectRatio = ref<{value: number, by: number} | null>(null)
  
  onMounted(() => {
    if (!loading.value && computedAspectRatio.value == null && globalStore.imagePreview) {
      computeImageAspectRatio(globalStore.imagePreview)
    }
  })
  
  function openInBrowser() {
    if (!globalStore.imagePreview) {
      return;
    }
    
    appPlatform.utils.openUrl(globalStore.imagePreview.url)
  }
  
  watch(() => globalStore.imagePreview, (newValue, oldValue) => {
    if (newValue === null) {
      loading.value = false;
      computedAspectRatio.value = null;
      return;
    }
    
    if (newValue === oldValue) return;
    
    computeImageAspectRatio(newValue);
  })
  
  function computeImageAspectRatio(preview: ImagePreview) {
    loading.value = true;
    getImageDimensions(preview.url).then((dimensions) => {
      const width = dimensions.width;
      const height = dimensions.height;

      // Figure out the lowest common denominator for the aspect ratio
      const gcd = (a: number, b: number) => {
        if (b === 0) return a;
        return gcd(b, a % b);
      };

      const divisor = gcd(width, height);
      computedAspectRatio.value = {
        value: width / divisor,
        by: height / divisor
      };
    }).finally(() => loading.value = false)
  }

  async function getImageDimensions(src: string): Promise<{ width: number, height: number }> {
    return new Promise((resolve) => {
      const img = new Image();
      img.src = src;
      // Ensure it's hidden from view
      img.style = "position: absolute; visibility: hidden; opacity: 0; z-index: -10000;";
      new ResizeObserver((_e, observer) => {
        img.remove();
        observer.disconnect();
        if (img.naturalWidth) {
          resolve({ width: img.naturalWidth, height: img.naturalHeight });
        } else {
          resolve({ width: img.width, height: img.height });
        }
      }).observe(img);
      document.body.appendChild(img);
    });
  }
  
  const isRemote = computed(() => globalStore.imagePreview?.url.startsWith("http"))
</script> 

<template>
  <transition name="transition-fade" :duration="200">
    <div v-if="globalStore.imagePreview" class="fixed top-0 left-0 inset-0 bg-black/80 backdrop-blur z-[1000] p-16 pt-20 flex items-center justify-center h-full max-h-full" @click="globalStore.closeImagePreview()">
      <div class="relative mx-auto" @click.stop :style="{maxWidth: `${computedAspectRatio?.value === 1 && computedAspectRatio?.by === 1 ? '80vh' : 'auto'}`}">
        <img v-if="!loading" :src="globalStore.imagePreview.url" class="rounded-lg shadow-2xl" />
        <Loader v-else />
        
        <div class="absolute bottom-2 left-2 max-w-full">
          <div v-if="globalStore.imagePreview.name || globalStore.imagePreview.description" class="bg-black/50 rounded-lg pl-2 pr-6 py-2 backdrop-blur-xs flex gap-4 items-center">
            <Popover text="Open in Browser" @click="openInBrowser">
              <div v-if="isRemote" class="cursor-pointer p-1.5 hover:bg-black rounded"><FontAwesomeIcon fixed-width :icon="faGlobe" /></div>
            </Popover>
            <div>
              <p class="text-lg font-bold" v-if="globalStore.imagePreview.name">{{ globalStore.imagePreview.name }}</p>
              <p class="text-white/80" v-if="globalStore.imagePreview.description">{{ globalStore.imagePreview.description }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>