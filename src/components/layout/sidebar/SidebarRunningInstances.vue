<template>
 <div class="sidebarRunningInstances flex justify-center mt-4" v-if="runningInstances.length > 0">
   <div class="relative cursor-pointer" style="max-width: 40px; max-height: 40px;">
     <img class="rounded border border-gray-700" v-if="firstLoadedInstance.instance" :src="firstLoadedInstance.instance.art" />
     <div class="bg-gray-600 bg-opacity-75 font-bold rounded text-xs absolute text-center" style="min-width: 1.5em; bottom: -.25rem; right: -.25rem;">{{runningInstances.length}}</div>
     
     <div class="absolute z-50 instance-popout p-4 rounded">
       <div v-for="(running, index) in runningWithData" :key="index" class="flex gap-4" @click="() => showRunningInstance(running.uuid)">
         <img class="rounded" v-if="running.instance" :src="running.instance.art" width="60" />
         <div class="flex-1">
           <div class="font-bold name">{{ running.instance?.name }}</div>
           <p class="text-muted">{{ running.status.finishedLoading ? 'Loaded' : 'Loading...' }}</p>
         </div>
       </div>
     </div>
   </div>
 </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {State} from 'vuex-class';
import {ns} from '@/core/state/appState';
import {InstanceRunningData} from '@/core/state/misc/runningState';
import {InstanceJson} from '@/core/@types/javaApi';
import {RouterNames} from '@/router';

@Component
export default class SidebarRunningInstances extends Vue {
  @State("instances", ns("v2/running")) public runningInstances!: InstanceRunningData[]
  @State("instances", ns("v2/instances")) public instances!: InstanceJson[]

  async showRunningInstance(uuid: string) {
    console.log('showRunningInstance', uuid)
    if (this.$route.fullPath === `/running/${uuid}`) return;
    
    await this.$router.push({ name: RouterNames.ROOT_RUNNING_INSTANCE, params: { uuid } });
  }
  
  get firstLoadedInstance() {
    return this.runningWithData[0]
  }
  
  get runningWithData(): ({ instance: InstanceJson | undefined } & InstanceRunningData)[] {
    return this.runningInstances
      .map(e => ({
        ...e,
        instance: this.instances.find(i => i.uuid === e.uuid)
      }))
  }
}
</script>

<style lang="scss" scoped>
.instance-popout {
  width: fit-content;
  background: rgba(black, .5);
  backdrop-filter: blur(5px);
  left: 100%;
  
  .name {
    display: block;
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
    width: 200px;
  }
}
</style>
