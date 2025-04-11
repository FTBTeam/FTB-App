<script lang="ts" setup>
import {alertController} from '@/core/controllers/alertController';
import { useAppSettings } from '@/store/appSettingsStore.ts';
import { onMounted, ref, watch } from 'vue';
import { FTBInput, UiMessage, UiButton } from '@/components/ui';

const appSettingsStore = useAppSettings();

const proxyType = ref("none");
const proxyHost = ref("");
const proxyPort = ref(1);
const proxyUser = ref("");
const proxyPass = ref("");

onMounted(async () => {
  if (!appSettingsStore.rootSettings) {
    await appSettingsStore.loadIfNeeded();
  }
  
  if (!appSettingsStore.rootSettings?.proxy) {
    return;
  }

  const {username, port, host, password, type} = appSettingsStore.rootSettings?.proxy;
  
  proxyType.value = type ?? "none";
  proxyHost.value = host ?? "";
  proxyPort.value = port;
  proxyUser.value = username ?? "";
  proxyPass.value = password ?? "";
})

watch(proxyType, (newValue) => {
  if (newValue === "none") {
    proxyHost.value = "";
    proxyPort.value = -1;
    proxyUser.value = "";
    proxyPass.value = "";
    save(true)
  }
})

function isInvalid() {
  return proxyHost.value === "" || proxyPort.value === -1 || proxyPort.value < 1;
}

function save(remove = false) {
  if (isInvalid() && !remove) {
    alertController.error("Missing Hostname or Port")
    return;
  }

  appSettingsStore.saveSettings({
    ...appSettingsStore.rootSettings!,
    proxy: {
      type: proxyType.value,
      host: proxyHost.value,
      port: proxyPort.value,
      username: proxyUser.value,
      password: proxyPass.value
    }
  })

  alertController.success("Proxy Settings updated")
}
</script>

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
      <FTBInput
        :disabled='proxyType === "none"'
        class="mr-4 flex-1"
        label="Proxy Host*"
        placeholder="localhost"
        :value="proxyHost"
        v-model="proxyHost"
      />

      <FTBInput
        :disabled='proxyType === "none"'
        label="Proxy Port*"
        placeholder="9040"
        type="number"
        :value="proxyPort"
        v-model="proxyPort"
      />
    </div>

    <FTBInput
      :disabled='proxyType === "none"'
      class="mb-4"
      label="Proxy Username"
      placeholder="Username"
      :value="proxyUser"
      v-model="proxyUser"
    />

    <FTBInput
      :disabled='proxyType === "none"'
      label="Proxy Password"
      placeholder="Password"
      :value="proxyPass"
      v-model="proxyPass"
      type="password"
    />
    
    <p class='text-muted text-xs italic'>Please be aware that proxy passwords are stored in plain text in the apps settings file. You have been warned!</p>
    <UiMessage type='warning' class='mt-4 mb-8'>We're currently classing this setting as <em>Beta</em> because there are some known requests that do not yet use this setting.</UiMessage>

    <div class='action flex justify-end'>
      <ui-button :wider="true" :disabled='proxyType === "none" || isInvalid()' type='success' icon='check' @click='() => save()'>Test & Save</ui-button>
    </div>
  </div>
</template>

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