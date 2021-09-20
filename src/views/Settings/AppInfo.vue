<template>
  <div class="app-info">
    <div class="app-info-section mb-8">
      <div class="items flex">
        <div class="title-value mr-10">
          <div class="title text-muted mb-1">UI Version</div>
          <div class="value">
            <span class="allow-selection">{{ webVersion }}</span>
            <div class="copy-me inline-block" aria-label="Click to copy" data-balloon-pos="up">
              <font-awesome-icon
                @click="copyToClipboard(webVersion)"
                class="ml-2 cursor-pointer"
                :icon="['fas', 'copy']"
                size="1x"
              />
            </div>
            <a class="cursor-pointer hover:underline" href="https://github.com/FTBTeam/FTB-App-Feedback" target="_blank"
              ><font-awesome-icon class="ml-2 cursor-pointer" :icon="['fab', 'github']" size="1x"
            /></a>
          </div>
        </div>

        <div class="title-value">
          <div class="title text-muted mb-1">App Version</div>
          <div class="value">
            <span class="allow-selection">{{ appVersion }}</span>
            <div class="copy-me inline-block" aria-label="Click to copy" data-balloon-pos="up">
              <font-awesome-icon
                @click="copyToClipboard(appVersion)"
                class="ml-2 cursor-pointer"
                :icon="['fas', 'copy']"
                size="1x"
              />
            </div>
            <a
              class="cursor-pointer hover:underline"
              href="https://github.com/CreeperHost/modpacklauncher"
              target="_blank"
              ><font-awesome-icon class="ml-2 cursor-pointer" :icon="['fab', 'github']" size="1x"
            /></a>
          </div>
        </div>
      </div>

      <router-link :to="{ name: 'license' }" class="hover:underline cursor-pointer text-muted text-sm mt-4 block"
        >Software License Information</router-link
      >
    </div>
    <div class="section logs mb-8 flex items-center">
      <div class="desc flex-1">
        <p class="font-bold mb-1">Create app logs</p>
        <p class="text-muted mb-2 pr-10">
          If you're having an issue with the app, you can use this to upload the latest logs from the App. You can the
          provide these logs to our App team to investigate.
        </p>
      </div>
      <ftb-button
        class="py-2 px-4 my-2 w-2/7 inline-block whitespace-no-wrap"
        color="info"
        css-class="text-center text-l"
        @click="uploadLogData"
        >Upload App Logs</ftb-button
      >
    </div>

    <div class="section cache mb-8 flex items-center">
      <div class="desc flex-1">
        <p class="font-bold mb-1">Manage cache</p>
        <p class="text-muted pr-10">
          Having an issue with the App loading content? Maybe you want to check if you've got the latest data? Refresh
          the cache, it'll make sure you're running the latest version of all available data.
        </p>
      </div>
      <ftb-button
        class="py-2 px-4 my-2 w-2/7 inline-block"
        color="info"
        css-class="text-center text-l"
        @click="refreshCachePlz"
        >Refresh Cache</ftb-button
      >
    </div>

    <ftb-toggle
      label="Verbose"
      :value="localSettings.verbose"
      @change="enableVerbose"
      onColor="bg-primary"
      small="Enabled very detailed logging for the FTB App... You likely don't need this but it could be helpful?"
    />
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import { Action, State } from 'vuex-class';
import { Settings, SettingsState } from '@/modules/settings/types';
import platform from '@/utils/interface/electron-overwolf';
import FTBToggle from '@/components/FTBToggle.vue';

@Component({
  components: {
    'ftb-toggle': FTBToggle,
  },
})
export default class AppInfo extends Vue {
  @State('settings') public settingsState!: SettingsState;
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @Action('loadSettings', { namespace: 'settings' }) public loadSettings: any;
  @Action('showAlert') public showAlert: any;
  @Action('sendMessage') public sendMessage: any;
  @Action('refreshCache', { namespace: 'modpacks' }) public refreshCache!: any;

  platform = platform;
  localSettings: Settings = {} as Settings;

  private webVersion: string = platform.get.config.webVersion;
  private appVersion: string = platform.get.config.appVersion;

  async created() {
    await this.loadSettings();

    // Make a copy of the settings so we don't mutate the vuex state
    this.localSettings = { ...this.settingsState.settings };
  }

  public async uploadLogData(): Promise<void> {
    this.sendMessage({
      payload: { type: 'uploadLogs', uiVersion: this.webVersion },
      callback: async (data: any) => {
        if (!data.error) {
          const url = `https://pste.ch/${data.code}`;
          platform.get.cb.copy(url);
          this.showAlert({
            title: 'Uploaded!',
            message: 'The URL has been copied to your clipboard',
            type: 'primary',
          });
        }
      },
    });
  }

  public refreshCachePlz() {
    this.refreshCache();
    this.showAlert({
      title: 'Cache refreshed!',
      message: 'Your cache has been flushed and reset',
      type: 'info',
    });
  }

  public enableVerbose(value: boolean): void {
    this.localSettings.verbose = value;
    this.saveSettings(this.localSettings);
  }
}
</script>

<style scoped lang="scss">
.app-info-section {
  background-color: var(--color-background);
  padding: 1rem;
  border-radius: 5px;
}

.allow-selection {
  user-select: text;
}
</style>
