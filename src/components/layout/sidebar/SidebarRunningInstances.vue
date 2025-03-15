<template>
 <div class="sidebarRunningInstances flex justify-center mt-4" @click="panelOpen = !panelOpen" v-if="runningWithData.length > 0">
   <div class="relative cursor-pointer" style="max-width: 40px; max-height: 40px;">
     <img class="rounded border border-gray-700" v-if="firstLoadedInstance.instance" :src="firstLoadedInstance.instance.art" />
     <div class="bg-gray-600 bg-opacity-75 font-bold rounded text-xs absolute text-center" style="min-width: 1.5em; bottom: -.25rem; right: -.25rem;">{{runningWithData.length}}</div>
     
     <div class="absolute z-50 instance-popout p-4 rounded cursor-default" :class="{'open': panelOpen}" style="max-height: 400px" @click.stop>
       <p class="font-bold mb-6 text-lg">Running instances</p>
       <div class="flex flex-col gap-6">
         <div v-for="(running, index) in runningWithData" :key="index" class="item cursor-pointer" @click="() => showRunningInstance(running.uuid)">
           <div class="img"><img class="rounded" v-if="running.instance" :src="running.instance.art" width="50" alt="Pack art"/></div>
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

<script lang="ts">
import {ns} from '@/core/state/appState';
import {InstanceRunningData} from '@/core/state/misc/runningState';
import {InstanceJson} from '@/core/@types/javaApi';
import {RouterNames} from '@/router';

@Component
export default class SidebarRunningInstances extends Vue {
  @State("instances", ns("v2/running")) public runningInstances!: InstanceRunningData[]
  @State("instances", ns("v2/instances")) public instances!: InstanceJson[]

  panelOpen = false;
  
  mounted() {
    document.addEventListener('click', this.clickOffHandler);
  }
  
  destroyed() {
    document.removeEventListener('click', this.clickOffHandler);
  }
  
  clickOffHandler(event: any) {
    if (!this.$el.contains(event.target)) {
      this.panelOpen = false;
    }
  }
  
  async showRunningInstance(uuid: string) {
    console.log('showRunningInstance', uuid)
    if (this.$route.fullPath === `/running/${uuid}`) return;
    
    await this.$router.push({ name: RouterNames.ROOT_RUNNING_INSTANCE, params: { uuid } });
  }
  
  get firstLoadedInstance() {
    return this.runningWithData[0]
  }
  
  get runningWithData(): ({ instance: InstanceJson | undefined } & InstanceRunningData)[] {
    return  this.runningInstances
      .map(e => ({
        ...e,
        instance: this.instances.find(i => i.uuid === e.uuid)
      }))
  }
}
</script>

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
