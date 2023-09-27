<template>
  <div class="tab-actions-body" v-if="instance || packInstance">
    <div class="body-heading" v-if="activeTab !== tabs.SETTINGS">
      <div class="action-heading">
        <div class="play">
          <ftb-button color="primary" class="py-3 px-8 ftb-play-button" @click="() => $emit('mainAction')">
            <font-awesome-icon :icon="isInstalled ? (requiresSync ? 'download' : 'play') : 'download'" class="mr-4" />
            {{ isInstalled ? (requiresSync ? 'Sync' : 'Play') : 'Install' }}
          </ftb-button>

          <pack-actions
            v-if="instance && isInstalled"
            :instance="instance"
            :allow-offline="allowOffline"
            @openSettings="$emit('tabChange', tabs.SETTINGS)"
            @playOffline="$emit('playOffline')"
          />
        </div>

        <div class="options">
          <PackUpdateButton
            v-if="isInstalled"
            :instance="packInstance"
            :localInstance="instance"
            @update="$emit('update')"
          />
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
          v-if="isInstalled && backups.length > 0"
          class="tab"
          :class="{ active: activeTab === tabs.BACKUPS }"
          @click="() => $emit('tabChange', tabs.BACKUPS)"
        >
          World Backups
        </div>
<!--        <div-->
<!--          v-if="isInstalled && currentVersionObject"-->
<!--          class="tab"-->
<!--          :class="{ active: activeTab === tabs.PUBLIC_SERVERS }"-->
<!--          @click="() => $emit('tabChange', tabs.PUBLIC_SERVERS)"-->
<!--        >-->
<!--          Public servers-->
<!--        </div>-->
        <a class="cta" @click.prevent="Platform.get.utils.openUrl(`https://www.creeperhost.net/modpack/${packSlug}`)">
          <img class="ch-logo" src="@/assets/ch-logo.svg" alt="" />
          Order a server
        </a>
      </div>
    </div>

    <div class="body-contents">
      <div class="alert py-2 px-4 mb-4 bg-warning rounded" v-if="packInstance.notification">
        <font-awesome-icon icon="info" class="mr-2" />
        {{ packInstance.notification }}
      </div>

      <div class="pack-overview" v-if="activeTab === tabs.OVERVIEW">
        <div class="stats">
          <template v-if="isInstalled">
            <div class="stat">
              <div class="name">Last played</div>
              <div class="value">{{ instance.lastPlayed | dayjsFromNow }}</div>
            </div>
            <div class="stat" v-if="instance.totalPlayTime !== 0">
              <div class="name">Total Playtime</div>
              <div class="value">
                <span v-for="(unit, index) in computeTime(instance.totalPlayTime)" :key="index">
                  <template v-if="unit !== ''"> {{ unit }} </template>
                </span>
              </div>
            </div>
            <div class="stat">
              <div class="name">Version</div>
              <div class="value">{{ instance.version }}</div>
            </div>
          </template>
          <template v-else>
            <div class="stat">
              <div class="name">Installs</div>
              <div class="value font-sans">{{ packInstance.installs | formatNumber }}</div>
            </div>

            <div class="stat" v-if="packInstance.type !== 'Curseforge'">
              <div class="name">Plays</div>
              <div class="value font-sans">{{ packInstance.plays | formatNumber }}</div>
            </div>
          </template>

          <div
            class="stat"
            v-if="packInstance && packInstance.released !== 'unknown'"
            :title="(packInstance.released || packInstance.updated) | dayjsFull"
          >
            <div class="name">Released</div>
            <div class="value font-sans">{{ (packInstance.released || packInstance.updated) | dayjsFromNow }}</div>
          </div>
          <div
            class="stat"
            v-if="packInstance && packInstance.versions && packInstance.versions[0]"
            :title="packInstance.versions[0].updated | dayjsFull"
          >
            <div class="name">Updated</div>
            <div class="value font-sans">{{ packInstance.versions[0].updated | dayjsFromNow }}</div>
          </div>
        </div>

        <div class="tags mb-6" v-if="tags.length">
          <router-link
            v-for="(tag, i) in tags"
            :key="`tag-${i}`"
            :to="{ name: 'browseModpacks', params: { search: tag.name } }"
            class="cursor-pointer tag rounded mr-2 lowercase"
            :style="{
              fontVariant: 'small-caps',
              backgroundColor: `hsla(${getColorForChar(tag.name, 90, 70)}, .5)`,
            }"
            >{{ tag.name }}</router-link
          >
        </div>
        <div
          class="wysiwyg"
          v-if="packInstance.description !== undefined"
          v-html="parseMarkdown(packInstance.description)"
        />
        <div v-else>
          <h2>No description available</h2>
        </div>
      </div>

      <!-- Tab views, we're not using the router because it's a pain-->
      <modpack-mods
        v-if="activeTab === tabs.MODS"
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
<!--      <modpack-public-servers-->
<!--        v-if="isInstalled && currentVersionObject"-->
<!--        v-show="activeTab === tabs.PUBLIC_SERVERS && currentVersionObject.mtgID"-->
<!--        :instance="instance"-->
<!--        :current-version="currentVersionObject.mtgID"-->
<!--        :pack-instance="packInstance"-->
<!--      />-->

      <modpack-backups
        @backupsChanged="$emit('backupsChanged')"
        v-if="activeTab === tabs.BACKUPS"
        :instance="instance"
        :backups="backups"
      />
    </div>
  </div>
  <div class="loading pt-12" v-else>
    <!-- This should literally never happen -->
    <loader />
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import { Prop } from 'vue-property-decorator';
import { ModPack } from '@/modules/modpacks/types';
import { ModpackPageTabs } from '@/views/InstancePage.vue';
import ModpackMods from '@/components/templates/modpack/ModpackMods.vue';
import ModpackSettings from '@/components/templates/modpack/ModpackSettings.vue';
import { getColorForChar } from '@/utils/colors';
import MarkdownIt from 'markdown-it';
import PackActions from '@/components/molecules/modpack/PackActions.vue';
import ModpackBackups from '@/components/templates/modpack/ModpackBackups.vue';
import PackUpdateButton from '@/components/molecules/modpack/PackUpdateButton.vue';
import Platform from '@/utils/interface/electron-overwolf';
import {Backup, SugaredInstanceJson} from '@/core/@types/javaApi';
import Loader from '@/components/atoms/Loader.vue';

