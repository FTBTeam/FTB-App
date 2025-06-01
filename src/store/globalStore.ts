import { defineStore } from 'pinia';
import {ImagePreview, ModPack, PackProviders} from "@/core/types/appTypes.ts";
import {SearchResultPack} from "@/core/types/modpacks/packSearch.ts";

type GlobalStore = {
  createInstanceVisible: boolean;
  imagePreview: ImagePreview | null;
  modpackPreview: {
    provider: PackProviders,
    id: number;
  } | null;
}

export const useGlobalStore = defineStore("global", {
  state: (): GlobalStore => {
    return {
      createInstanceVisible: false,
      imagePreview: null,
      modpackPreview: null
    }
  },

  actions: {
    updateCreateInstanceVisibility(visible: boolean) {
      this.createInstanceVisible = visible;
    },
    openImagePreview(image: ImagePreview) {
      this.imagePreview = image;
    },
    closeImagePreview() {
      this.imagePreview = null;
    },
    openModpackPreview(id: number, provider: PackProviders) {
      this.modpackPreview = { provider, id };
    },
    closeModpackPreview() {
      this.modpackPreview = null;
    },
  }
})