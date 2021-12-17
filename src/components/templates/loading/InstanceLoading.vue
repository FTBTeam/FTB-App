<template>
  <div class="authentication global__fullscreen-modal" v-if="isInstanceLoading">
    <div class="body-contents text-center">
      <div class="main">
        <h3 class="text-2xl mb-4"><b>Your instance is loading</b></h3>
        <p class="mb-2">
          {{ currentStep.stepDesc }}
          <!-- {{currentStep.step}}/{{currentStep.totalSteps}} -->
        </p>
        <p class="mb-2" v-if="currentStep.stepProgressHuman != undefined">
          {{ currentStep.stepProgressHuman }}
        </p>
        <ProgressBar class="my-10" :progress="currentStep.stepProgress" />
        <ftb-button
          class="px-6 py-4 border-solid border-2 border-red-500 hover:border-danger hover:bg-danger"
          @click="$emit('close')"
          >Cancel</ftb-button
        >
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import Component from 'vue-class-component';
import Vue from 'vue';
import YggdrasilAuthForm from '@/components/templates/authentication/YggdrasilAuthForm.vue';
import Loading from '@/components/atoms/Loading.vue';
import ProgressBar from '@/components/atoms/ProgressBar.vue';
import { Action, Getter } from 'vuex-class';
import MicrosoftAuth from '@/components/templates/authentication/MicrosoftAuth.vue';

@Component({
  components: { MicrosoftAuth, YggdrasilAuthForm, Loading, ProgressBar },
})
export default class InstanceLoading extends Vue {
  @Action('sendMessage') public sendMessage: any;
  @Action('registerLaunchProgressCallback') public registerLaunchProgressCallback: any;
  @Getter('getInstanceLoading', { namespace: 'core' }) public isInstanceLoading: any;

  currentStep = {
    stepDesc: '',
    step: 0,
    totalSteps: 0,
    stepProgress: 0,
    stepProgressHuman: '',
  };

  public mounted() {
    this.registerLaunchProgressCallback((data: any) => {
      this.currentStep.stepDesc = data.stepDesc;
      this.currentStep.step = data.step;
      this.currentStep.totalSteps = data.totalSteps;
      this.currentStep.stepProgress = data.stepProgress;
      this.currentStep.stepProgressHuman = data.stepProgressHuman;
    });
  }
}
</script>

<style lang="scss" scoped></style>
