<script setup lang="ts">
import { RouterView, useRouter } from 'vue-router';
import { useWsStore } from '@/store/wsStore.ts';
import TitleBar from './components/layout/TitleBar.vue';
import AdAside from './components/layout/AdAside.vue';
import Sidebar from '@/components/layout/sidebar/Sidebar.vue';
import { Loader } from '@/components/ui';
import Onboarding from '@/components/modals/Onboarding.vue';
import { DevToolsActions } from '@/components/layout';
import GlobalComponents from './components/layout/GlobalComponents.vue';
import { computed, onMounted, ref, watch } from 'vue';
import { alertController } from '@/core/controllers/alertController.ts';
import { useInstanceStore } from '@/store/instancesStore.ts';
import { useAds } from '@/composables/useAds.ts';
import appPlatform from '@platform';
import { constants } from '@/core/constants.ts';
import { useAccountsStore } from '@/store/accountsStore.ts';
import { useAppSettings } from '@/store/appSettingsStore.ts';
import { useAppStore } from '@/store/appStore.ts';

const appStore = useAppStore();
const wsStore = useWsStore();
const appSettingsStore = useAppSettings();
const accountStore = useAccountsStore();
const router = useRouter()
const instanceStore = useInstanceStore();
const ads = useAds()

const isMac = ref(false);
const requiredDataLoading = ref(false)
const showOnboarding = ref(false)

onMounted(() => {
  console.info("App started on ", constants.platform)

  startNetworkMonitor()
  appPlatform.utils.getOsType().then(e => isMac.value = e === "mac")

  // Remove the old one if needed (typically app restarts (soft))
  appStore.emitter.off("ws/message", refreshHandler)
  appStore.emitter.on("ws/message", refreshHandler)
})

watch(() => wsStore.ready, (value) => {
  if (value) {
    requiredDataLoading.value = true;
    initAppLoad()
      .then(() => checkForFirstRun().catch(console.error))
      .finally(() => requiredDataLoading.value = false)
  }
})

async function initAppLoad() {
  await appSettingsStore.loadSettings();
  
  await Promise.all([
    accountStore.loadProfiles(),
    instanceStore.loadInstances()
  ])
}

function refreshHandler(data: any) {
  if (data?.type === "refreshInstancesRequest") {
    instanceStore.loadInstances();
  }
}

async function checkForFirstRun() {
  if (!appPlatform.isElectron) {
    return;
  }

  if (await appPlatform.app.cpm.required() && await appPlatform.app.cpm.isFirstLaunch()) {
    showOnboarding.value = true;
  }
}

function startNetworkMonitor() {
  if (!navigator.onLine) {
    alertController.warning("It looks like you've lost your internet connection, the app may not work as expected");
  }

  window.addEventListener("offline", () => {
    alertController.warning("It looks like you've lost your internet connection, the app may not work as expected");
  });

  window.addEventListener("online", () => {
    alertController.success("You're back online, the app should be working as expected");
  });
}

const showSidebar = computed(() => !router.currentRoute.value.path.startsWith('/settings'))
</script>

<template>
  <div id="app" class="theme-dark" :class="{'macos': isMac}">
    <title-bar />
    <div class="app-container">
      <main class="main">
        <div v-if="!wsStore.ready">
          Loading...
        </div>
        
        <template v-else>
          <sidebar v-if="showSidebar" />
          <div class="app-content relative">
            <router-view v-if="!requiredDataLoading" />
            <loader v-else />
          </div>
          <ad-aside v-show="ads.adsEnabled" :hide-ads="showOnboarding" />
        </template>
      </main>
    </div>

    <onboarding v-if="wsStore.ready && showOnboarding" @accepted="showOnboarding = false" />
    <dev-tools-actions v-if="wsStore.ready" />
    <GlobalComponents />
  </div>
</template>

<style lang="scss" scoped>
.app-container {
  height: calc(100% - 2rem);
  position: relative;

  .macos & {
    // Title bar on macos is 1.8rem not 2rem
    height: calc(100% - 1.8rem);
  }

  .system-frame & {
    height: 100%;
  }

  &.centered {
    display: flex;
    align-items: center;
    justify-content: center;

    .pushed-content {
      margin-top: -5rem;
    }
  }
}

main.main {
  position: relative;
  z-index: 1;
  display: flex;
  height: 100%;
}

.app-content {
  flex: 1;
  min-height: 100%;
  overflow-y: auto;
}

</style>