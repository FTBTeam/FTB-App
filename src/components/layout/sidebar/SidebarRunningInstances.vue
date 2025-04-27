<script lang="ts" setup>
import {RouterNames} from '@/router';
import { useTemplateRef, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAttachDomEvent } from '@/composables';
import { useRunningInstancesStore } from '@/store/runningInstancesStore.ts';
import { useInstanceStore } from '@/store/instancesStore.ts';
import { artworkFileOrElse } from '@/utils/helpers/packHelpers.ts';

const router = useRouter()
const runningInstancesStore = useRunningInstancesStore();
const instanceStore = useInstanceStore();

const panelOpen = ref(false);
const elm = useTemplateRef('root');

useAttachDomEvent<MouseEvent>('click', (event) => {
  if (event.target && !elm.value?.contains(event.target as any)) {
    panelOpen.value = false;
  }
})

async function showRunningInstance(uuid: string) {
  if (router.currentRoute.value.fullPath === `/running/${uuid}`) return;

  await router.push({ name: RouterNames.ROOT_RUNNING_INSTANCE, params: { uuid } });
}

const firstLoadedInstance = computed(() => runningWithData.value[0])
const runningWithData = computed(() => runningInstancesStore.instances
  .map(e => ({
    ...e,
    instance: instanceStore.instances.find(i => i.uuid === e.uuid)
  })))

function navigateToOrOpen() {
  // Failsafe if one instance closes and the panel is open but we're left with one instance
  if (panelOpen.value) {
    panelOpen.value = false;
    return;
  }
  
  if (runningInstancesStore.instances.length > 1) {
    panelOpen.value = !panelOpen.value;
  } else {
    showRunningInstance(runningInstancesStore.instances[0].uuid);
  }
}
</script>

<template>
 <div class="sidebarRunningInstances flex justify-center mt-4" @click="navigateToOrOpen" v-if="runningWithData.length > 0" ref="root">
   <div class="relative cursor-pointer" style="max-width: 40px; max-height: 40px;">
     <img class="rounded border border-gray-700" v-if="firstLoadedInstance.instance" :src="artworkFileOrElse(firstLoadedInstance.instance)" />
     <div class="bg-gray-600 bg-opacity-75 font-bold rounded text-xs absolute text-center" style="min-width: 1.5em; bottom: -.25rem; right: -.25rem;">{{runningWithData.length}}</div>
     
     <div class="absolute z-50 instance-popout p-4 rounded cursor-default" :class="{'open': panelOpen}" style="max-height: 400px" @click.stop>
       <p class="font-bold mb-6 text-lg">Running instances</p>
       <div class="flex flex-col gap-6">
         <div v-for="(running, index) in runningWithData" :key="index" class="item cursor-pointer" @click="() => showRunningInstance(running.uuid)">
           <div class="img"><img class="rounded" v-if="running.instance" :src="artworkFileOrElse(running.instance)" width="50" alt="Pack art"/></div>
           <div class="flex flex-col">
             <div class="font-bold name">{{ running.instance?.name }}</div>
             <p class="text-muted text-sm">{{ running.status.finishedLoading ? 'Loaded' : 'Loading...' }}</p>
           </div>
         </div>
       </div>
       </div>
   </div>
 </div>
</template>

<style lang="scss" scoped>
.instance-popout {
  width: 280px;
  background: rgba(black, .8);
  backdrop-filter: blur(5px);
  left: 160%;
  opacity: 0;
  visibility: hidden;
  bottom: -2rem;
  border-radius: .5rem;
  border: 1px solid rgba(white, .2);
  overflow-y: auto;
  
  transition: left .25s ease-in-out, opacity .25s ease-in-out, visibility .25s ease-in-out;
  
  &.open {
    left: 120%;
    opacity: 1;
    visibility: visible;
  }
  
  .item {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr);
    gap: 1rem;
    align-items: center;
    
    .img {
      width: 50px;
      height: 50px;
    }
  }
  
  .name {
    display: block;
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
    max-width: 100%;
  }
}
</style>
