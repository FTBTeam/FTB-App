import { defineStore } from 'pinia';
import { InstanceJson, SugaredInstanceJson } from '@/core/types/javaApi';
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
    },
    
    addInstance(instance: SugaredInstanceJson | InstanceJson) {
      this.instances.push(instance as SugaredInstanceJson);
      this.instanceCategories = Array.from(new Set(this.instances.map(i => i.category)));
    },
    
    updateInstance(instance: SugaredInstanceJson) {
      const index = this.instances.findIndex(i => i.uuid === instance.uuid);
      if (index === -1) {
        return;
      }
      
      this.instances[index] = instance;
      this.instanceCategories = Array.from(new Set(this.instances.map(i => i.category)));
    },
    
    removeInstance(uuid: string) {
      const index = this.instances.findIndex(i => i.uuid === uuid);
      if (index === -1) {
        return;
      }
      
      this.instances.splice(index, 1);
      this.instanceCategories = Array.from(new Set(this.instances.map(i => i.category)));
    }
  }
})