<script lang="ts" setup>
import { computed, ref, useTemplateRef, watch } from 'vue';
import { useAttachDomEvent } from '@/composables';
import InstallQueueRow from '@/components/groups/modpack/InstallQueue/InstallQueueRow.vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { useInstallStore } from '@/store/installStore.ts';
import {faDownload} from '@fortawesome/free-solid-svg-icons';

const installStore = useInstallStore();

const open = ref(false)
const rootElm = useTemplateRef("root");

watch(() => installStore.currentInstall, () => {
  if (installStore.installQueue?.length === 0 && installStore.currentInstall) {
    open.value = true;
  }
})

useAttachDomEvent<MouseEvent>('click', close);

function close(event: MouseEvent) {
  if (!rootElm.value?.contains(event.target as any)) {
    open.value = false;
  }
}

const onlyQueue = computed(() => {
  return installStore.installQueue.filter(e => e.uuid !== installStore.currentInstall?.request.uuid)
})
</script>

<template>
  <transition name="transition-fade">
    <div class="app-install my-2" v-if="installStore.currentInstall || installStore.installQueue.length > 0" ref="root">
      <div class="btn flex items-center" @click="open = !open">
        <div class="relative">
          <FontAwesomeIcon :icon="faDownload" />
          <div class="absolute -bottom-4 -right-4 inline-block bg-black/40 px-1 text-sm rounded">
            {{ onlyQueue.length  + (installStore.currentInstall ? 1 : 0) }}
          </div>
        </div>
      </div>
      
      <transition name="transition-fade-and-up">
        <div class="dropdown" v-if="open">
          <div class="group" v-if="installStore.currentInstall">
            <div class="name">Installing</div>
            <InstallQueueRow class="row" :entry="installStore.currentInstall" :is-install="true" />
          </div>
          <div class="group" v-if="onlyQueue.length > 0">
            <div class="name">Queue</div>
            <InstallQueueRow class="row" v-for="(item, index) in onlyQueue" :key="index" :is-next="index === 0" :entry="item" :is-install="false" />
          </div>
        </div>
      </transition>
    </div>
  </transition>
</template>

<style lang="scss" scoped>
.group {
  &:not(:last-child) {
    margin-bottom: .85rem;
  }
  
  .name {
    font-weight: 800;
    margin-bottom: 1rem;
  }
  
  .row:not(:last-child) {
    margin-bottom: 0.85rem;
  }
}

.app-install {
  position: relative;

  .btn {
    cursor: pointer;
    background: #1a1a1a;

    display: flex;
    align-items: center;
    justify-content: center;
    padding: .6rem .9rem;
    border-radius: 6px;
    width: auto !important;
    font-size: 18px;
    
    svg {
      max-width: unset !important;
      margin: 0;
    }
  }

  .dropdown {
    position: absolute;
    top: 0;
    left: 120%;
    z-index: 100;
    width: 390px;
    background-color: var(--color-background);
    padding: 1rem;
    border-radius: 5px;
    box-shadow: 0 5px 0.5rem rgb(0 0 0 / 20%);
    border: 1px solid #1a1a1a;
  }
}
</style>