<template>
  <div class="px-6 py-4 h-full search-container">
    <div class="search-and-switcher mb-4">
      <div class="switcher shadow">
        <div
          @click="changeTab('ftbsearch')"
          class="btn-icon bg-navbar ftb"
          :class="{ active: currentTab === 'ftbsearch' }"
        >
          <img src="@/assets/ftb-white-logo.svg" alt="" />
        </div>

        <div
          @click="changeTab('cursesearch')"
          class="btn-icon bg-navbar eww-orange"
          :class="{ active: currentTab === 'cursesearch' }"
        >
          <img src="@/assets/curse-logo.svg" alt="" />
        </div>
      </div>
      <FTBSearchBar
        v-model="searchValue"
        class="w-full"
        :placeholder="`Search ${currentTab === 'ftbsearch' ? 'FTB Modpacks' : 'Curseforge Modpacks'}`"
        :min="3"
      />
    </div>

    <message icon="warning" type="danger" class="m-6" v-if="error">
      {{ error }}
    </message>

    <Loading class="mt-20" v-if="searchStarted || loading || (modpacks.loading && searchValue === '')" />

    <div class="result-cards" v-else>
      <search-result-pack-card
        v-for="(pack, index) in results"
        :pack="pack"
        :key="index"
        :type="currentTab === 'ftbsearch' ? 0 : 1"
        @install="displayInstallModpack"
      />
    </div>

    <FTBModal :visible="showInstallModal" @dismiss-modal="clearInstallModal" size="large-dynamic">
      <install-modal
        v-if="!installerLoading && installModalData"
        :pack-name="installModalData.pack.name"
        :pack-description="installModalData.pack.synopsis"
        :versions="installModalData.versions"
        :do-install="runInstaller"
      />
      <div v-else class="py-6">
        <p class="font-bold text-center mt-2">Loading Modpack Versions</p>
        <Loading :size="`5`" class="mt-8" />
      </div>
    </FTBModal>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import { Action, State } from 'vuex-class';
import PackCardList from '@/components/organisms/packs/PackCardList.vue';
import Loading from '@/components/atoms/Loading.vue';
import FTBSearchBar from '@/components/atoms/input/FTBSearchBar.vue';
import { ModPack, ModpackState, Versions } from '@/modules/modpacks/types';
import { Route } from 'vue-router';
import { AuthState } from '@/modules/auth/types';
import { abortableFetch, AbortableRequest, createModpackchUrl, debounce } from '@/utils';
import { ListPackSearchResults, PackSearchEntries } from '@/typings/modpackch';
import SearchResultPackCard from '@/components/molecules/SearchResultPackCard.vue';
import InstallModal from '@/components/organisms/modals/InstallModal.vue';
import FTBModal from '@/components/atoms/FTBModal.vue';
import { InstallerState } from '@/modules/app/appStore.types';

const namespace: string = 'modpacks';

@Component({
  components: {
    SearchResultPackCard,
    PackCardList,
    FTBSearchBar,
    Loading,
    InstallModal,
    FTBModal,
  },
})
export default class BrowseModpacks extends Vue {
  @State('auth') public authState!: AuthState;
  @State('modpacks') public modpacks: ModpackState | undefined = undefined;
  @Action('loadAllPacks', { namespace }) public loadAllPacks: any;
  @Action('getPopularPlays', { namespace }) public getPopularPlays: any;
  @Action('getPrivatePacks', { namespace }) public getPrivatePacks: any;
  @Action('installModpack', { namespace: 'app' }) public installModpack!: (data: InstallerState) => void;

  searchValue: string = '';
  currentTab: string = 'ftbsearch';

  currentSearch: AbortableRequest | null = null;
  searchResults: PackSearchEntries.Packs[] = [];

  loading = false;
  searchStarted = false;

  // Installing
  showInstallModal = false;
  installModalData: {
    pack: PackSearchEntries.Packs;
    versions: Versions[];
  } | null = null;
  installerLoading = false;
  error = '';

  async mounted() {
    if (this.$route.params.search) {
      this.searchValue = this.$route.params.search;
      await this.searchPacks();
    }

    if (this.modpacks === undefined || this.modpacks.all === undefined || this.modpacks.all.length <= 0) {
      await this.loadAllPacks();
      if (this.authState.token !== null) {
        await this.getPrivatePacks();
      }
    }
  }

  @Watch('$route')
  public async onPropertyChanged(value: Route, oldValue: Route) {
    if (value.params.search !== oldValue.params.search) {
      this.searchValue = value.params.search;
      await this.searchPacks();
    }
  }

