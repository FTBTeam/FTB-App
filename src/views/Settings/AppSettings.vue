<template>
  <div class="app-settings">
    <!-- <ftb-toggle label="Enable Analytics: " :value="settingsCopy.enableAnalytics" @change="enableAnalytics"
                    onColor="bg-primary"/> -->
    <ftb-toggle
      label="Use the beta channel"
      :value="localSettings.enablePreview"
      @change="enablePreview"
      onColor="bg-primary"
      class="mb-8"
      small="This allows you to opt-in to the beta channel of the FTB App, this version is typically less stable than the normal release channel."
    />
    <ftb-toggle
      v-if="!platform.isElectron()"
      label="Close Overwolf on Exit"
      :value="localSettings.exitOverwolf"
      @change="exitOverwolf"
      onColor="bg-primary"
      class="mb-8"
      small="If enabled, we'll automatically close the Overwolf app when you exit the FTB App, can be useful if you don't use Overwolf."
    />
    <!--      :disabled="auth.token === null ? 'true' : ''"-->
    <!--      :value="localSettings.enableChat"-->
    <ftb-toggle
      label="Enable MineTogether Chat"
      :value="false"
      @change="enableChat"
      onColor="bg-primary"
      :disabled="true"
      class="mb-8"
      small="Enabled you to use the MineTogether chat from within the app. Currently disabled..."
    />
  </div>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import { Settings, SettingsState } from '@/modules/settings/types';
import { Action, State } from 'vuex-class';
import platform from '@/utils/interface/electron-overwolf';
import { AuthState } from '@/modules/auth/types';

@Component({
  components: {
    'ftb-toggle': FTBToggle,
  },
})
export default class AppSettings extends Vue {
  @State('auth') private auth!: AuthState;
  @State('settings') public settingsState!: SettingsState;
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @Action('loadSettings', { namespace: 'settings' }) public loadSettings: any;

  platform = platform;
  localSettings: Settings = {} as Settings;

  async created() {
    await this.loadSettings();

    // Make a copy of the settings so we don't mutate the vuex state
    this.localSettings = { ...this.settingsState.settings };
  }

  public enablePreview(value: boolean): void {
    this.localSettings.enablePreview = value;
    this.saveSettings(this.localSettings);
  }

  public enableChat(value: boolean): void {
    if (this.auth.token !== null) {
      this.localSettings.enableChat = value;
      this.saveSettings(this.localSettings);
    }
  }

  public exitOverwolf(value: boolean): void {
    this.localSettings.exitOverwolf = value;
    this.saveSettings(this.localSettings);
    platform.get.actions.changeExitOverwolfSetting(value);
  }
}
</script>

<style scoped lang="scss"></style>
