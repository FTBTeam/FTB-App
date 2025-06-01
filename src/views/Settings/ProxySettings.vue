<script lang="ts" setup>
import {alertController} from '@/core/controllers/alertController';
import { useAppSettings } from '@/store/appSettingsStore.ts';
import { onMounted, ref, watch } from 'vue';
import { UiButton, Input, InputNumber, Message } from '@/components/ui';
import { faAsterisk, faCheck, faGlobe, faUser, faWarning } from '@fortawesome/free-solid-svg-icons';

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

    <Message type='warning' class='mt-6 mb-8' :icon="faWarning" header="Beta notice">We're currently classing this setting as <em>Beta</em> because there are some known requests that do not yet use this setting.</Message>

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
      <Input
        :icon="faGlobe"
        :disabled='proxyType === "none"'
        class="mr-4 flex-1"
        label="Proxy Host*"
        placeholder="localhost"
        :value="proxyHost"
        v-model="proxyHost"
        fill
      />

      <InputNumber
        :disabled="proxyType === 'none'"
        label="Proxy Port*"
        placeholder="9040"
        :min="0"
        v-model="proxyPort"
      />
    </div>

    <Input
      :icon="faUser"
      :disabled='proxyType === "none"'
      class="mb-4"
      label="Proxy Username"
      placeholder="Username"
      :value="proxyUser"
      v-model="proxyUser"
      fill
    />

    <Input
      :icon="faAsterisk"
      :disabled='proxyType === "none"'
      label="Proxy Password"
      placeholder="Password"
      :value="proxyPass"
      v-model="proxyPass"
      type="password"
      fill
    />
    
    <div class='action flex justify-end'>
      <UiButton :wider="true" :disabled='proxyType === "none" || isInvalid()' type='success' :icon='faCheck' @click='() => save()'>Test & Save</UiButton>
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
      background-color: var(--color-green-600)
    }
  }
}
</style>