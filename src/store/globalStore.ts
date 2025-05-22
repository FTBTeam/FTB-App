import { defineStore } from 'pinia';
import {ImagePreview} from "@/core/types/appTypes.ts";

type GlobalStore = {
  createInstanceVisible: boolean;
  imagePreview: ImagePreview | null;
}

export const useGlobalStore = defineStore("global", {
  state: (): GlobalStore => {
    return {
      createInstanceVisible: false,
      imagePreview: null
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
  }
})