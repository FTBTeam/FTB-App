<template>
  <div class="proxy-settings">
    <h2 class="text-xl mb-2 font-bold flex items-center">
      Proxy Settings <span class="ml-4 text-sm px-2 py-0-5 bg-orange-500 rounded">Beta</span>
    </h2>
    <p class="text-muted text-sm mb-4">You can configure the app to use a Proxy Server to route all of the internal network requests though. <em>Please note that this is still in beta and some requests might not be put through the proxy.</em></p>
    
    <div class='switcher'>
      <input id='switcher-none' type='radio' name='type' value='none' v-model='proxyType' />
      <label for='switcher-none' class='item'>
        None
      </label>
      <input id='switcher-http' type='radio' name='type' value='http' v-model='proxyType' />
      <label for='switcher-http' class='item'>
        HTTP
      </label>
      <input id='switcher-socks5' type='radio' name='type' value='socks5' v-model='proxyType' />
      <label for='switcher-socks5' class='item'>
        SOCKS5
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
    <message type='warning' class='mt-4 mb-8'>We're currently classing this setting as <em>Beta</em> because there are some known requests that do not yet use this setting.</message>

    <div class='action flex justify-end'>
      <ui-button :wider="true" :disabled='proxyType === "none" || isInvalid()' type='success' icon='check' @click='() => save()'>Test & Save</ui-button>
    </div>
  </div>
</template>

<script lang="ts">
import {Component, Vue, Watch} from 'vue-property-decorator';
import {Action, State} from 'vuex-class';
import {SettingsState} from '@/modules/settings/types';
import {alertController} from '@/core/controllers/alertController';
import UiButton from '@/components/ui/UiButton.vue';

@Component({
  components: {UiButton}
})
export default class ProxySettings extends Vue {
  @Action('saveSettings', { namespace: 'settings' }) public saveSettings: any;
  @State('settings') private settings!: SettingsState;
  @Action('loadSettings', { namespace: 'settings' }) public loadSettings: any;
  
  proxyType: string = "none";
  proxyHost: string = "";
  proxyPort = -1;
  proxyUser: string = "";
  proxyPass: string = "";

  async created() {
    await this.loadSettings();
    
    const {username, port, host, password, type} = this.settings.settings.proxy

    this.proxyType = type ?? "none";
    this.proxyHost = host ?? "";
    this.proxyPort = port;
    this.proxyUser = username ?? "";
    this.proxyPass = password ?? "";
  }
  
  @Watch('proxyType')
  onTypeChange(newValue: string | null) {
    if (newValue === "none") {
      this.proxyHost = "";
      this.proxyPort = -1;
      this.proxyUser = "";
      this.proxyPass = "";
      this.save(true)
    }
  }

  isInvalid() {
    return this.proxyHost === "" || this.proxyPort === -1 || this.proxyPort < 1;
  }
  
  save(remove = false) {
    if (this.isInvalid() && !remove) {
      alertController.error("Missing Hostname or Port")
      return;
    }
    
    this.saveSettings({
      ...this.settings.settings,
      proxy: {
        type: this.proxyType,
        host: this.proxyHost,
        port: this.proxyPort,
        username: this.proxyUser,
        password: this.proxyPass
      }
    })

    alertController.success("Proxy Settings updated")
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