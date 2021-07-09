<template>
  <div class="mod-card">
    <div class="art">
      <img :src="art" alt="Mod artwork" />
    </div>

    <div class="main" :style="{ backgroundImage: art }">
      <div class="content">
        <div class="about">
          <div class="name">{{ mod.name }}</div>
          <div class="desc pr-10 mb-3">{{ mod.synopsis }}</div>
        </div>
        <div class="get">
          <div class="btns">
            <div class="curse-btn" v-if="curseLink.link" @click="() => platform.get.utils.openUrl(curseLink.link)">
              <img src="@/assets/curse-logo.svg" alt="" />
            </div>
            <ftb-button color="primary" class="px-6 py-2" @click="showInstall = true">Install</ftb-button>
          </div>
        </div>
      </div>
      <div class="numbers">
        <div class="stat">
          <div class="text">Downloads</div>
          <div class="value is-value">{{ mod.installs.toLocaleString() }}</div>
        </div>
        <div class="stat">
          <div class="text">Latest</div>
          <div class="value">
            {{ latest.length > 20 ? latest.substr(0, 20) + '...' : latest }}
          </div>
        </div>
        <div class="stat">
          <div class="text">Author{{ mod.authors.length > 1 ? 's' : '' }}</div>
          <div class="value authors">
            {{ mod.authors.map(e => e.name).join(', ') }}
          </div>
        </div>
      </div>
    </div>

    <ftb-modal :visible="showInstall" @dismiss-modal="closeModal" :dismissable="!installing">
      <h2 class="text-3xl mb-2">{{ mod.name }}</h2>
      <p
        :style="{
          opacity: !installing && !finishedInstalling ? 1 : 0,
          height: !installing && !finishedInstalling ? 'auto' : '0 !important',
        }"
      >
        Select the version of '{{ mod.name }}' that you'd like to install to your pack.
      </p>

      <template v-if="!installing && !finishedInstalling">
        <selection
          class="my-6"
          label="Selection mod version"
          @selected="e => (selectedVersion = e)"
          v-if="versions"
          :options="
            versions.map(e => ({
              value: e.id,
              text: e.name,
              badge: {
                text: e.type,
                color:
                  e.type.toLowerCase() === 'release'
                    ? '#27AE60'
                    : e.type.toLowerCase() === 'beta'
                    ? '#00a8ff'
                    : 'black',
              },
              meta: prettyBytes(e.size),
            }))
          "
        />

        <ftb-button
          color="secondary"
          class="py-2 px-8 mb-4 text-center"
          :disabled="!selectedVersion"
          @click="installMod"
          >Install</ftb-button
        >
      </template>

      <div class="installing mt-6 mb-4" v-else-if="!finishedInstalling">
        <div class="progress font-bold"><font-awesome-icon icon="spinner" spin class="mr-2" /> Installing</div>
        <div class="stats">
          <div class="stat">
            <div class="text">Progress</div>
            <div class="value">{{ installProgress.persent }}%</div>
          </div>
          <div class="stat">
            <div class="text">Speed</div>
            <div class="value">{{ prettyBytes(installProgress.speed) }}/s</div>
          </div>

          <div class="stat">
            <div class="text">Downloaded</div>
            <div class="value">
              {{ prettyBytes(installProgress.current) }} / {{ prettyBytes(installProgress.total) }}
            </div>
          </div>
        </div>
      </div>

      <div class="done" v-else>
        <span class="block">{{ mod.name }} has been installed!</span>

        <ftb-button color="primary" class="py-2 px-6 inline-block mt-6 mb-3" @click="closeModal">Close</ftb-button>
      </div>
    </ftb-modal>
  </div>
</template>

<script lang="ts">
import { Mod } from '@/types';
import { Component, Prop, Vue } from 'vue-property-decorator';
import FTBButton from '../FTBButton.vue';
import platform from '@/utils/interface/electron-overwolf';
import { Instance } from '@/modules/modpacks/types';
import { Action } from 'vuex-class';
import Selection from '@/components/elements/Selection.vue';
import FTBModal from '../FTBModal.vue';
import MessageModal from '../modals/MessageModal.vue';
import eventBus from '@/utils/event-bus';
import { prettyByteFormat } from '@/utils/helpers';

type InstallProgress = {
  persent: number;
  speed: number;
  current: number;
  total: number;
};

