<template>
 <div class="dev-tools-actions" v-if="devModeEnabled">
     <div class="trigger cursor-pointer" @click="open = !open">
       <font-awesome-icon icon="code" />
     </div>
     <div class="options flex items-center gap-4" v-show="open">
       <div class="divider ml-4" />
       <router-link class="item" :to="{ name: 'home' }">
         <font-awesome-icon icon="home" />
       </router-link>
       <font-awesome-icon class="item" icon="fire" @click="openDebugger"/>
       <font-awesome-icon class="bars" icon="bars" @click="toggleDebugDisableAdAside" />
       <font-awesome-icon icon="wrench" @click="openDevTools" />
     </div>
 </div>
</template>

<script lang="ts">
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {sendMessage} from '@/core/websockets/websocketsApi';
import platform from '@/utils/interface/electron-overwolf';

@Component
export default class DevToolsActions extends Vue {
  @Action('toggleDebugDisableAdAside', { namespace: 'core' }) toggleDebugDisableAdAside!: () => void;
  
  open = false;
  platform = platform;
  
  mounted() {
    document.addEventListener('click', this.onBackgroundClick)
  }
  
  destroyed() {
    document.removeEventListener('click', this.onBackgroundClick)
  }
  
  onBackgroundClick(event: any) {
    if (event.target.closest('.dev-tools-actions')) return;
    this.open = false;
  }
  
  openDebugger() {
    gobbleError(() => sendMessage("openDebugTools", {}))
  }

  openDevTools() {
    this.platform.get.utils.openDevTools();
  }
  
  get devModeEnabled() {
    return process.env.NODE_ENV === 'development';
  }
}
</script>

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