@Component({
  name: 'pack-body',
  components: {
    Loader,
    PackUpdateButton,
    ModpackSettings,
    ModpackMods,
    PackActions,
    ModpackBackups,
  },
})
export default class PackBody extends Vue {
  // The stored instance for an installed pack
  @Prop({ default: null }) instance!: SugaredInstanceJson;
  @Prop({ default: false }) packLoading!: boolean;
  // Pack Instance is the modpack api response
  @Prop() packInstance!: ModPack;
  @Prop() isInstalled!: boolean;
  @Prop() activeTab!: ModpackPageTabs;
  @Prop() mods!: any[];
  @Prop() updatingModlist!: boolean;
  @Prop({ default: false }) allowOffline!: boolean;

  @Prop({ default: () => [] }) backups!: Backup[];

  Platform = Platform;

  tabs = ModpackPageTabs;
  getColorForChar = getColorForChar;

  get tags() {
    if (this.packInstance.tags === undefined) return [];
    return this.packInstance.tags.sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0)) ?? [];
  }

  parseMarkdown(input: string) {
    if (!input) {
      return '';
    }

    return new MarkdownIt().render(input);
  }

  computeTime(second: number) {
    second = second / 1000;
    const days = Math.floor(second / (3600 * 24));
    const hours = Math.floor((second % (3600 * 24)) / 3600);
    const minutes = Math.floor((second % 3600) / 60);
    const seconds = Math.floor(second % 60);

    return {
      days: days > 0 ? days + 'd' : '',
      hours: hours > 0 ? hours + 'h' : '',
      minutes: minutes > 0 ? minutes + 'm' : '',
      seconds: seconds > 0 ? seconds + 's' : '',
    };
  }

  // get currentVersionObject(): Versions | null {
  //   if (this.instance === null) {
  //     return null;
  //   }
  //
  //   if (this.packInstance !== null) {
  //     if (this.packInstance.versions === undefined) {
  //       return null;
  //     }
  //     const version = this.packInstance.versions.find((f: Versions) => f.id === this.instance?.versionId);
  //     if (version !== undefined) {
  //       return version;
  //     }
  //   }
  //   return null;
  // }
  
  get requiresSync() {
    return this.instance?.pendingCloudInstance ?? false;
  }

  get packSlug() {
    return `${this.packInstance.id}_${this.packInstance.name.replaceAll(' ', '-').replaceAll(/[^\w|-]+/g, '')}`;
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
    position: relative;
    display: flex;

    .ftb-play-button {
      position: relative;
      z-index: 2;
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

  .cta {
    display: flex;
    align-items: center;
    background-color: #1d1d1d;
    padding: 0.45rem 1rem;
    border-radius: 5px;
    margin-bottom: 0.5rem;
    border: 1px solid rgba(255, 255, 255, 0.2);
    transition: border-color 0.25s ease-in-out, background-color 0.25s ease-in-out, color 0.25s ease-in-out;
    color: rgba(white, 0.8);

    &:hover {
      background-color: #272727;
      border-color: #8ab839;
      color: white;
    }

    img {
      max-width: 12px;
      margin-right: 1rem;
    }
  }
}

.pack-overview {
  .stats {
    display: flex;
    margin-bottom: 1.5rem;

    .stat {
      margin-right: 2rem;

      .name {
        opacity: 0.7;
        font-size: 0.875rem;
      }

      .value {
        font-weight: bold;
      }
    }
  }

  .tags {
    display: flex;
    flex-wrap: wrap;

    .tag {
      margin-bottom: 0.5rem;
      padding: 0.15rem 0.5rem;
      font-weight: bold;
    }
  }
}

.body-contents {
  padding: 0.8rem 1.5rem 1rem 1.5rem;
}
</style>
