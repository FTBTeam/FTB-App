<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import {Action} from 'vuex-class';
import {GetModpack} from '@/core/state/modpacks/modpacksState';
import {ModPack} from '@/modules/modpacks/types';

@Component
export default class PackCardCommon extends Vue {
  @Action("getModpack", { namespace: 'v2/modpacks' }) getModpack!: GetModpack;
  
  loading = false;
  apiModpack: ModPack | null = null;
  
  async fetchModpack(packId: number) {
    this.loading = true;
    const result = await this.getModpack(packId);
    if (result) {
      this.apiModpack = result;
    }
    this.loading = false;
  }
}
</script>