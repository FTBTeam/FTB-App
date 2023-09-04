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
  
  mounted() {
    this.fetchModpack(this.packId);
  }

  install() {
    const apiPack = this.apiModpack!;
    instanceInstallController.requestInstall({
      id: apiPack.id,
      version: apiPack.versions[0].id,
      name: "hello" + Math.random() * 100,
      versionName: "hello2",
      logo: "",
    })
  }

  /**
   * This is mostly a visual thing so people don't install a modpack multiple times
   * because they think it's not installing.
   */
  get isInstalling() {
    return this.currentInstall?.request.id === this.apiModpack?.id;
  }
}
</script>

<style lang="scss" scoped>

</style>