@Component({
  components: {
    'ftb-button': FTBButton,
    'ftb-modal': FTBModal,
    MessageModal,
    Selection,
  },
})
export default class ModCard extends Vue {
  static emptyProgress = {
    persent: 0,
    speed: 0,
    current: 0,
    total: 0,
  };

  @Action('sendMessage') public sendMessage!: any;

  @Prop() mod!: Mod;
  @Prop() instance!: Instance;
  @Prop() target!: string;

  platform = platform;
  showInstall = false;
  selectedVersion: string | null = null;

  installing = false;
  finishedInstalling = false;
  installProgress: InstallProgress = ModCard.emptyProgress;
  wsReqId = -1;

  prettyBytes = prettyByteFormat;

  versions =
    this.mod.versions.filter(
      e => e.targets.findIndex(a => a.type === 'game' && a.name === 'minecraft' && a.version === this.target) !== -1,
    ) ?? [];

  mounted() {
    eventBus.$on('ws.message', (data: any) => {
      if (
        data.type === 'ping' ||
        data.type === 'pong' ||
        !this.installing ||
        this.wsReqId === -1 ||
        this.wsReqId !== data.requestId
      ) {
        return;
      }

      // Handle progress
      if (data.type === 'instanceInstallModProgress') {
        this.installProgress = {
          persent: data.overallPrecentage,
          speed: data.speed,
          current: data.currentBytes,
          total: data.overallBytes,
        };
      }

      // Handle completion
      if (data.type === 'instanceInstallModReply') {
        this.wsReqId = -1;
        this.installing = false;
        this.installProgress = ModCard.emptyProgress;
        this.finishedInstalling = true;
      }
    });
  }

  destroyed() {
    // Stop listening to events!
    eventBus.$off('ws.message');
  }

  installMod() {
    if (!this.selectedVersion) {
      return;
    }

    this.installing = true;
    this.sendMessage({
      payload: {
        type: 'instanceInstallMod',
        uuid: this.instance?.uuid,
        modId: this.mod.id,
        versionId: this.versions[this.versions.length - 1].id,
      },
      callback: (_: any, wsMessageId: number) => {
        this.wsReqId = wsMessageId;
      },
    });

    this.selectedVersion = null;
  }

  closeModal() {
    this.showInstall = false;
    this.finishedInstalling = false;
  }

  get art() {
    return this.mod.art[0]?.url ?? 'broken';
  }

  get latest() {
    return this.versions[this.versions.length - 1] ? this.versions[this.versions.length - 1].name : 'Unknown';
  }

  get curseLink() {
    return this.mod.links.find(e => e.type === 'curseforge');
  }
}
</script>

<style lang="scss" scoped>
.mod-card {
  display: flex;
  align-items: center;
  padding: 1rem;
  border-radius: 5px;
  background: var(--color-background);
  margin-bottom: 1rem;

  .art {
    margin-right: 1.5rem;

    img {
      max-width: 120px;
      border-radius: 5px;
    }
  }

  .main {
    flex: 1;

    .content {
      display: flex;
      justify-content: space-between;

      .name {
        font-size: 1.1rem;
        font-weight: bold;
        margin-bottom: 0.2rem;
      }

      .about {
        user-select: text;
      }

      .get {
        .btns {
          display: flex;
          align-items: center;

          .curse-btn {
            width: 25px;
            margin-right: 1rem;

            opacity: 0.5;
            transition: opacity 0.25s ease-in-out;
            cursor: pointer;

            &:hover {
              opacity: 1;
            }
          }
        }
      }
    }

    .numbers {
      display: flex;
      align-items: center;

      .stat {
        margin-right: 2rem;

        .text {
          opacity: 0.7;
        }

        .value {
          font-weight: bold;

          &.is-value {
            font-family: Arial, Helvetica, sans-serif;
          }
        }
      }
    }

    .authors {
      max-width: 200px;
      white-space: nowrap;
      text-overflow: ellipsis;
      overflow: hidden;

      user-select: text;
    }
  }

  .installing {
    .stats {
      margin-top: 1rem;
      display: flex;
      justify-content: space-between;

      .stat {
        .text {
          opacity: 0.7;
        }

        .value {
          font-weight: bold;
          font-family: Arial, Helvetica, sans-serif;
        }
      }
    }
  }
}
</style>
