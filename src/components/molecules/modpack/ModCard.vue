<template>
  <div class="mod-card-wrapper">
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
            <ftb-button color="primary" class="px-6 py-2" @click="showInstall = true">Install</ftb-button>
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
              {{ latest.length > 20 ? latest.substring(0, 20) + '...' : latest }}
            </div>
          </div>
          <div class="stat">
            <div class="text">Author{{ mod.authors.length > 1 ? 's' : '' }}</div>
            <div class="value authors">
              {{ mod.authors.map((e) => e.name).join(', ') }}
            </div>
          </div>
          <div class="curse-btn" v-if="curseLink.link" @click="() => platform.get.utils.openUrl(curseLink.link)">
            <img src="@/assets/curse-logo.svg" alt="" />
          </div>
        </div>
      </div>  
    </div>
  
    <modal :open="showInstall" @closed="closeModal" :close-on-background-click="!installing"
      :title="mod ? mod.name : 'Loading...'" sub-title="Select the version you want to install" :external-contents="true"
    >
      <modal-body>
        <div class="py-6" v-if="!installing && !finishedInstalling">
          <div class="flex gap-4 mb-4">
            <font-awesome-icon icon="download" size="xl" />
            <b class="text-lg block">Install {{ mod.name }}</b>
          </div>
          <selection
            v-if="versions"
            label="Selection mod version"
            @selected="(e) => (selectedVersion = e)"
            :options="
              versions.map((e) => ({
                value: e.id,
                text: e.name,
                badge: {
                  text: e.type,
                  color: getColorForReleaseType(e.type),
                },
                meta: prettyBytes(e.size),
              }))
            "
          />
        </div>
        
        <div v-if="!installing && !finishedInstalling" class="pt-8">
          <div class="flex items-center gap-4 mb-6">
            <img src="@/assets/curse-logo.svg" alt="CurseForge logo" width="40" />
            <b class="text-lg block">About mod installs</b>
          </div>
          <hr class="curse-border block mb-4" />
          <p class="text-muted mb-4">🎉 Each mod install from the FTB App directly supports the mod developers through the CurseForge reward system!</p>
          <b class="block mb-2">Dependencies</b>
          <p class="mb-2 text-muted">If the mod depends on other mods on the CurseForge platform, the app will try and resolve these dependencies for you and install them as well.</p>
          <p class="text-muted">Sometimes this does not work and you'll have to install the dependencies manually.</p>
        </div>
        
        <div class="installing mt-6 mb-4" v-if="!finishedInstalling && installing"> 
          <div class="progress font-bold"><font-awesome-icon icon="spinner" spin class="mr-2" /> Installing</div>
          <div class="stats">
            <div class="stat">
              <div class="text">Progress</div>
              <div class="value">{{ installProgress.percentage }}%</div>
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
        
        <p v-if="!installing && finishedInstalling">
          <span class="block">{{ mod.name }} has been installed!</span>
        </p>
      </modal-body>
      
      <modal-footer>
        <div class="flex justify-end gap-4" v-if="!installing || !finishedInstalling">
          <ui-button type="success" icon="download" v-if="!installing && !finishedInstalling" :disabled="!selectedVersion" @click="installMod">Install</ui-button>
          <ui-button type="primary" @click="closeModal" icon="check" v-if="finishedInstalling">Close</ui-button>
        </div>
      </modal-footer>
    </modal>
  </div>
</template>

<script lang="ts">
import {Mod, ModVersion} from '@/types';
import {Component, Prop, Vue} from 'vue-property-decorator';
import platform from '@/utils/interface/electron-overwolf';
import {Instance} from '@/modules/modpacks/types';
import Selection from '@/components/atoms/input/Selection.vue';
import {emitter} from '@/utils/event-bus';
import {prettyByteFormat} from '@/utils/helpers';
import {getColorForReleaseType} from '@/utils/colors';
import {sendMessage} from '@/core/websockets/websocketsApi';
import UiButton from '@/components/core/ui/UiButton.vue';

type InstallProgress = {
  percentage: number;
  speed: number;
  current: number;
  total: number;
};

@Component({
  components: {
    UiButton,
    Selection,
  },
})
export default class ModCard extends Vue {
  static emptyProgress = {
    percentage: 0,
    speed: 0,
    current: 0,
    total: 0,
  };

  @Prop() mod!: Mod;
  @Prop() instance!: Instance;
  @Prop() target!: string;

  platform = platform;
  showInstall = false;
  selectedVersion: string | null = null;

  installing = false;
  finishedInstalling = false;
  installProgress: InstallProgress = ModCard.emptyProgress;
  wsReqId = "";

  getColorForReleaseType = getColorForReleaseType;
  prettyBytes = prettyByteFormat;

  versions: ModVersion[] = [];

  mounted() {
    this.versions =
      this.mod.versions
        .filter(
          (e) =>
            e.targets.findIndex((a) => a.type === 'game' && a.name === 'minecraft' && a.version === this.target) !== -1,
        )
        .sort((a, b) => b.id - a.id) ?? [];

    emitter.on('ws.message', this.onInstallMessage);
  }

  onInstallMessage(data: any) {
    if (!this.installing || this.wsReqId !== data.requestId) {
      return;
    }

    // Handle progress
    if (data.type === 'instanceInstallModProgress') {
      this.installProgress = {
        percentage: data.overallPrecentage,
        speed: data.speed,
        current: data.currentBytes,
        total: data.overallBytes,
      };
    }

    // Handle completion
    if (data.type === 'instanceInstallModReply') {
      this.wsReqId = "";
      this.installing = false;
      this.installProgress = ModCard.emptyProgress;
      this.finishedInstalling = true;
      this.$emit('modInstalled');
    }
  }

  destroyed() {
    // Stop listening to events!
    emitter.off('ws.message', this.onInstallMessage);
  }

  async installMod() {
    if (!this.selectedVersion) {
      return;
    }

    this.installing = true;
    const result = await sendMessage("instanceInstallMod", {
      uuid: this.instance?.uuid,
      modId: this.mod.id,
      versionId: parseInt(this.selectedVersion, 10),
    })
    
    this.wsReqId = result.messageId;
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
    return this.mod.links.find((e) => e.type === 'curseforge');
  }
}
</script>

<style lang="scss" scoped>
.curse-border {
  border-color: var(--curse-color);
  border-width: 2px;
}

.mod-card {
  display: flex;
  align-items: center;
  padding: 1rem;
  border-radius: 5px;
  background: rgba(white, 0.04);
  margin-bottom: 1rem;

  .art {
    margin-right: 1.5rem;
    min-width: 120px;

    img {
      max-width: 120px;
      border-radius: 5px;
      margin: 0 auto;
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
        }
      }

      .curse-btn {
        width: 25px;
        margin-right: 0.5rem;
        margin-left: auto;
        align-self: flex-end;

        opacity: 0.5;
        transition: opacity 0.25s ease-in-out;
        cursor: pointer;

        &:hover {
          opacity: 1;
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
        }
      }
    }
  }
}
</style>
