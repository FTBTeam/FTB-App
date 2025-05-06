import { defineStore } from 'pinia';

type GlobalStore = {
  createInstanceVisible: boolean;
}

export const useGlobalStore = defineStore("global", {
  state: (): GlobalStore => {
    return {
      createInstanceVisible: false
    }
  },

  actions: {
    updateCreateInstanceVisibility(visible: boolean) {
      this.createInstanceVisible = visible;
    }
  }
})