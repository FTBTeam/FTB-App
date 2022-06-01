<template>
  <div class="mod-packs h-full" v-if="isLoaded">
    <!-- My Modpacks Stuff -->
    <div class="packs px-6 py-4" v-if="modpacks.installedPacks.length > 0">
      <div class="flex items-center mb-4">
        <FTBSearchBar v-model="searchTerm" placeholder="Search" class="mr-4 flex-1" />
        <ftb-button class="py-3 px-4 flex items-center" color="info" @click="useShareCode = true">
          <font-awesome-icon icon="code" class="mr-2" size="1x" />
          <span>Use share code</span>
        </ftb-button>
      </div>

      <div class="pack-card-list grid">
        <pack-card-wrapper
          v-for="modpack in packs"
          :list-mode="false"
          :key="modpack.uuid"
          :art="modpack.art"
          :installed="true"
          :minecraft="'1.7.10'"
          :version="modpack.version"
          :description="getModpack(modpack.id) !== null ? getModpack(modpack.id).synopsis : 'Unable to load synopsis'"
          :tags="getModpack(modpack.id) !== null ? getModpack(modpack.id).tags : []"
          :versions="modpack.versions"
          :name="modpack.name"
          :authors="modpack.authors"
          :instance="modpack"
          :instanceID="modpack.uuid"
          :kind="modpack.kind"
        >
        </pack-card-wrapper>
      </div>
    </div>
    <div class="flex flex-1 flex-wrap justify-center flex-col items-center no-packs" v-else>
      <div class="message flex flex-1 flex-wrap items-center flex-col mt-32">
        <font-awesome-icon icon="heart-broken" size="6x" />
        <h1 class="text-5xl">Oh no!</h1>
        <span class="mb-4 w-3/4 text-center">
          Would you look at that! Looks like you've got no modpacks installed yet... If you know what you want, click
          Browse and search through our collection and all of CurseForge modpacks, otherwise, use Discover we've got
          some great recommended packs.</span
        >
        <div class="flex flex-row justify-between my-2">
          <router-link to="/browseModpacks">
            <ftb-button color="primary" class="py-2 px-10 mx-2">Browse</ftb-button>
          </router-link>
          <router-link to="/discover">
            <ftb-button color="primary" class="py-2 px-6 mx-2">Discover</ftb-button>
          </router-link>
        </div>
      </div>
    </div>

    <modal
      :open="useShareCode"
      title="Use a share code"
      subTitle="A share code is a code you can use to install new packs"
      @closed="useShareCode = false"
    >
      <message type="danger" v-if="shareCodeError" class="mb-4">
        {{ shareCodeError }}
      </message>

      <ftb-input label="Share code" v-model="shareCode" />
      <div class="flex justify-end">
        <ftb-button color="primary" class="py-2 px-6 mt-2 inline-block" @click="checkAndInstall">
          <font-awesome-icon icon="download" class="mr-2" size="1x" />
          Install
        </ftb-button>
      </div>
    </modal>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import PackCardWrapper from '@/components/organisms/packs/PackCardWrapper.vue';
import FTBSearchBar from '@/components/atoms/input/FTBSearchBar.vue';
import { Instance, ModPack, ModpackState } from '@/modules/modpacks/types';
import { Action, Getter, State } from 'vuex-class';
import { SettingsState } from '@/modules/settings/types';
import { wsTimeoutWrapperTyped } from '@/utils';
import { InstallerState } from '@/modules/app/appStore.types';

@Component({
  components: {
    PackCardWrapper,
    FTBSearchBar,
  },
})
export default class Library extends Vue {
  @State('settings') public settings!: SettingsState;
  @State('modpacks') public modpacks!: ModpackState;
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @Getter('packsCache', { namespace: 'modpacks' }) public packsCache!: ModPack[];
  @Action('fetchModpack', { namespace: 'modpacks' }) public fetchModpack!: (id: number) => Promise<ModPack>;
  @Action('sendMessage') public sendMessage!: any;
  @Action('installModpack', { namespace: 'app' }) public installModpack!: (data: InstallerState) => void;

  private searchTerm: string = '';
  private isLoaded: boolean = false;
  isGrid: boolean = false;

  useShareCode = false;
  shareCode: string = '';
  shareCodeError = '';

  @Watch('modpacks', { deep: true })
  public async onModpacksChange(newVal: ModpackState, oldVal: ModpackState) {
    if (JSON.stringify(newVal.installedPacks) !== JSON.stringify(oldVal.installedPacks)) {
      this.isLoaded = false;
      try {
        await Promise.all(
          this.modpacks.installedPacks.map(async (instance) => {
            const pack = await this.fetchModpack(instance.id);
            return pack;
          }),
        );
        this.isLoaded = true;
      } catch (err) {
        this.isLoaded = true;
      }
    }
  }

  public async mounted() {
    if (this.modpacks) {
      this.isLoaded = false;
      try {
        await Promise.all(this.modpacks.installedPacks.map(async (instance) => await this.fetchModpack(instance.id)));
        this.isLoaded = true;
      } catch (err) {
        this.isLoaded = true;
      }
    }
  }

  async checkAndInstall() {
    if (this.shareCode === '') {
      return;
    }

    const checkCode = await wsTimeoutWrapperTyped<any, { success: boolean }>({
      type: 'checkShareCode',
      shareCode: this.shareCode,
    });

    if (!checkCode.success) {
      this.shareCodeError = `Unable to find a valid pack with the code of ${this.shareCode} `;
      return;
    }

    this.useShareCode = false;

    this.installModpack({
      pack: {
        shareCode: this.shareCode,
      },
      meta: {
        name: 'Shared pack',
        version: this.shareCode,
      },
    });

    this.shareCode = '';
  }

  get packs(): Instance[] {
    return this.modpacks == null
      ? []
      : this.searchTerm.length > 0
      ? this.modpacks.installedPacks.filter((pack) => {
          return pack.name.search(new RegExp(this.searchTerm, 'gi')) !== -1;
        })
      : this.modpacks.installedPacks.sort((a, b) => {
          return b.lastPlayed - a.lastPlayed;
        });
  }

  public getModpack(id: number): ModPack | null {
    return this.packsCache[id] ? this.packsCache[id] : null;
  }
}
</script>

<style lang="scss" scoped>
.pack-card-list {
  &.grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, 148px);
    gap: 1rem;
  }
}

.no-packs {
  position: relative;
  height: 100%;

  &::before {
    content: '';
    top: 0;
    left: 0;
    position: absolute;
    width: 100%;
    height: 100%;

    background: url('../assets/images/no-pack-bg.webp') center center no-repeat;
    background-size: auto 100%;
    z-index: -1;
    opacity: 0.3;
  }
}
</style>
