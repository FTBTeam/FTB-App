<script lang="ts" setup>
import platform from '@/utils/interface/electron-overwolf';
import {safeNavigate} from '@/utils';
import {RouterNames} from '@/router';
import { useAttachDomEvent } from '@/composables';
import { onMounted, ref, computed } from 'vue';
import { toTitleCase } from '@/utils/helpers/stringHelpers.ts';

const blurred = ref(false);
const isMac = ref(false);
const windowId = ref<string | null>(null);

useAttachDomEvent<FocusEvent>('focus', windowFocusChanged)
useAttachDomEvent<FocusEvent>('blur', windowFocusChanged)

onMounted(async () => {
  const [windowIdRes, osType] = await Promise.all([
    platform.get.frame.getWindowId(),
    platform.get.utils.getOsType()
  ])
  
  windowId.value = windowIdRes
  isMac.value = osType === "mac"
})

function windowFocusChanged(event: FocusEvent) {
  blurred.value = event.type === 'blur';
}

function startDragging(event: any) {
  platform.get.frame.handleDrag(event, windowId.value);
}

function close(): void {
  // Callback only on overwolf
  platform.get.frame.close(windowId.value, () => {
  });
}

function minMax() {
  platform.get.frame.max(windowId.value);
}

function minimise() {
  platform.get.frame.min(windowId.value);
}

function max() {
  platform.get.frame.max(windowId.value);
}

function goToSettings() {
  safeNavigate(RouterNames.SETTINGS_APP);
}

const branch = computed(() => platform.get?.config?.branch);
const isUnix = computed(() => !platform.isOverwolf());
</script>

<template>
  <div class="titlebar" :class="{ isMac, isUnix }" @mousedown="startDragging" @dblclick="minMax">
    <div class="spacer" v-if="isMac"></div>
    <div class="meta-title">
      <span>FTB App</span>
    </div>
    <div class="branch-container">
      <div @click="goToSettings" class="branch" v-if="branch && branch.toLowerCase() !== 'release'" aria-label="App channel" :data-balloon-pos="isMac ? 'down-right' : 'down-left'">{{ toTitleCase(branch) }}</div>
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
  z-index: 50001;
  position: relative;
  transition: background-color 0.3s ease-in-out;

  .system-frame & {
    display: none;
  }
  
  &.blurred {
    background-color: var(--color-navbar);
  }

  &.isUnix {
    -webkit-app-region: drag;
  }
  
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
