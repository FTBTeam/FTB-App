<script lang="ts" setup>
import appPlatform from '@platform';
import {safeNavigate} from '@/utils';
import {RouterNames} from '@/router';
import { useAttachDomEvent } from '@/composables';
import { onMounted, ref, computed } from 'vue';
import { toTitleCase } from '@/utils/helpers/stringHelpers.ts';
import SidebarProfile from "@/components/layout/sidebar/SidebarProfile.vue";

const blurred = ref(false);
const isMac = ref(false);
const windowId = ref<string | null>(null);

useAttachDomEvent<FocusEvent>('focus', windowFocusChanged)
useAttachDomEvent<FocusEvent>('blur', windowFocusChanged)

onMounted(async () => {
  const [windowIdRes, osType] = await Promise.all([
    appPlatform.frame.getWindowId(),
    appPlatform.utils.getOsType()
  ])
  
  windowId.value = windowIdRes
  isMac.value = osType === "mac"
})

function windowFocusChanged(event: FocusEvent) {
  blurred.value = event.type === 'blur';
}

function startDragging(event: any) {
  appPlatform.frame.handleDrag(event, windowId.value);
}

function close(): void {
  // Callback only on overwolf
  appPlatform.frame.close(windowId.value, () => {
  });
}

function minMax() {
  appPlatform.frame.max(windowId.value);
}

function minimise() {
  appPlatform.frame.min(windowId.value);
}

function max() {
  appPlatform.frame.max(windowId.value);
}

function goToSettings() {
  safeNavigate(RouterNames.SETTINGS_APP);
}

const branch = computed(() => appPlatform.config.branch);
const isUnix = computed(async () => await appPlatform.utils.getOsType() !== "windows");
</script>

<template>
  <div class="titlebar" :class="{ isMac, isUnix }" @mousedown="startDragging" @dblclick="minMax">
    <div class="macos-buttons" v-if="isMac"></div>
    
    <div class="meta-title">
      <span>FTB App</span>
    </div>
    
    <div class="branch-container flex gap-2">
      <div @click="goToSettings" class="branch" v-if="branch && branch.toLowerCase() !== 'release'" aria-label="App channel" :data-balloon-pos="isMac ? 'down-right' : 'down-left'">{{ toTitleCase(branch) }}</div>
      <div v-if="appPlatform.isOverwolf" class="branch" aria-label="Overwolf Edition" :data-balloon-pos="isMac ? 'down-right' : 'down-left'">Overwolf</div>
    </div>
    
    <div class="profile">
      <SidebarProfile />
    </div>

    <div class="windows-buttons">
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
      </div >
    </div>
  </div>
</template>

<style scoped lang="scss">
.titlebar {
  height: 3rem;
  background-color: #1d1c1c;
  display: grid;
  grid-template-areas: 'branch title profile aside';
  grid-template-columns: auto 1fr auto 440px;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  z-index: 50001;
  position: relative;
  transition: background-color 0.3s ease-in-out;
  
  &.blurred {
    background-color: var(--color-navbar);
  }

  &.isUnix {
    -webkit-app-region: drag;
  }
  
  &.isMac {
    text-align: center;
    grid-template: 'icons title branch profile aside';
    grid-template-columns: 80px 1fr auto auto 440px;
    
    .meta-title {
      width: 100%;
      justify-content: center;

      img {
        margin-left: 1rem;
        margin-right: 0;
      }
    }
    
    .branch-container {
      margin-right: .4rem;
      margin-left: 0;
      justify-content: flex-end;
    }
  }

  .meta-title {
    grid-area: title;
    padding: 0 0.5rem;
    font-size: 1.1rem;
    color: rgba(white, .5);
    display: flex;
    font-weight: 800;
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

.macos-buttons {
  grid-area: icons;
}

.profile {
  grid-area: profile;
  height: 100%;
  -webkit-app-region: none;
}

.windows-buttons {
  grid-area: aside;
  background: black;
  width: 100%;
  height: 100%;
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
  grid-area: branch;
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
