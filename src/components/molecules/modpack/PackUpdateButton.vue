<template>
  <div class="update" v-if="instance && localInstance && latestVersion">
    <ftb-button color="warning" class="update-btn px-4 py-1" @click="showConfirm = true">
      <span class="hidden sm:inline-block">Update available</span>
      <font-awesome-icon icon="cloud-download-alt" class="sm:ml-2" />
    </ftb-button>

    <update-confirm-modal v-if="localInstance && instance" :local-instance="localInstance" :latest-version="latestVersion" :open="showConfirm" @close="showConfirm = false" />
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import { Prop, Watch } from 'vue-property-decorator';
import {ModPack, Versions} from '@/modules/modpacks/types';
import Component from 'vue-class-component';
import Loader from '@/components/atoms/Loader.vue';
import {parseMarkdown} from '@/utils';
import {InstanceJson, SugaredInstanceJson} from '@/core/@types/javaApi';
import {packUpdateAvailable} from '@/utils/helpers/packHelpers';
import UiButton from '@/components/core/ui/UiButton.vue';
import UpdateConfirmModal from '@/components/core/modpack/UpdateConfirmModal.vue';

@Component({
  methods: {parseMarkdown},
  components: {UpdateConfirmModal, UiButton, Loader },
})
export default class PackUpdateButton extends Vue {
  @Prop() instance!: ModPack;
  @Prop() localInstance!: InstanceJson | SugaredInstanceJson;

  showConfirm = false;
  latestVersion = null as Versions | null;
  
  @Watch('instance')
  @Watch('localInstance')
  onInstanceChange() {
    if (!this.instance || !this.localInstance) {
      return;
    }

    this.latestVersion = packUpdateAvailable(this.localInstance, this.instance) ?? null;
  }
}
</script>

<style lang="scss" scoped>
.update-btn {
  position: relative;

  box-shadow: 0 0 0 0 rgba(#ff801e, 1);
  animation: pulse 1.8s ease-in-out infinite;

  @keyframes pulse {
    0% {
      box-shadow: 0 0 0 0 rgba(#ff801e, 0.7);
    }

    70% {
      box-shadow: 0 0 0 10px rgba(#ff801e, 0);
    }

    100% {
      box-shadow: 0 0 0 0 rgba(#ff801e, 0);
    }
  }
}

.changes {
  border-top: 2px solid rgb(white, 0.05);
  padding-top: 1rem;
}
</style>
