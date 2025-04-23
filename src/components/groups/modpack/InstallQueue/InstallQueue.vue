<script lang="ts" setup>
import { computed, ref, useTemplateRef, watch } from 'vue';
import { useAttachDomEvent } from '@/composables';
import InstallQueueRow from '@/components/groups/modpack/InstallQueue/InstallQueueRow.vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { useInstallStore } from '@/store/installStore.ts';
import { faCircleNotch } from '@fortawesome/free-solid-svg-icons';

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
    <div class="app-install" v-if="installStore.currentInstall || installStore.installQueue.length > 0" ref="root">
      <div class="btn flex gap-2 items-center bg-green-500" @click="open = !open">
        <FontAwesomeIcon :icon="faCircleNotch" spin />
        Downloading
      </div>

      <transition name="transition-fade-and-up">
        <div class="dropdown" v-if="open">
          <div class="group" v-if="installStore.currentInstall">
            <div class="name">Installing</div>
            <InstallQueueRow class="row" :item="installStore.currentInstall" :is-install="true" />
          </div>
          <div class="group" v-if="onlyQueue.length > 0">
            <div class="name">Queue</div>
            <InstallQueueRow class="row" v-for="(item, index) in onlyQueue" :key="index" :is-next="index === 0" :item="item" :is-install="false" />
          </div>
        </div>
      </transition>
    </div>
  </transition>
</template>

<style lang="scss" scoped>
.group {
  &:not(:last-child) {
    margin-bottom: .5rem;
  }
  
  .name {
    font-weight: 600;
    font-size: .8rem;
    margin-bottom: 0.35rem;
  }
  
  .row:not(:last-child) {
    margin-bottom: 0.35rem;
  }
}

.app-install {
  position: relative;

  .btn {
    cursor: pointer;
    padding: .5rem .75rem;
    border: 1px solid rgba(white, .2);
    border-radius: 3px;
  }

  .dropdown {
    position: absolute;
    top: 120%;
    right: 0;
    z-index: 100;
    width: 510px;
    background-color: var(--color-background);
    padding: 1rem;
    border-radius: 5px;
    box-shadow: 0 5px 0.5rem rgb(0 0 0 / 20%);
    border: 1px solid #1a1a1a;
  }
}
</style>