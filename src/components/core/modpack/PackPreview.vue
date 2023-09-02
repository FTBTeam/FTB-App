<template>
  <div v-if="apiModpack" @click="install">
    {{ apiModpack.name }}
    {{currentInstall}}
  </div>
  <div v-else>
    Loading...
  </div>
</template>

<script lang="ts">
import {Prop, Component} from 'vue-property-decorator';
import PackCardCommon from '@/components/core/modpack/PackCardCommon.vue';
import {instanceInstallController} from '@/core/controllers/InstanceInstallController';
import {Getter} from 'vuex-class';
import {InstallState} from '@/core/state/instances/installState';
import {ns} from '@/core/state/appState';

@Component
export default class PackPreview extends PackCardCommon {
  @Prop() packId!: number;
  
  @Getter("currentInstall", ns("v2/install")) currentInstall!: InstallState | null;
  
  mounted() {
    this.fetchModpack(this.packId);
  }

  install() {
    instanceInstallController.requestInstall({
      id: 0,
      version: 0,
      name: "hello",
      versionName: "hello2",
      logo: "",
      updatingInstanceUuid: "hi"
    })
  }
}
</script>

<style lang="scss" scoped>

</style>