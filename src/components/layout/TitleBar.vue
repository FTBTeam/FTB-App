<template>
  <div class="titlebar" :class="{ isMac }" @mousedown="startDragging" @dblclick="minMax">
    <div class="spacer" v-if="isMac"></div>
    <div class="meta-title">
      <span>FTB App</span>
    </div>
    <div class="branch-container">
      <div @click="goToSettings" class="branch" v-if="branch && branch.toLowerCase() !== 'release'" aria-label="App channel" data-balloon-pos="down-right">{{ branch }}</div>
    </div>
    <div class="action-buttons" v-if="!isMac">
      <div class="icons">
        <div class="title-action close" @click="close">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="12"
            height="12"
            viewBox="0 0 12 12"
            class="pointer-events-none"
          >
            <line x1="0.71" y1="11.12" x2="11.11" y2="0.72" stroke-width="2" />
            <line x1="0.77" y1="0.71" x2="11.18" y2="11.12" stroke-width="2" />
          </svg>
        </div>
        <div class="title-action" @click="max">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            width="12"
            height="12"
            viewBox="0 0 12 12"
            class="pointer-events-none"
          >
            <rect x="1" y="1" width="10" height="10" stroke-width="2" />
          </svg>
        </div>
        <div class="title-action" @click="minimise">
          <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 12 1" class="pointer-events-none">
            <line y1="0.5" x2="11" y2="0.5" stroke-width="2" />
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {Action, State} from 'vuex-class';
import platform from '@/utils/interface/electron-overwolf';
import {SettingsState} from '@/modules/settings/types';
import os from 'os';
import {safeNavigate} from '@/utils';
import {RouterNames} from '@/router';

@Component
export default class TitleBar extends Vue {
  @Action('disconnect') public disconnect: any;
  @State('settings') private settings!: SettingsState;
  @Action('saveSettings', { namespace: 'settings' }) private saveSettings!: any;
  @Action('toggleDebugDisableAdAside', { namespace: 'core' }) toggleDebugDisableAdAside!: () => void;
  
  public isMac: boolean = false;
  private windowId: string | null = null;
  
  public mounted() {
    this.isMac = os.type() === 'Darwin';

    platform.get.frame.setupTitleBar((windowId) => (this.windowId = windowId));
  }

  public startDragging(event: any) {
    platform.get.frame.handleDrag(event, this.windowId);
  }

  public close(): void {
    // Callback only on overwolf
    platform.get.frame.close(this.windowId, () => {
      this.saveSettings(this.settings?.settings);
      this.disconnect();
    });
  }

  minMax() {
    platform.get.frame.max(this.windowId);
  }
  
  public minimise(): void {
    platform.get.frame.min(this.windowId);
  }

  public max(): void {
    platform.get.frame.max(this.windowId);
  }
  
  get branch() {
    return platform.get.config.branch
  }

  goToSettings() {
    safeNavigate(RouterNames.SETTINGS_INFO)
  }
}
</script>

<style scoped lang="scss">
.titlebar {
  height: 2rem;
  background-color: #1d1c1c;
  display: grid;
  grid-template: 'left center right';
  grid-template-columns: 1fr 1fr 1fr;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  -webkit-app-region: drag;
  z-index: 50000;
  position: relative;
  transition: background-color 0.3s ease-in-out;

  &.isMac {
    height: 1.8em;
    text-align: center;

    .spacer {
      grid-area: left;
      width: 50px;
    }
    
    .meta-title {
      font-weight: 800;
      width: 100%;
      justify-content: center;

      img {
        margin-left: 1rem;
        margin-right: 0;
      }
    }
    
    .branch-container {
      grid-area: right;
      margin-right: .4rem;
      margin-left: 0;
      justify-content: flex-end;
    }
  }

  .meta-title {
    grid-area: center;
    padding: 0 0.5rem;
    font-size: 0.875rem;
    color: rgba(white, .5);
    display: flex;
    font-weight: 500;
    align-items: center;
    gap: 1.5rem;
    justify-content: center;

    img {
      height: 18px;
      margin-right: 0.8rem;
    }
  }

  .icons {
    display: flex;
    flex-direction: row-reverse;
  }

  user-select: none;
}

.branch {
  font-size: 10px;
  background-color: rgba(white, .2);
  color: white;
  border-radius: 4px;
  font-weight: normal;
  padding: .1rem .3rem;
  white-space: nowrap;
  display: inline-block;
}

.branch-container {
  grid-area: left;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin-left: .4rem;
}

.title-action {
  -webkit-app-region: no-drag;
  cursor: pointer;
  display: flex;
  flex-direction: row;
  align-items: center;
  height: 2rem;
  margin-right: 5px;
  padding: 0 1rem;
  fill: none;
  stroke: #989898;
  transition: background-color 0.25s ease-in-out;

  &:hover {
    background-color: #414141;
  }

  &.close {
    margin-right: 0;
  }

  &.close:hover {
    stroke: #fff;
    background-color: #fc3636;
  }
}
</style>
