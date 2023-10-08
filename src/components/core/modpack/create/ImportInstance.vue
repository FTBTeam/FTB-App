<template>
  <modal :open="open" @closed="close" :external-contents="true" title="Import" sub-title="Import instances from other launchers">
    <modal-body>
      <div class="flex gap-2 flex-wrap">
        <ui-button v-for="(provider, index) in providers" :key="index" @click="userProvider = provider[0]">
          {{ provider[1] }}
        </ui-button>
      </div>
      
      {{ userProvider }}
      
      <Loader v-if="working" />
      
      <div class="instances" v-if="providerInstances.length > 0">
        <div class="instance" v-for="(instance, index) in providerInstances" :key="index">
          <div class="name">{{ instance.name }}</div>
          <div class="minecraft">{{instance.minecraftVersion}}</div>
          <div class="location">{{instance.dataLocation}}</div>
        </div>
      </div>
    </modal-body>
  </modal>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue, Watch} from 'vue-property-decorator';

import ModalBody from '@/components/atoms/modal/ModalBody.vue';
import Modal from '@/components/atoms/modal/Modal.vue';
import UiButton from '@/components/core/ui/UiButton.vue';
import Loader from '@/components/atoms/Loader.vue';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {Providers, SimpleInstanceInfo} from '@/core/@types/javaApi';
import {gobbleError} from '@/utils/helpers/asyncHelpers';

const providers: [Providers, string][] = [
  ['CURSEFORGE', "Curse Forge"],
  ['PRISM', "Prism Launcher"],
  ['MODRINTH', "Modrinth"],
  ['ATLAUNCHER', "AtLauncher"],
  ['GDLAUNCHER', "GD Launcher"],
  ['MULTIMC', "MultiMC"],
]

@Component({
  components: {Loader, UiButton, Modal, ModalBody}
})
export default class ImportInstance extends Vue {
  @Prop() open!: boolean;
  @Emit() close() {
    this.userProvider = providers[0][0];
    this.providerInstances = [];
    this.working = false;
  }

  providers = providers;
  userProvider: Providers = providers[0][0];
  working = false;
  
  providerInstances: SimpleInstanceInfo[] = [];
  
  mounted() {
    console.log("ImportInstance mounted");
    gobbleError(() => this.loadProviderInstances())
  }
  
  @Watch('userProvider')
  onProviderChanged() {
    console.log("Provider changed to", this.userProvider);
    gobbleError(() => this.loadProviderInstances())
  }
  
  async loadProviderInstances() {
    this.working = true;
    console.log("Loading instances from", this.userProvider);
    const result = await sendMessage("import.providerInstances", {
      provider: this.userProvider
    })
    
    console.log("Result", result);
    this.providerInstances = result.instances;
    this.working = false;
  }
}
</script>

<style lang="scss" scoped>

</style>