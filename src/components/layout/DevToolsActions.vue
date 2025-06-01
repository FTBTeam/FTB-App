<script lang="ts" setup>
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import appPlatform from '@platform';
import { useAttachDomEvent } from '@/composables';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { ref } from 'vue';
import { useAppSettings } from '@/store/appSettingsStore.ts';
import { faBars, faCode, faFire, faHome, faWrench } from '@fortawesome/free-solid-svg-icons';

const appSettings = useAppSettings();

useAttachDomEvent<MouseEvent>('click', (event) => {
  if ((event.target as any)?.closest('.dev-tools-actions')) return;
  open.value = false;
})

const open = ref(false)

function openDebugger() {
  gobbleError(() => sendMessage("openDebugTools", {}))
}

function openDevTools() {
  appPlatform.utils.openDevTools();
}

const devModeEnabled = !import.meta.env.PROD;
</script>

<template>
 <div class="dev-tools-actions" v-if="devModeEnabled">
   <div class="trigger cursor-pointer" @click="open = !open">
     <FontAwesomeIcon :icon="faCode" />
   </div>
   <div class="options flex items-center gap-4" v-show="open">
     <div class="divider ml-4" />
     <router-link class="item" :to="{ name: 'home' }">
       <FontAwesomeIcon :icon="faHome" />
     </router-link>
     <FontAwesomeIcon class="item" :icon="faFire" @click="openDebugger"/>
     <FontAwesomeIcon class="bars" :icon="faBars" @click="() => appSettings.toggleDebugAds()" />
     <FontAwesomeIcon :icon="faWrench" @click="openDevTools" />
   </div>
 </div>
</template>

<style lang="scss" scoped>
.dev-tools-actions {
  position: fixed;
  padding: 0.5rem 1rem;
  bottom: 9rem;
  background-color: black;
  z-index: 1000;
  border-radius: 5px;
  display: flex;
  align-items: center;
  left: .6rem;

  .divider {
    height: 1rem;
    width: 1px;
    background-color: white;
    opacity: 0.5;
  }
  
  span {
    margin-right: 1rem;
    font-size: 0.8rem;
    opacity: 0.5;
  }

  .item {
    display: block;
    transition: 0.2s ease-in-out transform;

    &:hover {
      transform: scale(1.1);
    }
  }
}
</style>