<script lang="ts">
import {GetModpack} from '@/core/state/modpacks/modpacksState';
import {ModPack, PackProviders} from '@/modules/modpacks/types';
import {ns} from '@/core/state/appState';
import {InstallStatus} from '@/core/controllers/InstanceInstallController';

@Component
export default class PackCardCommon extends Vue {
  @Action("getModpack", ns("v2/modpacks")) getModpack!: GetModpack;
  @Getter("currentInstall", ns("v2/install")) currentInstall!: InstallStatus | null;
  
  loading = false;
  apiModpack: ModPack | null = null;
  
  async fetchModpack(packId: number, provider: PackProviders = "modpacksch") {
    if (packId === -1) {
      return;
    }
    
    this.loading = true;
    const result = await this.getModpack({
      id: packId,
      provider: provider
    });
    
    if (result) {
      this.apiModpack = result;
    }
    this.loading = false;
  }
  
  render() {
    return undefined;
  }
}
</script>