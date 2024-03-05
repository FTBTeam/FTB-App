<template>
 <div class="privacy">
   <p class="block text-white-700 text-lg font-bold mb-4">Privacy</p>
   <p class="mb-6">The FTB App is run and owned by Feed The Beast Ltd. You can check out our <Link url="https://go.ftb.team/privacy-notice">Privacy Policy</Link> and <Link url="https://go.ftb.team/t-and-c">Terms of Use</Link> on our website.</p>

   <p class="block text-white-700 font-bold mb-4">About ads on our platform</p>
   <p class="mb-8">The ads on our App support the continued development of this application as well as directly support FTB in creating unique, immersive and high quality modpacks for the Modded Minecraft Community. Our ads are provided by <Link url="https://www.overwolf.com">Overwolf</Link> and you can visit their <Link url="https://go.ftb.team/ow-privacy">Privacy Policy</Link> and <Link url="https://go.ftb.team/ow-terms">Terms of Use</Link> on their website.</p>

   <template v-if="platform.isElectron()">
     <p class="block text-white-700 text-lg font-bold mb-4">Ads personalization & Data</p>
     <div>
       <p>View and manage how advertisers on select apps may use your data for ad personalization</p>
       <ui-button class="mt-6" type="primary" @click="platform.get.app.cpm.openWindow('purposes')">Manage ads</ui-button>
     </div>
   </template>
 </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import UiButton from '@/components/core/ui/UiButton.vue';
import platform from '@/utils/interface/electron-overwolf';
import Link from '@/components/core/ui/Link.vue';

@Component({
  components: {UiButton, Link}
})
export default class Privacy extends Vue {
  platform = platform;
}
</script>

<style lang="scss" scoped>

</style>