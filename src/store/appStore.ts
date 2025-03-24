import { defineStore } from 'pinia';

type AppState = {
  connecting: boolean;
  ready: boolean;
}

export const useAppStore = defineStore("app", {
  state: (): AppState => {
    return {
      connecting: false,
      ready: false,
    }
  },
  
  actions: {
  }
})