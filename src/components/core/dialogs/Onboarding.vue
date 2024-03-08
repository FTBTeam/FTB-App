<template>
 <div class="onboarding">
    <modal :permanent="true" :open="true" title="Privacy">
      <p>
        <b>FTB App</b> may display in-app ads to help provide you with a free high-quality app.
        In order to deliver ads that are relevant for you, <b>FTB App</b> and trusted <a class="underline cursor-pointer hover:text-blue-500" @click.prevent="showManageWindow('vendors')">ad vendors</a>
        store and/or access information on your computer, and process personal data such as IP address and cookies.
        Click on the “Manage” button to control your consents,
        or to object to the processing of your data when done on the basis of legitimate interest.
        You can change your preferences at any time via the settings screen.
        <br/><br/>
        Purposes we use: Store and/or access information on a device,
        personalised ads and content, ad and content measurement,
        audience insights and product development.
      </p>
      
      <template #footer>
        <div class="flex gap-4 justify-end">
          <ui-button @click="showManageWindow()">Manage</ui-button>
          <ui-button type="success" @click="accept()">Next</ui-button>
        </div>
      </template>
    </modal>
 </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import UiButton from '@/components/core/ui/UiButton.vue';
import platform from '@/utils/interface/electron-overwolf';

@Component({
  components: {UiButton}
})
export default class Onboarding extends Vue {
  platform = platform;
  
  showManageWindow(tab: 'purposes' | 'features' | 'vendors' = 'purposes') {
    this.platform.get.app.cpm.openWindow(tab);
  }

  accept() {
    this.platform.get.app.cpm.setFirstLaunched();
    this.$emit('accepted');
  }
}
</script>

<style lang="scss" scoped>

</style>