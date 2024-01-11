<template>
  <div class="onboarding">
    <img src="../../assets/images/ftb-logo.svg" width="80" />
    <p>Onboarding</p>
    <p>Welcome to the FTB App</p>
    
    <p v-if="installing">Installing the app</p>
    
    <ui-button v-if="!installing" @click="$router.push('/')">Go home</ui-button>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import {Getter} from 'vuex-class';
import {ns} from '@/core/state/appState';
import {RouterNames} from '@/router';
import platform from '@/utils/interface/electron-overwolf';
import UiButton from '@/components/core/ui/UiButton.vue';

/**
 * - Say hi
 * - Tell them about the app
 *   - Allow skip
 * - Install the app
 * - Once the app is ready, offer the chance to sign in but allow skip
 * - Show them around a quick tour
 * - Offer to show them the changelog
 * - Setup Instance defaults?
 */
@Component({
  components: {UiButton}
})
export default class Onboarding extends Vue {
  @Getter("ready", ns("v2/app")) appReady!: boolean
  @Getter("installed", ns("v2/app")) appInstalled!: boolean
  
  settingUp = false;
  installing = true;
  stage = "Unknown"
  
  async mounted() {
    if (this.appReady && this.appInstalled) {
      await this.$router.push({ name: RouterNames.HOME })
    }
  }
  
  async run() {
    // It's time to install the app
    await platform.get.app.installApp(stage => this.stage = stage, (data) => {
      console.log(data);
    })
    
    this.installing = false;
    // Ask the app to start the backend now that it's installed
  }
}
</script>

<style lang="scss" scoped>
.onboarding {
  padding: 2rem;
}
</style>