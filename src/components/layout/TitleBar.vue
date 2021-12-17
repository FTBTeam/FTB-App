<template>
  <div class="titlebar" :class="{ isMac }" @mousedown="startDragging">
    <div class="meta-title">
      <img src="@/assets/ftb-white-logo.svg" alt="" />
      FTB App
    </div>
    <div class="action-buttons">
      <div class="icons" v-if="!isMac">
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
import { Component, Vue } from 'vue-property-decorator';
import { Action, State } from 'vuex-class';
import os from 'os';
import platform from '@/utils/interface/electron-overwolf';
import { SettingsState } from '@/modules/settings/types';

@Component
export default class TitleBar extends Vue {
  @Action('sendMessage') public sendMessage: any;
  @Action('disconnect') public disconnect: any;
  @State('settings') private settings!: SettingsState;
  @Action('saveSettings', { namespace: 'settings' }) private saveSettings!: any;
  public isMac: boolean = false;
  private windowId: string | null = null;

  public mounted() {
    if (os.type() === 'Darwin') {
      this.isMac = true;
    } else {
      this.isMac = false;
    }

    platform.get.frame.setupTitleBar(windowId => (this.windowId = windowId));
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

  public minimise(): void {
    platform.get.frame.min(this.windowId);
  }

  public max(): void {
    platform.get.frame.max(this.windowId);
  }
}
</script>

<style scoped lang="scss">
.titlebar {
  height: 2rem;
  background-color: #1d1c1c;
  display: flex;
  align-items: center;
  justify-content: space-between;
  -webkit-app-region: drag;
  z-index: 1000000000;
  position: relative;

  &.isMac {
    flex-direction: row-reverse;

    .meta-title {
      text-align: right;
    }
  }

  .meta-title {
    padding: 0 0.5rem;
    font-size: 0.875rem;
    opacity: 0.5;
    display: flex;
    font-weight: 500;

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