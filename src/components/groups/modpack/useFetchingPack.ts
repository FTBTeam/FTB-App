import { ModPack, PackProviders } from '@/modules/modpacks/types.ts';
import { ref } from 'vue';

export function useFetchingPack() {
  const apiModpack = ref<ModPack | null>(null);
  const loading = ref(false);
  
  // TODO: [port]: Fixme
  //@Action("getModpack", ns("v2/modpacks")) getModpack!: GetModpack;
  const getModpack = async (_: { id: number; provider: PackProviders }) => {
    return null;
  }
  
  async function fetchModpack(packId: number, provider: PackProviders = "modpacksch") {
    if (packId === -1) {
      return;
    }

    loading.value = true;
    const result = await getModpack({
      id: packId,
      provider: provider
    });

    if (result) {
      apiModpack.value = result;
    }
    loading.value = false;
  }
  
  return {
    loading,
    apiModpack,
    fetchModpack
  }
}