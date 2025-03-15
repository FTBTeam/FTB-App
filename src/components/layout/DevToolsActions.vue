<script lang="ts" setup>
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import Platform from '@/utils/interface/electron-overwolf';
import { useAttachDomEvent } from '@/composables';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';

// TODO: [port] fixme
// @Action('toggleDebugDisableAdAside', { namespace: 'core' }) toggleDebugDisableAdAside!: () => void;
const toggleDebugDisableAdAside = () => {
  console.log('toggleDebugDisableAdAside');
}

useAttachDomEvent<MouseEvent>('click', (event) => {
  if (event.target.closest('.dev-tools-actions')) return;
  open.value = false;
})

const open = ref(false)
const platform = Platform

function openDebugger() {
  gobbleError(() => sendMessage("openDebugTools", {}))
}

function openDevTools() {
  platform.get.utils.openDevTools();
}

// TODO: [port] this should likely use import.meta.env
const devModeEnabled = process.env.NODE_ENV === 'development';
</script>

<template>
 <div class="dev-tools-actions" v-if="devModeEnabled">
   <div class="trigger cursor-pointer" @click="open = !open">
     <FontAwesomeIcon icon="code" />
   </div>
   <div class="options flex items-center gap-4" v-show="open">
     <div class="divider ml-4" />
     <router-link class="item" :to="{ name: 'home' }">
       <FontAwesomeIcon icon="home" />
     </router-link>
     <FontAwesomeIcon class="item" icon="fire" @click="openDebugger"/>
     <FontAwesomeIcon class="bars" icon="bars" @click="toggleDebugDisableAdAside" />
     <FontAwesomeIcon icon="wrench" @click="openDevTools" />
   </div>
 </div>
</template>

<style lang="scss" scoped>
.dev-tools-actions {
  position: fixed;
  padding: 0.5rem 1rem;
  bottom: 1rem;
  background-color: black;
  z-index: 1000;
  border-radius: 5px;
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  left: 5rem;

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