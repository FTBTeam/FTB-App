<template>
  <div class="tab-actions-body" v-if="instance || packInstance">
    <div class="body-heading" v-if="activeTab !== tabs.SETTINGS">
      <div class="action-heading">
        <div class="action-holder flex items-center justify-between duration-200 transition-opacity" :class="{'opacity-0': (isInstalling && currentInstall) || modloaderUpdating}">
          <div class="play">
            <ftb-button color="primary" class="py-3 px-8 ftb-play-button" :disabled="isInstalled && isRunning" @click="() => $emit('mainAction')">
              <font-awesome-icon :icon="isInstalled ? 'play' : 'download'" class="mr-4" />
              {{ isInstalled ? 'Play' : 'Install' }}
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
            <div class="option" v-if="packInstance && !isVanilla" @click="() => $emit('showVersion')">
              Versions
              <font-awesome-icon icon="code-branch" class="ml-2" />
            </div>
            <div class="option" @click="() => $emit('tabChange', tabs.SETTINGS)" v-if="isInstalled">
              Settings
              <font-awesome-icon icon="cogs" class="ml-2" />
            </div>
          </div>
        </div>

        <transition name="transition-fade" duration="250">
          <div class="install-progress" v-if="isInstalling && currentInstall">
            <div class="status flex gap-4 mb-4">
              <div class="percent">{{currentInstall.progress}}<span>%</span></div>
              <b>{{currentInstall.stage ?? "??"}}</b>
              <transition name="transition-fade" duration="250">
                <div class="files text-sm" v-if="currentInstall.speed">
                  <font-awesome-icon icon="bolt" class="mr-2" />({{(currentInstall.speed / 12500000).toFixed(2)}}) Mbps
                </div>
              </transition>
            </div>
            <progress-bar class="progress" :progress="parseFloat(currentInstall?.progress ?? '0') / 100" />
          </div>
        </transition>
        
        <transition name="transition-fade" duration="250">
          <div class="install-progress" v-if="modloaderUpdating">
            <div class="status flex gap-4 items-center">
              <font-awesome-icon spin icon="circle-notch" class="mr-2" />
              <div class="message">
                <b class="block">Updating Modloader</b>
                <small class="text-muted">This may take a moment</small>
              </div>
            </div>
          </div>
        </transition>
      </div>

      <div class="tabs">
        <div
          v-if="packInstance"
          class="tab"
          :class="{ active: activeTab === tabs.OVERVIEW }"
          @click="() => $emit('tabChange', tabs.OVERVIEW)"
        >
          Overview
        </div>
        <div v-if="!isVanilla" class="tab" :class="{ active: activeTab === tabs.MODS }" @click="() => $emit('tabChange', tabs.MODS)">
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
<!--        <div v-if="packInstance && packInstance.meta && packInstance.meta.supportsWorlds" class="tab flex items-center whitespace-no-wrap justify-center" :class="{ active: activeTab === tabs.WORLDS }" @click="() => $emit('tabChange', tabs.WORLDS)">-->
<!--          FTB Worlds <span class="bg-yellow-400 rounded px-1 py-0-5 sm:px-2 sm:py-1 text-black text-opacity-75 font-bold ml-4 text-sm italic">New!</span>-->
<!--        </div>-->
        <a class="cta whitespace-no-wrap cursor-pointer" @click.prevent="Platform.get.utils.openUrl('https://bisecthosting.com/ftb')">
          <img class="" src="@/assets/images/branding/bh-logo.svg" alt="" />
          Order a server
        </a>
      </div>
    </div>

    <div class="body-contents">
      <div class="alert py-2 px-4 mb-4 bg-warning rounded" v-if="packInstance && packInstance.notification">
        <font-awesome-icon icon="info" class="mr-2" />
        {{ packInstance.notification }}
      </div>

      <div class="pack-overview" v-if="activeTab === tabs.OVERVIEW">
        <div class="stats-and-links">
          <div class="stats">
            <template v-if="isInstalled">
              <div class="stat" v-if="instance.lastPlayed !== 0">
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
            </template>
            <template v-else-if="packInstance">
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
              v-if="(!isInstalled || (instance && !instance.lastPlayed)) && packInstance && packInstance.released !== 'unknown'"
              :title="(packInstance.released || packInstance.updated) | dayjsFull"
            >
              <div class="name">Released</div>
              <div class="value font-sans">{{ (packInstance.released || packInstance.updated) | dayjsFromNow }}</div>
            </div>
            <div
              class="stat"
              v-if="(!isInstalled || (instance && !instance.lastPlayed)) && packInstance && packInstance.versions && packInstance.versions[0]"
              :title="packInstance.versions[0].updated | dayjsFull"
            >
              <div class="name">Updated</div>
              <div class="value font-sans">{{ packInstance.versions[0].updated | dayjsFromNow }}</div>
            </div>
          </div>
          
          <div class="links" v-if="packInstance && instance && isInstalled">
            <a :href="issueTracker" @click="openExternal" class="link" v-if="issueTracker">
              <font-awesome-icon :icon="['fab', 'github']" />
              Report issue
            </a>
          </div>
        </div>

        <div class="tags mb-6" v-if="tags.length">
          <router-link
            v-for="(tag, i) in tags"
            :key="`tag-${i}`"
            :to="{ name: 'browseModpacks', params: { search: tag.name } }"
            class="tag"
            >{{ tag.name }}</router-link
          >
        </div>
        <div
          class="wysiwyg"
          v-if="packInstance?.provider === 'modpacks.ch' && packInstance && packInstance.description !== undefined"
          v-html="parseMarkdown(packInstance.description)"
        />
        <div class="description" v-else-if="packInstance?.provider === 'curseforge' && packInstance && packInstance.description !== undefined" v-html="packInstance.description"></div>
        <div v-else>
          <h2>No description available</h2>
        </div>
      </div>

      <!-- Tab views, we're not using the router because it's a pain-->
      <modpack-mods
        v-if="activeTab === tabs.MODS && !isVanilla"
        :api-pack="packInstance"
        :pack-installed="isInstalled"
        :instance="instance"
        @showFind="() => $emit('searchForMods')"
        @searchForMods="() => $emit('searchForMods')"
      />

      <!-- If the pack page grows more, we will have to use the router to clean this up. -->
      <modpack-settings @back="$emit('tabChange', tabs.OVERVIEW)" :instance="instance" v-if="instance && isInstalled && activeTab === tabs.SETTINGS" />

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
      
      <worlds-tab
        v-if="activeTab === tabs.WORLDS"
        :instance="instance"
        :apiPack="packInstance"
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
import {Prop} from 'vue-property-decorator';
import {ModPack} from '@/modules/modpacks/types';
import {ModpackPageTabs} from '@/views/InstancePage.vue';
import ModpackMods from '@/components/templates/modpack/ModpackMods.vue';
import ModpackSettings from '@/components/templates/modpack/ModpackSettings.vue';
import PackActions from '@/components/molecules/modpack/PackActions.vue';
import ModpackBackups from '@/components/templates/modpack/ModpackBackups.vue';
import PackUpdateButton from '@/components/molecules/modpack/PackUpdateButton.vue';
import Platform from '@/utils/interface/electron-overwolf';
import {Backup, SugaredInstanceJson} from '@/core/@types/javaApi';
import Loader from '@/components/atoms/Loader.vue';
import {stringIsEmpty} from '@/utils/helpers/stringHelpers';
import {parseMarkdown} from '@/utils';
import ProgressBar from '@/components/atoms/ProgressBar.vue';
import {ns} from '@/core/state/appState';
import {InstallStatus} from '@/core/controllers/InstanceInstallController';
import {Getter, State} from 'vuex-class';
import {ModLoaderUpdateState} from '@/core/@types/states/appState';
import {typeIdToProvider} from '@/utils/helpers/packHelpers';
import {InstanceRunningData} from '@/core/state/misc/runningState';
import WorldsTab from '@/components/molecules/modpack/WorldsTab.vue';

