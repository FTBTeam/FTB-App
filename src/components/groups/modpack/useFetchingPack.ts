import { ref } from 'vue';
import { useModpackStore } from '@/store/modpackStore.ts';
import { toggleBeforeAndAfter } from '@/utils/helpers/asyncHelpers.ts';
import { ModPack, PackProviders } from '@/core/types/appTypes.ts';

export function useFetchingPack() {
  const apiModpack = ref<ModPack | null>(null);
  const loading = ref(false);
  
  const modpackStore = useModpackStore();
  
  async function fetchModpack(packId: number, provider: PackProviders = "modpacksch") {
    if (packId === -1) {
      return;
    }

    await toggleBeforeAndAfter(async () => {
      const result = await modpackStore.getModpack(packId, provider);

      if (result) {
        apiModpack.value = result;
      }
    }, state => loading.value = state);
  }
  
  return {
    loading,
    apiModpack,
    fetchModpack
  }
}