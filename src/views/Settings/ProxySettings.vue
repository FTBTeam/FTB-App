<template>
  <div class="proxy-settings">
    <h2 class="text-xl mb-2 font-bold flex items-center">
      Proxy Settings <span class="ml-4 text-sm px-2 py-0-5 bg-orange-500 rounded">Beta</span>
    </h2>
    <p class="text-muted text-sm mb-4">You can configure the app to use a Proxy Server to route all of the internal network requests though. <em>Please note that this is still in beta and some requests might not be put through the proxy.</em></em></p>
    
    <div class='switcher'>
      <input id='switcher-none' type='radio' name='type' value='none' v-model='proxyType' />
      <label for='switcher-none' class='item'>
        None
      </label>
      <input id='switcher-http' type='radio' name='type' value='http' v-model='proxyType' />
      <label for='switcher-http' class='item'>
        HTTP
      </label>
      <input id='switcher-sock5' type='radio' name='type' value='sock5' v-model='proxyType' />
      <label for='switcher-sock5' class='item'>
        SOCK5
      </label>
    </div>
    
    <div class="flex mb-4">
      <ftb-input
        :disabled='proxyType === "none"'
        class="mr-4 flex-1"
        label="Proxy Host*"
        placeholder="localhost"
        :value="proxyHost"
        v-model="proxyHost"
      />

      <ftb-input
        :disabled='proxyType === "none"'
        label="Proxy Port*"
        placeholder="9040"
        type="number"
        :value="proxyPort"
        v-model="proxyPort"
      />
    </div>

    <ftb-input
      :disabled='proxyType === "none"'
      class="mb-4"
      label="Proxy Username"
      placeholder="Username"
      :value="proxyUser"
      v-model="proxyUser"
    />

    <ftb-input
      :disabled='proxyType === "none"'
      label="Proxy Password"
      placeholder="Password"
      :value="proxyPass"
      v-model="proxyPass"
      type="password"
    />
    
    <p class='text-muted text-xs italic'>Please be aware that proxy passwords are stored in plain text in the apps settings file. You have been warned!</p>

    <div class='action flex justify-end'>
      <ftb-button :disabled='proxyType === "none" || isInvalid()' color="primary" @click='save' class="inline-block py-2 px-10 mt-8">Test & Save</ftb-button>
    </div>
  </div>
</template>

<script lang="ts">
import { Component, Vue, Watch } from 'vue-property-decorator';
import FTBToggle from '@/components/atoms/input/FTBToggle.vue';
import { Action, State } from 'vuex-class';
import { SettingsState } from '@/modules/settings/types';

@Component({
  components: {
    'ftb-toggle': FTBToggle,
  },
})
export default class MTIntegration extends Vue {
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @State('settings') private settings!: SettingsState;
  @Action('loadSettings', { namespace: 'settings' }) public loadSettings: any;
  @Action('showAlert') public showAlert: any;
  
  proxyType: string | null = "none";
  proxyHost: string | null = "";
  proxyPort: null | number = null;
  proxyUser: string | null = "";
  proxyPass: string | null = "";

  async created() {
    await this.loadSettings();
    
    const {proxyUser, proxyPort, proxyHost, proxyPassword, proxyType} = this.settings.settings;

    this.proxyType = proxyType ?? "none";
    this.proxyHost = proxyHost ?? null;
    this.proxyPort = proxyPort ?? null;
    this.proxyUser = proxyUser ?? null;
    this.proxyPass = proxyPassword ?? null;
  }
  
  @Watch('proxyType')
  onTypeChange(newValue: string | null) {
    if (newValue === "none") {
      this.proxyHost = "";
      this.proxyPort = null;
      this.proxyUser = "";
      this.proxyPass = "";
    }
  }

  isInvalid() {
    console.log(this.proxyHost, this.proxyPort)
    return this.proxyHost === "" || this.proxyPort === null || this.proxyPort < 1;
  }
  
  save() {
    if (this.isInvalid()) {
      this.showAlert({
        type: 'danger',
        title: 'Error',
        message: 'Missing Hostname or Port',
      });
      return;
    }

    const payload = {
      ...this.settings.settings,
      proxyUser: this.proxyUser,
      proxyPort: this.proxyPort,
      proxyHost: this.proxyHost,
      proxyPassword: this.proxyPass,
      proxyType: this.proxyType,
    };
    
    console.log(payload)

    this.saveSettings(payload)

    this.showAlert({
      type: 'primary',
      title: 'Saved!',
      message: 'Proxy Settings updated',
    });
  }
}
</script>

<style scoped lang='scss'>
.switcher {
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  background-color: rgba(white, .05);
  border-radius: 5px;
  overflow: hidden;
  
  label {
    flex: 1;
    display: block;
    text-align: center;
    cursor: pointer;
    padding: .5rem 0;
    transition: background-color .2s ease-in-out;
  }

  input {
    visibility: hidden;
    opacity: 0;
    width: 0;
    height: 0;
    
    &:checked + label {
      background-color: var(--color-primary-button)
    }
  }
}
</style>