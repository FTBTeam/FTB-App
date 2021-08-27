<template>
  <div class="modpack-public-servers">
    <!-- You normally won't see this -->
    <div class="loading-state" v-if="loading">
      <div class="discovering pt-10 text-center">
        <span class="font-bold text-2xl">Discovering Servers...</span>
        <p class="mt-4">
          Found <code class="bg-green-600 rounded py-1 px-2 mx-2">{{ serversFound }}</code> servers
        </p>
      </div>
    </div>
    <div v-else-if="this.servers[this.currentVersion] && this.servers[this.currentVersion].length > 0">
      <server-card
        v-for="server in sortedServers"
        :key="server.id"
        :server="server"
        :art="packInstance.art.length > 0 ? packInstance.art.filter(art => art.type === 'square')[0].url : ''"
      ></server-card>
    </div>
    <div class="flex flex-1 pt-10 flex-wrap overflow-x-auto justify-center flex-col items-center" v-else>
      <font-awesome-icon icon="heart-broken" style="font-size: 10vh"></font-awesome-icon>
      <h1 class="text-5xl">Oh no!</h1>
      <span>It doesn't looks like there are any public MineTogether servers</span>
    </div>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { ModPack } from '@/modules/modpacks/types';
import { Prop } from 'vue-property-decorator';
import { Action, Getter, State } from 'vuex-class';
import { Server, ServersState } from '@/modules/servers/types';
import ServerCard from '@/components/ServerCard.vue';

@Component({
  components: {
    ServerCard,
  },
})
export default class ModpackPublicServers extends Vue {
  @State('servers') public serverListState!: ServersState;
  @Action('getServers', { namespace: 'servers' }) public getServers!: (payload: {
    id: string;
    onSingleDone: (server: Server) => void;
  }) => Promise<void>;

  @Getter('getServers', { namespace: 'servers' }) servers!: any;

  @Prop() currentVersion!: string;
  @Prop() packInstance!: ModPack;

  loading = true;
  serversFound = 0;

  async mounted() {
    if (this.currentVersion) {
      this.loading = true;
      await this.getServers({
        id: this.currentVersion,
        onSingleDone: () => {
          this.serversFound++;
        },
      });

      this.loading = false;
    } else {
      this.loading = false;
    }
  }

  get sortedServers() {
    return (this.servers[this.currentVersion] ?? []).sort(
      (a: Server, b: Server) =>
        parseInt(b.protoResponse?.players?.online ?? '0') - parseInt(a.protoResponse?.players?.online ?? '0'),
    );
  }
}
</script>

<style lang="scss" scoped></style>
