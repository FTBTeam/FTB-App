<script lang="ts" setup>
import CreateInstance from '@/components/groups/modpack/create/CreateInstance.vue';
import {safeNavigate} from '@/utils';
import {RouterNames} from '@/router';
import { onMounted, onUnmounted, ref } from 'vue';
import Popover from '@/components/ui/Popover.vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faCirclePlus, faPlus, faSearch } from '@fortawesome/free-solid-svg-icons';
import {useGlobalStore} from "@/store/globalStore.ts";
import appPlatform from '@platform';
import {useAppStore} from "@/store/appStore.ts";
import {alertController} from "@/core/controllers/alertController.ts";

const { disabled } = defineProps<{
  disabled?: boolean;
}>()

const globalStore = useGlobalStore();
const appStore = useAppStore();

const open = ref(false)

onMounted(() => {
  document.addEventListener('click', close)
})

onUnmounted(() => {
  document.removeEventListener('click', close)
})

function selectCurseFileToImport() {
  appPlatform.io.selectFileDialog('*.zip', async file => {
    if (!file || !file.endsWith('.zip')) {
      alertController.warning('Please select a valid .zip file.')
      return;
    }
    
    await appStore.controllers.install.requestImport(file, "Default")
  })
}

function close(event: any) {
  if (event.target.closest('.create-instance-container')) return
  open.value = false
}
</script>

<template>
  <div class="create-instance-container">
    <Popover :text="open ? '' : 'Create an instance'">
      <div class="action-icon" :class="{disabled}" @click="() => {
        if (!disabled) {
          open = true
        }
      }">
        <FontAwesomeIcon :icon="faPlus" />
      </div>
    </Popover>
    <div class="dropdown" :class="{active: open}">
      <div class="options">
        <div class="option create" @click="() => {
           open = false;
           globalStore.updateCreateInstanceVisibility(true)
        }">
          <div class="icon"><FontAwesomeIcon :icon="faCirclePlus" /></div>
          <p>Create instance</p>
          <small>Build your own vanilla or modded experience</small>
        </div>
        <div class="option search" @click="() => {
          safeNavigate(RouterNames.ROOT_BROWSE_PACKS)
          open = false;
        }">
          <div class="icon"><FontAwesomeIcon :icon="faSearch" /></div>
          <p>Search for Modpack</p>
          <small>Search FTB Modpacks or CurseForge</small>
        </div>
        <div class="separator"></div>
<!--        <div class="option import" @click="() => {-->
<!--          open = false;-->
<!--          importOpen = true;-->
<!--        }">-->
<!--          <div class="icon"><FontAwesomeIcon icon="file-import" /></div>-->
<!--          <p>Import</p>-->
<!--          <small>Import instances from other launchers</small>-->
<!--        </div>-->
        <div class="option curse" @click="selectCurseFileToImport">
          <div class="icon">
            <img src="@/assets/curse-logo.svg" width="20" alt="" />
          </div>
          <p>Curse Import</p>
          <small>Import a CurseForge .zip file</small>
        </div>
      </div>
    </div>
    
    <create-instance :open="globalStore.createInstanceVisible" @close="globalStore.updateCreateInstanceVisibility(false)" />
  </div>
</template>

<style scoped lang="scss">
@import 'tailwindcss/theme' theme(reference);

.create-instance-container {
  position: relative;
  margin-top: 1rem;
  margin-bottom: .5rem;
  
  .action-icon {
    padding: .7rem 1rem;
    background-color: black;
    border-radius: 8px;
    cursor: pointer;
    
    transition: background-color .25s ease-in-out;
    
    &.disabled {
      opacity: .5;
      cursor: not-allowed;
    }
    
    &:hover {
      background-color: var(--color-green-600);
    }
  }
  
  .dropdown {
    position: absolute;
    left: calc(100% + 1.5rem);
    width: 365px;
    background-color: var(--color-navbar);
    border-radius: 8px;
    border: 1px solid rgba(white, .2);
    padding: .25rem 0;
    z-index: 200;
    top: 0;
    font-size: 14px;
    transform: translateX(-10px);
    opacity: 0;
    visibility: hidden;
    transition: transform .25s ease-in-out, opacity .25s ease-in-out, visibility .25s ease-in-out;
    
    &.active {
      transform: translateX(0);
      opacity: 1;
      visibility: visible;
    }
    
    .options {
      display: flex;
      flex-direction: column;
      gap: .25rem;
      
      .separator {
        height: 1px;
        background-color: rgba(white, .2);
      }
      
      .option {
        display: grid;
        grid-template: 'icon content' 
                       'icon content2';
        
        align-items: center;
        
        grid-template-columns: auto 1fr;
        gap: 0 1rem;
        padding: .75rem;
        margin: 0 .25rem;
        cursor: pointer;
        border-radius: 6px;
        
        transition: background-color .25s ease-in-out;

        &.create:hover { background-color: hsl(190deg 73% 58% / 15%); }
        &.search:hover { background-color: hsl(245deg 73% 58% / 15%); }
        &.import:hover { background-color: hsl(100deg 73% 58% / 15%); }
        &.share:hover { background-color: hsl(290deg 73% 58% / 15%); }
        &.curse:hover { background-color: hsl(var(--curse-color-hsl) / 15%); }
        
        .icon {
          --size: 2.5rem;
          
          grid-area: icon;
          background-color: black;
          border-radius: 8px;
          display: flex;
          align-items: center;
          justify-content: center;
          width: var(--size);
          height: var(--size);
          border: 1px solid rgba(white, .2)
        }
        
        &.create .icon { background-color: hsl(190deg 73% 58% / 35%); }
        &.search .icon { background-color: hsl(245deg 73% 58% / 35%); }
        &.import .icon { background-color: hsl(100deg 73% 58% / 35%); }
        &.share .icon { background-color: hsl(290deg 73% 58% / 35%); }
        &.curse .icon { background-color: var(--curse-color); }
        
        p {
          grid-area: content;
          font-weight: bold;
        }
        
        small {
          grid-area: content2;
          opacity: .8;
          font-size: 0.875em;
        }
      }
    }
  }
}
</style>