@Component({
  name: 'pack-body',
  components: {
    WorldsTab,
    ProgressBar,
    Loader,
    PackUpdateButton,
    ModpackSettings,
    ModpackMods,
    PackActions,
    ModpackBackups,
  },
  methods: {
    stringIsEmpty
  }
})
export default class PackBody extends Vue {
  // The stored instance for an installed pack
  @Prop({ default: null }) instance!: SugaredInstanceJson;
  @Prop({ default: false }) packLoading!: boolean;
  // Pack Instance is the modpack api response
  @Prop() packInstance?: ModPack;
  @Prop() isInstalled!: boolean;
  @Prop() activeTab!: ModpackPageTabs;
  @Prop({ default: false }) allowOffline!: boolean;

  @Prop({ default: () => [] }) backups!: Backup[];

  @State("instances", ns("v2/running")) public runningInstances!: InstanceRunningData[]
  @Getter("currentInstall", ns("v2/install")) currentInstall!: InstallStatus | null;
  @Getter("currentModloaderUpdate", ns("v2/install")) currentModloaderUpdate!: ModLoaderUpdateState[] | null;
  
  Platform = Platform;

  tabs = ModpackPageTabs;
  
  get tags() {
    if (!this.packInstance) return [];
    
    if (this.packInstance.tags === undefined) return [];
    return this.packInstance.tags.sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0)) ?? [];
  }

  parseMarkdown(input: string) {
    if (!input) {
      return '';
    }

    return parseMarkdown(input);
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

  get isInstalling() {
    if (!this.currentInstall) {
      return false;
    }

    return this.currentInstall?.forInstanceUuid === this.instance.uuid
  }
  
  get isRunning() {
    if (!this.isInstalled || !this.instance) return false;
    
    return this.runningInstances.some(e => e.uuid === this.instance.uuid);    
  }

  get isVanilla() {
    // This should likely be done a smarter way
    return this.instance 
      ? this.instance.modLoader === this.instance.mcVersion
      : this.packInstance?.id === 81;
  }

  get packSlug() {
    if (!this.packInstance) return '';
    return `${this.packInstance.id}_${this.packInstance.name.replaceAll(' ', '-').replaceAll(/[^\w|-]+/g, '')}`;
  }
  
  get modloaderUpdating() {
    return this.currentModloaderUpdate?.some(e => e.instanceId === this.instance.uuid) ?? false;
  }
  
  get issueTracker() {
    if (!this.instance) return '';
    
    if (typeIdToProvider(this.instance.packType) === "curseforge" && this.packInstance) {
      return this.packInstance.links.find(e => e.type === "issues")?.link ?? '';
    } 
    
    return "https://go.ftb.team/support-modpack"
  }
}
</script>

