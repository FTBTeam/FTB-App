<template>
  <div class="px-6 py-4">
    <div class="flex flex-col md:w-full lg:w-9/12 xl:w-8/12 mx-auto">
      <ftb-button class="py-2 px-4  my-2 w-15" color="primary" css-class="text-center text-l" @click="goTo('/settings')"
        >Back</ftb-button
      >
      <div class="flex flex-col mb-2 mt-4">
        <div class="flex flex-row my-4 -mt-2">
          <div class="relative w-full">
            <div class="flex items-center bg-blue-500 text-white text-sm font-bold px-4 py-3 rounded" role="alert">
              <font-awesome-icon icon="info" class="mr-2" />
              <p>The information on this page was valid as of {{ config.dateCompiled | moment }}</p>
            </div>
          </div>
        </div>

        <h1 class="text-2xl">NodeJS Package Licenses</h1>
        <div class="bg-sidebar-item p-5 rounded my-4 packages">
          <div class="flex flex-col">
            <div class="flex flex-row mb-2" v-for="(license, index) in licenses" :key="index">
              <p class="inline-block">{{ index }}</p>
              <a class="ml-auto text-right underline cursor-pointer" :href="license.repository" target="_blank"
                >View project</a
              >
              <div class="tag bg-teal-400 inline-block text-gray-900 font-bold px-2 py-1 ml-4 rounded text-xs">
                {{ license.licenses }}
              </div>
            </div>
          </div>
        </div>
      </div>
      <h1 class="text-2xl">Java Dependency Licenses</h1>
      <div class="bg-sidebar-item p-5 rounded my-4 packages">
        <div class="flex flex-col my-2">
          <div class="flex flex-col my-2">
            <div class="flex flex-row mb-2" v-for="(license, index) in config.javaLicenses" :key="index">
              <p class="inline-block">{{ index }}</p>
              <a class="ml-auto text-right underline cursor-pointer" :href="license.repository" target="_blank"
                >View project</a
              >
              <div class="tag bg-teal-400 inline-block text-gray-900 font-bold px-2 py-1 ml-4 rounded text-xs">
                {{ license.licenses }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import FTBInput from '@/components/FTBInput.vue';
import FTBToggle from '@/components/FTBToggle.vue';
import FTBButton from '@/components/FTBButton.vue';
import FTBSlider from '@/components/FTBSlider.vue';
import Licenses from '../../licenses.json';
import platfrom from '@/utils/interface/electron-overwolf';

@Component({
  components: {
    'ftb-input': FTBInput,
    'ftb-toggle': FTBToggle,
    'ftb-slider': FTBSlider,
    'ftb-button': FTBButton,
  },
})
export default class LicensePage extends Vue {
  private licenses = Licenses;
  private config = {
    dateCompiled: platfrom.get.config.dateCompiled,
    javaLicenses: platfrom.get.config.javaLicenses,
  };

  public goTo(page: string): void {
    // We don't care about this error!
    this.$router.push(page).catch(err => {
      return;
    });
  }
}
</script>

<style lang="scss" scoped>
.packages {
  max-height: 400px;
  overflow-y: auto;
}
</style>
