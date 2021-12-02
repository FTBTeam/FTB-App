<template>
  <div class="tab-actions-body" v-if="instance || packInstance">
    <div class="body-heading" v-if="activeTab !== tabs.SETTINGS">
      <div class="action-heading">
        <div class="play">
          <ftb-button
            color="primary"
            class="py-3 px-8"
            @click="() => $emit('mainAction')"
            :disabled="!isInstalled && modpacks.installing !== null"
          >
            <font-awesome-icon :icon="isInstalled ? 'play' : 'download'" class="mr-4" />
            {{ isInstalled ? 'Play' : 'Install' }}
          </ftb-button>
        </div>
        <div class="stats" v-if="isInstalled">
          <div class="stat">
            <div class="name">Last played</div>
            <div class="value">{{ instance.lastPlayed | momentFromNow }}</div>
          </div>
          <div class="stat">
            <div class="name">Version</div>
            <div class="value">{{ instance.version }}</div>
          </div>
        </div>
        <div class="stats" v-else>
          <div class="stat">
            <div class="name">Modpack Installs</div>
            <div class="value font-sans">{{ packInstance.installs | formatNumber }}</div>
          </div>

          <div class="stat">
            <div class="name">Plays</div>
            <div class="value font-sans">{{ packInstance.plays | formatNumber }}</div>
          </div>

          <!--          <div class="stat">-->
          <!--            <div class="name">Released on</div>-->
          <!--            <div class="value font-sans">{{ packInstance.updated | moment }}</div>-->
          <!--          </div>-->
        </div>

        <div class="options">
          <div class="update" v-if="isInstalled && !isLatestVersion">
            <ftb-button color="warning" class="update-btn px-4 py-1" @click="() => $emit('update')">
              Update available
              <font-awesome-icon icon="cloud-download-alt" class="ml-2" />
            </ftb-button>
          </div>
          <div class="option" @click="() => $emit('showVersion')">
            Versions
            <font-awesome-icon icon="code-branch" class="ml-2" />
          </div>
          <div class="option" @click="() => $emit('tabChange', tabs.SETTINGS)" v-if="isInstalled">
            Settings
            <font-awesome-icon icon="cogs" class="ml-2" />
          </div>
        </div>
      </div>

      <div class="tabs">
        <div
          class="tab"
          :class="{ active: activeTab === tabs.OVERVIEW }"
          @click="() => $emit('tabChange', tabs.OVERVIEW)"
        >
          Overview
        </div>
        <div class="tab" :class="{ active: activeTab === tabs.MODS }" @click="() => $emit('tabChange', tabs.MODS)">
          Mods
        </div>
        <div
          v-if="isInstalled"
          class="tab"
          :class="{ active: activeTab === tabs.PUBLIC_SERVERS }"
          @click="() => $emit('tabChange', tabs.PUBLIC_SERVERS)"
        >
          Public servers
        </div>
      </div>
    </div>

    <div class="body-contents">
      <div class="alert py-2 px-4 mb-4 bg-warning rounded" v-if="packInstance.notification">
        <font-awesome-icon icon="info" class="mr-2" />
        {{ packInstance.notification }}
      </div>

      <div class="pack-overview" v-if="activeTab === tabs.OVERVIEW">
        <div class="tags mb-6" v-if="tags.length">
          <router-link
            v-for="(tag, i) in tags"
            :key="`tag-${i}`"
            :to="{ name: 'browseModpacks', params: { search: tag.name } }"
            class="cursor-pointer rounded mr-2 text-sm px-4 py-1 lowercase"
            :style="{
              fontVariant: 'small-caps',
              backgroundColor: `hsla(${getColorForChar(tag.name, 90, 70)}, .5)`,
            }"
            >{{ tag.name }}</router-link
          >
        </div>
        <div class="" v-if="packInstance.description !== undefined">
          <VueShowdown :markdown="packInstance.description" :extensions="['classMap', 'attribMap', 'newLine']" />
        </div>
        <div v-else>
          <h2>No description available</h2>
        </div>
      </div>

      <!-- Tab views, we're not using the router because it's a pain-->
      <modpack-mods
        v-if="activeTab === tabs.MODS"
        v-show="!searchingForMods"
        :modlist="mods"
        :pack-installed="isInstalled"
        :updatingModlist="updatingModlist"
        :instance="instance"
        @showFind="() => $emit('searchForMods')"
        @getModList="() => $emit('getModList', true)"
        @searchForMods="() => $emit('searchForMods')"
      />

      <!-- If the pack page grows more, we will have to use the router to clean this up. -->
      <modpack-settings :instance="instance" v-if="instance && isInstalled && activeTab === tabs.SETTINGS" />

      <!-- v-show to allow servers to load in the background -->
      <modpack-public-servers
        v-if="isInstalled"
        v-show="activeTab === tabs.PUBLIC_SERVERS && currentVersionObject.mtgID"
        :instance="instance"
        :current-version="currentVersionObject.mtgID"
        :pack-instance="packInstance"
      />
    </div>
  </div>
  <div class="loading pt-12" v-else>
    <!-- This should literally never happen -->
    <loading />
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Prop } from 'vue-property-decorator';
import { Instance, ModPack, ModpackState, Versions } from '@/modules/modpacks/types';
import { ModpackPageTabs } from '@/views/InstancePage.vue';
import ModpackMods from '@/components/templates/modpack/ModpackMods.vue';
import ModpackSettings from '@/components/templates/modpack/ModpackSettings.vue';
import ModpackPublicServers from '@/components/templates/modpack/ModpackPublicServers.vue';
import { getColorForChar } from '@/utils/colors';
import { State } from 'vuex-class';
import ModpackVersions from '@/components/templates/modpack/ModpackVersions.vue';
import Loading from '@/components/atoms/Loading.vue';

