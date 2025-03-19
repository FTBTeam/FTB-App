import { defineStore } from 'pinia';
import { SugaredInstanceJson } from '@/core/types/javaApi';
import { toggleBeforeAndAfter } from '@/utils/helpers/asyncHelpers.ts';
import { sendMessage } from '@/core/websockets/websocketsApi.ts';

type InstanceState = {
  instanceCategories: string[],
  instances: SugaredInstanceJson[],
  loading: boolean
}

export const useInstanceStore = defineStore("instance", {
  state: (): InstanceState => {
    return {
      instanceCategories: [],
      instances: [],
      loading: false
    }
  },
  
  actions: {
    async loadInstances() {
      if (this.loading) {
        return;
      }
      
      try {
        const result = await toggleBeforeAndAfter(async () => await sendMessage('installedInstances', {refresh: true}), state => this.loading = state)
        
        this.instances = result.instances;
        this.instanceCategories = result.availableCategories
      } catch (error) {
        console.error("Failed to load instances", error);
      }
    }
  }
})