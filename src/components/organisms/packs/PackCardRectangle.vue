<template>
  <div class="w-1/3 md:w-1/3 lg:w-1/11 xl:w-1/11 m-2 card">
    <div v-if="!fake" style="height: 100%">
      <article class="overflow-hidden shadow-lg" style="height: 100%">
        <img
          class="w-full pack-image"
          :src="art.length > 0 ? art : '../../assets/placeholder_art.png'"
          alt="placeholder"
          :class="installing ? 'blur' : ''"
        />
        <div class="content" :class="installing ? 'hide' : ''">
          <div class="name-box">{{ name }} (v{{ version }})</div>
        </div>
        <div class="hoverContent" v-if="!installing">
          <div class="row mb-2">
            <p class="font-bold text-text-color lg:text-2xl text-center">{{ name }}</p>
          </div>
          <div class="row">
            <div class="buttons" v-if="installed">
              <font-awesome-icon
                :icon="'ellipsis-h'"
                size="3x"
                class="cursor-pointer button hover-scale lg:text-5xl sm:text-base button-shadow"
                @click="goToInstance"
              />
            </div>
            <div class="buttons" v-if="!installed">
              <font-awesome-icon
                @click="openInstall"
                :icon="'download'"
                size="3x"
                class="cursor-pointer button hover-scale lg:text-5xl sm:text-base button-shadow"
              />
              <div class="divider"></div>
              <font-awesome-icon
                :icon="'info-circle'"
                size="3x"
                class="cursor-pointer button hover-scale lg:text-5xl sm:text-base button-shadow"
                @click="openInfo"
              />
            </div>
          </div>
          <div class="row mt-2">
            <p class="font-bold text-text-color sm:text-sm lg:text-lg">v{{ version }}</p>
          </div>
        </div>
        <div class="hoverContent show" v-else>
          <div class="row mb-2">
            <p class="font-bold text-text-color lg:text-2xl text-center">Installing {{ name }}</p>
          </div>
          <div class="row">
            <font-awesome-icon
              :icon="'spinner'"
              size="5x"
              class="cursor-pointer button hover-scale lg:text-5xl sm:text-base"
              spin
            />
          </div>
        </div>
      </article>
    </div>
    <FTBModal :visible="showInstall" @dismiss-modal="hideInstall">
      <InstallModal :pack-name="name" :doInstall="install" :pack-description="description" :versions="versions" />
    </FTBModal>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Vue } from 'vue-property-decorator';
import FTBButton from '@/components/atoms/input/FTBButton.vue';
import FTBModal from '@/components/atoms/FTBModal.vue';
import SettingsModal from '@/components/organisms/modals/SettingsModal.vue';
import InformationModal from '@/components/organisms/modals/InformationModal.vue';
import InstallModal from '@/components/organisms/modals/InstallModal.vue';
import MessageModal from '@/components/organisms/modals/MessageModal.vue';
import { Action, State } from 'vuex-class';
import { Instance, ModpackState } from '@/modules/modpacks/types';
import { InstallerState } from '@/modules/app/appStore.types';
import { getPackArt } from '@/utils';

@Component({
  components: {
    FTBButton,
    FTBModal,
    SettingsModal,
    InformationModal,
    InstallModal,
    'message-modal': MessageModal,
  },
  props: [
    'packID',
    'art',
    'name',
    'installed',
    'version',
    'versionID',
    'minecraft',
    'description',
    'instanceID',
    'versions',
    'instance',
    'fake',
  ],
})
export default class PackCard extends Vue {
  @State('modpacks') public modpacks: ModpackState | undefined = undefined;
  @Action('sendMessage') public sendMessage: any;
  @Action('updateInstall', { namespace: 'modpacks' }) public updateInstall: any;
  @Action('finishInstall', { namespace: 'modpacks' }) public finishInstall: any;
  @Action('errorInstall', { namespace: 'modpacks' }) public errorInstall: any;
  @Action('storeInstalledPacks', { namespace: 'modpacks' }) public storePacks: any;
  @Action('installModpack', { namespace: 'app' }) public installModpack!: (data: InstallerState) => void;

  @Prop() public instance!: Instance;

  public name!: string;
  private showInstall = false;

  get installing() {
    return (
      this.modpacks !== undefined &&
      this.modpacks.installing !== null &&
      this.modpacks.installing.modpackID === this.$props.packID
    );
  }

  public install(name: string, version: number, versionName: string) {
    this.showInstall = false;
    this.installModpack({
      pack: {
        name,
        id: this.instance.id,
        version: version,
        packType: this.instance.packType,
      },
      meta: {
        name: this.$props.name,
        version: versionName,
        art: getPackArt(this.instance?.art),
      },
    });
  }

  public goToInstance(): void {
    this.$router.push({ name: 'instancepage', query: { uuid: this.$props.instanceID } });
  }

  public openInfo(): void {
    this.$router.push({ name: 'modpackpage', query: { modpackid: this.$props.packID } });
  }

  public openInstall(): void {
    this.showInstall = true;
  }

  public hideInstall(): void {
    this.showInstall = false;
  }
}
</script>

<style lang="scss">
.card {
  position: relative;
}

.pack-image {
  transition: filter 0.5s;
  height: 100%;
  object-fit: cover;
}

.card:hover .pack-image {
  filter: blur(5px) brightness(50%);
}

.pack-image.blur {
  filter: blur(5px) brightness(50%);
}

.card:hover .hoverContent {
  opacity: 1;
}

.hoverContent.show {
  opacity: 1;
}

.card:hover .content {
  opacity: 0;
}
.content.hide {
  opacity: 0;
}

.buttons {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}

.row {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.content {
  position: absolute;
  bottom: 0;
  width: 100%;
  color: #fff;
  opacity: 1;
  transition: opacity 0.3s;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: flex-end;
}

.content .name-box {
  background: rgba(0, 0, 0, 0.6);
  width: 100%;
  text-align: left;
  font-weight: 700;
  padding: 2px 2px 2px 6px;
}

.hoverContent {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
  color: #fff;
  opacity: 0;
  transition: opacity 0.5s;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  // margin: auto 0;
}

.divider {
  height: 20px;
  border-left: 1px solid #fff;
  border-right: 1px solid #fff;
  margin: 0 20px;
}

.button {
  transition: transform 0.2s ease-in;
}

.hover-scale:hover {
  transform: scale(1.3);
}

.button-shadow {
  // text-shadow: 3px 6px #272634;
  filter: drop-shadow(10px 10px 5px rgba(0, 0, 0, 0.8));
}
</style>