  private debounceSearch = debounce(() => {
    this.searchPacks();
  }, 1000);

  private async changeTab(tab: string) {
    this.currentTab = tab;
    this.searchResults = [];
    await this.searchPacks();
  }

  @Watch('searchValue')
  private onSearch() {
    if (this.searchValue === '') {
      this.searchStarted = false;
      this.loading = false;
      return;
    }

    this.error = '';
    this.searchStarted = true;
    this.debounceSearch();
  }

  async searchPacks() {
    if (this.searchValue.length < 2) {
      return;
    }

    if (this.currentSearch) {
      this.currentSearch.abort();
    }

    this.loading = true;

    this.currentSearch = abortableFetch(
      createModpackchUrl(
        `/modpack/search/20/detailed?term=${this.searchValue}&platform=${
          this.currentTab !== 'ftbsearch' ? 'curseforge' : 'modpacksch'
        }`,
      ),
    );

    // Caught as this request will be aborted if it's slow and a new search term comes in
    try {
      const fetch = await this.currentSearch.ready;
      const data = await fetch.json();

      if (data.status !== 'error') {
        this.searchResults = (data as ListPackSearchResults).packs;
      }
    } catch {
    } finally {
      this.loading = false;
      this.searchStarted = false;
    }
  }

  async displayInstallModpack(pack: PackSearchEntries.Packs) {
    this.showInstallModal = true;
    this.installerLoading = true;

    try {
      const request = await fetch(
        createModpackchUrl(`/${this.currentTab === 'ftbsearch' ? 'modpack' : 'curseforge'}/${pack.id}`),
      );
      const response: ModPack = await request.json();

      this.installModalData = {
        pack,
        versions: response.versions.sort((a, b) => b.id - a.id),
      };
    } catch (error) {
      this.error = 'Failed to find pack version information, please try again in a minute...';
      this.clearInstallModal();
      console.error(error);
      return;
    } finally {
      this.installerLoading = false;
    }
  }

  runInstaller(version: number, versionName: string) {
    this.showInstallModal = false;
    this.installModpack({
      pack: {
        id: this.installModalData?.pack.id,
        version: version,
        packType: this.currentTab === 'ftbsearch' ? 0 : 1,
      },
      meta: {
        name: this.installModalData?.pack.name ?? '',
        version: versionName,
        art: PackCardList.getLogo(this.installModalData?.pack.art),
      },
    });
  }

  clearInstallModal() {
    this.showInstallModal = false;
    this.installModalData = null;
    this.installerLoading = false;
  }

  get results(): PackSearchEntries.Packs[] {
    // Transform the 'in state' packs to a search result for displaying all packs
    if (this.searchValue === '' && typeof this.modpacks?.all !== 'undefined') {
      return this.modpacks?.all.map(
        (e) =>
          ({
            platform: 'modpacksch',
            name: e.name,
            art: e.art as unknown as PackSearchEntries.Art[],
            authors: e.authors as PackSearchEntries.Authors[],
            tags: e.tags as PackSearchEntries.Tags[],
            synopsis: e.synopsis,
            id: e.id,
            updated: e.updated,
          } as PackSearchEntries.Packs),
      );
    }

    return this.searchResults;
  }
}
</script>

<style lang="scss">
h1 {
  text-transform: capitalize;
}

.result-cards {
  position: relative;
  z-index: 1;
}

.search-and-switcher {
  position: relative;
  display: flex;
  z-index: 1;

  .switcher {
    margin-right: 1rem;
    display: flex;
    border-radius: 5px;
    overflow: hidden;

    .btn-icon {
      padding: 0 1.2rem;
      display: flex;
      align-items: center;
      cursor: pointer;
      transition: background-color 0.25s ease-in-out;

      img {
        width: 40px;
        opacity: 0.5;
        transition: opacity 0.25s ease-in-out;
      }

      &.ftb img {
        margin-top: -4px;
      }

      &.active {
        background-color: var(--color-primary-button);
        img {
          opacity: 1;
        }
      }

      &.eww-orange.active {
        background-color: #ff784d;
      }

      &:hover {
        img {
          opacity: 1;
        }
      }
    }
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.05s ease;
}
.slide-enter,
.slide-leave-to {
  transform: translateX(-10px);
}
.display-mode-switcher {
  display: flex;
  flex-direction: row-reverse;
  background-color: #313131;
}
</style>
