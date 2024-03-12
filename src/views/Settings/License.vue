<template>
  <div class="flex flex-col ">
    <router-link :to="{ name: 'app-settings' }">
      <ui-button icon="arrow-left" type="success" :wider="true">Back</ui-button>
    </router-link>
    
    <div class="flex flex-col mt-4" v-if="licenseData && licenseData.javascript">
      <h1 class="text-xl mb-4">NPM Package Licenses</h1>
      <div class="rounded packages">
        <div class="flex flex-row mb-2" v-for="(license, index) in licenseData.javascript" :key="index">
          <a class="inline-block" :href="license.repository" target="_blank">{{ index }}</a>
          <div class="tag bg-blue-400 inline-block text-gray-900 font-bold px-2 py-1 ml-auto rounded text-xs">
            {{ license.licenses }}
          </div>
        </div>
      </div>
    </div>
    <div class="disclaimer mt-2 mb-6">
      <small class="text-muted">
        <font-awesome-icon icon="info" class="mr-4" />
        The information on this page was valid as of {{ configData.dateCompiled | dayjs }}
      </small>
    </div>

    <div class="flex flex-col mt-4" v-if="licenseData && licenseData.java">
      <h1 class="text-xl mb-4">Java Dependency Licenses</h1>
      <div class="rounded packages">
        <div class="flex flex-row mb-2" v-for="(license, index) in licenseData.java.dependencies" :key="index">
          <div class="inline-block">{{ license.name }}</div>
          <div class="tag bg-blue-400 inline-block text-gray-900 font-bold px-2 py-1 ml-auto rounded text-xs">
            <div class="licensesHolder flex gap-2">
              <template v-for="(licenseItem, index) in license.licenses">
                <a :key="`li-${index}`" class="inline-block" target="_blank" v-if="licenseItem.url" :href="licenseItem.url">{{licenseItem.name}}</a>
                <span :key="`li-${index}`" class="inline-block" v-else>{{licenseItem.name}}</span>
              </template>
            </div>
          </div>
        </div>
      </div>
      <div class="disclaimer mt-2 mb-6">
        <small class="text-muted">
          <font-awesome-icon icon="info" class="mr-4" />
          The information on this page was valid as of {{ configData.dateCompiled | dayjs }}
        </small>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';

import platform from '@/utils/interface/electron-overwolf';
import UiButton from '@/components/core/ui/UiButton.vue';

type LicenseResponse = Awaited<ReturnType<typeof platform.get.app.getLicenses>>;

@Component({
  components: {UiButton}
})
export default class LicensePage extends Vue {
  licenseData: LicenseResponse | null = null;
  
  async mounted() {
    this.licenseData = platform.get.app.getLicenses();
  }
  
  get configData() {
    return platform.get.config
  }
}
</script>

<style lang="scss" scoped>
.packages {
  max-height: 400px;
  overflow-y: auto;
  padding: 1rem;
  background-color: var(--color-background);
}
</style>
