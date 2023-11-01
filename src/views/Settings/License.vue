<template>
  <div class="flex flex-col ">
    <router-link :to="{ name: 'app-info' }">
      <ui-button icon="arrow-left" type="success" :wider="true">Back</ui-button>
    </router-link>
    <div class="flex flex-col mt-4">
      <h1 class="text-xl mb-4">NodeJS Package Licenses</h1>
      <div class="rounded packages">
        <div class="flex flex-row mb-2" v-for="(license, index) in licenses" :key="index">
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
        The information on this page was valid as of {{ config.dateCompiled | dayjs }}
      </small>
    </div>

    <h1 class="text-xl mb-4">Java Dependency Licenses</h1>
    <div class="rounded packages">
      <div class="flex flex-row mb-2" v-for="(license, index) in config.javaLicenses" :key="index">
        <a class="inline-block" target="_blank" :href="license.repository">{{ index }}</a>
        <div class="tag bg-blue-400 inline-block text-gray-900 font-bold px-2 py-1 ml-auto rounded text-xs">
          {{ license.license }}
        </div>
      </div>
    </div>
    <div class="disclaimer mt-2 mb-6">
      <small class="text-muted">
        <font-awesome-icon icon="info" class="mr-4" />
        The information on this page was valid as of {{ config.dateCompiled | dayjs }}
      </small>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';

import Licenses from '../../../licenses.json';
import platform from '@/utils/interface/electron-overwolf';
import UiButton from '@/components/core/ui/UiButton.vue';

@Component({
  components: {UiButton}
})
export default class LicensePage extends Vue {
  private licenses = Licenses;
  private config = {
    dateCompiled: platform.get.config.dateCompiled,
    javaLicenses: platform.get.config.javaLicenses,
  };
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