<style lang="scss" scoped>
.action-heading {
  padding: 1rem 1.5rem 1.5rem;
  background-color: rgba(black, 0.2);
  position: relative;

  .action-holder {
    position: relative;
  }
  
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
  
  .install-progress {
    position: absolute;
    inset: 0;
    z-index: 10;
    padding: 1rem;
  }
}

.tabs {
  background-color: rgba(black, 0.1);
  padding: 0.5rem 1.5rem 0 1.5rem;
  margin-bottom: 0.8rem;
  display: flex;
  align-items: center;
  gap: 1rem;

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
    position: relative;
    display: flex;
    align-items: center;
    background-color: #1d1d1d;
    padding: 0.45rem 1rem 0.45rem 2rem;
    border-radius: 5px;
    margin-bottom: 0.5rem;
    border: 1px solid rgba(255, 255, 255, 0.2);
    transition: border-color 0.25s ease-in-out, background-color 0.25s ease-in-out, color 0.25s ease-in-out;
    color: rgba(white, 0.8);

    &:hover {
      background-color: #272727;
      border-color: #03ddff;
      color: white;
    }

    img {
      position: absolute;
      left: -16px;
      top: 0;
      max-width: 36px;
      margin-right: 1rem;
    }
  }
}

.pack-overview {
  .stats-and-links {
    margin-bottom: 1.5rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .links {
      .link {
        display: flex;
        align-items: center;
        color: rgba(white, .7);
        transition: color .25s ease-in-out;
        
        &:hover {
          color: white;
        }
        
        svg {
          margin-right: .5rem;
        }
      }
    }
  }
  
  .stats {
    display: flex;

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
    gap: .5rem;

    .tag {
      margin-bottom: 0.5rem;
      padding: 0.2rem 0.5rem;
      font-size: 12px;
      -webkit-user-drag: none;
      
      background-color: rgba(black, .4);
      border-radius: 5px;
      
      transition: background-color .25s ease-in-out;
      
      &:hover {
        background-color: var(--color-success-button);
      }
    }
  }
}

.body-contents {
  padding: 0.8rem 1.5rem 1rem 1.5rem;
}
</style>