@Component({
  components: { Loading, ModpackVersions, ModpackPublicServers, ModpackSettings, ModpackMods },
})
export default class PackBody extends Vue {
  @State('modpacks') public modpacks!: ModpackState;

  // The stored instance for an installed pack
  @Prop({ default: null }) instance!: Instance;
  // Pack Instance is the modpack api response
  @Prop() packInstance!: ModPack;
  @Prop() isLatestVersion!: boolean;
  @Prop() isInstalled!: boolean;
  @Prop() activeTab!: ModpackPageTabs;
  @Prop() mods!: any[];
  @Prop() searchingForMods!: boolean;
  @Prop() updatingModlist!: boolean;

  tabs = ModpackPageTabs;
  getColorForChar = getColorForChar;

  get tags() {
    return this.packInstance.tags.sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0)) ?? [];
  }

  get currentVersionObject(): Versions | null {
    if (this.instance === null) {
      return null;
    }

    if (this.packInstance !== null) {
      if (this.packInstance.versions === undefined) {
        return null;
      }
      const version = this.packInstance.versions.find((f: Versions) => f.id === this.instance?.versionId);
      if (version !== undefined) {
        return version;
      }
    }
    return null;
  }
}
</script>

<style lang="scss" scoped>
.action-heading {
  padding: 1rem 1.5rem 1.5rem;
  display: flex;
  align-items: center;
  background-color: rgba(black, 0.2);

  .play {
    margin-right: 2rem;
  }

  .stats {
    display: flex;

    .stat {
      margin-right: 2rem;

      .name {
        opacity: 0.7;
        font-size: 0.875rem;
      }
    }
  }

  .options {
    display: flex;
    align-items: center;
    margin-left: auto;

    .option {
      margin-left: 2rem;
      opacity: 0.7;
      cursor: pointer;
      transition: opacity 0.25s ease-in-out;

      &:hover {
        opacity: 1;
      }
    }
  }

  .update-btn {
    position: relative;

    box-shadow: 0 0 0 0 rgba(#ff801e, 1);
    animation: pulse 1.8s ease-in-out infinite;

    @keyframes pulse {
      0% {
        box-shadow: 0 0 0 0 rgba(#ff801e, 0.7);
      }

      70% {
        box-shadow: 0 0 0 10px rgba(#ff801e, 0);
      }

      100% {
        box-shadow: 0 0 0 0 rgba(#ff801e, 0);
      }
    }
  }
}

.tabs {
  background-color: rgba(black, 0.1);
  padding: 0.5rem 1.5rem 0 1.5rem;
  margin-bottom: 0.8rem;
  display: flex;
  align-items: center;

  .tab {
    flex: 1;
    text-align: center;
    padding: 0.5rem 1.5rem 1rem 1.5rem;
    color: rgba(white, 0.5);
    cursor: pointer;
    transition: color 0.25s ease-in-out, background-color 0.25s ease-in-out;
    margin-right: 1rem;
    border-bottom: 2px solid transparent;

    &.active {
      border-bottom-color: var(--color-info-button);
      color: white;
    }

    &:hover {
      color: white;
    }
  }
}

.body-contents {
  padding: 0.8rem 1.5rem 1rem 1.5rem;
}
</style>
