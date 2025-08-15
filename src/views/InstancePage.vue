<script lang="ts" setup>
import ModpackVersions from '@/components/groups/instance/ModpackVersions.vue';
import PackMetaHeading from '@/components/groups/modpack/PackMetaHeading.vue';
import PackTitleHeader from '@/components/groups/modpack/PackTitleHeader.vue';
import PackBody from '@/components/groups/modpack/PackBody.vue';
import {RouterNames} from '@/router';
import VersionsBorkedModal from '@/components/modals/VersionsBorkedModal.vue';
import {SugaredInstanceJson} from '@/core/types/javaApi';
import {resolveArtwork, typeIdToProvider} from '@/utils/helpers/packHelpers';
import {alertController} from '@/core/controllers/alertController';
import {dialogsController} from '@/core/controllers/dialogsController';
import {modpackApi} from '@/core/pack-api/modpackApi';
import { UiButton, ClosablePanel, Modal, Message, Input } from '@/components/ui';
import {createLogger} from '@/core/logger';
import {InstanceController} from '@/core/controllers/InstanceController';
import { useRouter } from 'vue-router';
import { computed, onMounted, ref } from 'vue';
import { useModpackStore } from '@/store/modpackStore.ts';
import { useInstanceStore } from '@/store/instancesStore.ts';
import { useAccountsStore } from '@/store/accountsStore.ts';
import { ModPack, Versions } from '@/core/types/appTypes.ts';
import { faPlay } from '@fortawesome/free-solid-svg-icons';
import { useAppStore } from '@/store/appStore.ts';

const router = useRouter()

const modpackStore = useModpackStore();
const instancesStore = useInstanceStore();
const accountsStore = useAccountsStore()

const logger = createLogger("InstancePage.vue");

const packLoading = ref(false);
const tabs = ModpackPageTabs;
const activeTab = ref<ModpackPageTabs>(ModpackPageTabs.OVERVIEW);

const apiPack = ref<ModPack | null>(null);

const showVersions = ref(false);
const offlineMessageOpen = ref(false);
const offlineUserName = ref('');
const offlineAllowed = ref(false);

const borkedVersionNotification = ref<string | null>(null);
const borkedVersionDowngradeId = ref<number | null>(null);
const borkedVersionIsDowngrade = ref(false);

const packUuid = computed(() => router.currentRoute.value.params.uuid);
const instance = computed<SugaredInstanceJson | null>(() => {
  if (!packUuid.value) {
    return null;
  }
  
  logger.debug(`Getting instance ${packUuid.value}`)
  return instancesStore.instances.find(e => e.uuid === packUuid.value) ?? null;
})

onMounted(async () => {
  if (instance.value == null) {
    logger.error(`Instance not found ${packUuid.value}`)
    await router.push(RouterNames.ROOT_LIBRARY);
    return;
  }

  const quickNav = router.currentRoute.value.query.quickNav;
  if (quickNav) {
    logger.debug(`Quick nav detected, navigating to ${quickNav}`)
    // Ensure the tab is valid
    if (Object.values(ModpackPageTabs).includes(quickNav as ModpackPageTabs)) {
      activeTab.value = quickNav as ModpackPageTabs;
    } else {
      logger.error("Invalid quick nav tab")
    }
  }

  // TODO: (M#01) Allow to work without this.
  if (instance.value.id !== -1) {
    apiPack.value = await modpackStore.getModpack(instance.value.id, typeIdToProvider(instance.value.packType));

    if (!apiPack.value) {
      activeTab.value = ModpackPageTabs.MODS;
    }
  }

  logger.debug("Loading backups")

  // Throwaway error, don't block
  checkForBorkedVersion().catch(e => logger.error(e))

  if (accountsStore.mcActiveProfile) {
    logger.debug("Active profile found, allowing offline")
    offlineAllowed.value = true;
  }

  if (router.currentRoute.value.query.presentOffline) {
    logger.debug("Present offline query detected")
    offlineMessageOpen.value = true;
  }

  offlineUserName.value = accountsStore.mcActiveProfile?.username ?? "MinecraftPlayer";  
})

/**
 * Tries to fetch the current version data and warn the user if the version should be downgraded. Will only request
 * if the held version is set to `archived`
 */
async function checkForBorkedVersion() {
  logger.debug("Checking for borked version")
  if (!instance.value?.versionId || !apiPack.value) {
    return;
  }

  if (instance.value.versionId === -1) {
    return;
  }

  const currentVersion = apiPack.value.versions.find((e) => e.id === instance.value?.versionId);
  if (!currentVersion || currentVersion.type.toLowerCase() !== 'archived') {
    return;
  }

  const apiData = await modpackApi.modpacks.getModpackVersion(instance.value.id, instance.value.versionId, typeIdToProvider(instance.value.packType))
  if (!apiData) {
    return;
  }

  if (apiData.notification) {
    borkedVersionNotification.value = apiData.notification;
  }

  // Find a downgrade / upgrade version
  const nextAvailableVersion = apiPack.value.versions.find((e) => e.type !== 'archived');
  if (nextAvailableVersion) {
    logger.debug("Found next available version")
    borkedVersionDowngradeId.value = nextAvailableVersion.id;
    if (nextAvailableVersion.id < instance.value.versionId) {
      logger.debug("Borked version is a downgrade")
      borkedVersionIsDowngrade.value = true;
    }
  }
}

function closeBorked() {
  borkedVersionNotification.value = null;
  borkedVersionDowngradeId.value = null;
  borkedVersionIsDowngrade.value = false;
}

function goBack() {
  if (!hidePackDetails.value) {
    router.push({ name: RouterNames.ROOT_LIBRARY });
  } else {
    activeTab.value = apiPack.value ? ModpackPageTabs.OVERVIEW : ModpackPageTabs.MODS;
  }
}

async function launchModPack() {
  if (instance.value === null) {
    return;
  }

  if (instance.value.memory < instance.value.minMemory) {
    logger.debug("Showing low memory warning")
    const result = await dialogsController.createConfirmationDialog("Low memory",
      `You are trying to launch the modpack with memory settings that are below the` +
      `minimum required.This may cause the modpack to not start or crash frequently.\n\nWe recommend that you` +
      `increase the assigned memory to at least **${instance.value?.minMemory}MB**\n\nYou can change the memory by going to the settings tab of the modpack and adjusting the memory slider`
    )

    if (!result) {
      return
    }
  }

  launch();
}

function launch() {
  if (instance.value === null) {
    // HOW
    return;
  }
  
  logger.debug("Launching instance from instance page")
  InstanceController.from(instance.value!).play();
}

function playOffline() {
  logger.debug("Launching instance in offline mode")
  InstanceController.from(instance.value!).playOffline(offlineUserName.value);
}

function update(version: Versions | null = null): void {
  const targetVersion = version ?? apiPack.value?.versions.sort((a, b) => b.id - a.id)[0];
  if (!targetVersion || !instance.value) {
    logger.error("Failed to find pack for update")
    return;
  }

  const appStore = useAppStore();
  logger.debug(`Requesting update to ${targetVersion.id} ${targetVersion.type}`)
  appStore.controllers.install.requestUpdate(instance.value, targetVersion, typeIdToProvider(instance.value.packType))
}

function updateOrDowngrade(versionId: number) {
  const pack = apiPack.value?.versions.find((e) => e.id === versionId);
  if (!pack) {
    logger.error("Failed to find pack for update / downgrade")
    alertController.error('The selected recovery pack version id was not available...')
    return;
  }

  // update
  update(pack);
  closeBorked();
}

function closeOfflineModel() {
  offlineMessageOpen.value = false;
}

const packSplashArt = computed(() => resolveArtwork(apiPack.value, 'splash'));
const hidePackDetails = computed(() => activeTab.value === ModpackPageTabs.SETTINGS);
const versionType = apiPack.value?.versions?.find((e) => e.id === instance.value?.versionId)?.type.toLowerCase() ?? 'release';
</script>

<script lang="ts">
export enum ModpackPageTabs {
  OVERVIEW = "overview",
  MODS = "mods",
  SETTINGS = "settings",
  WORLDS = "worlds",
}
</script>

<template>
  <div class="pack-page">
    <div class="pack-page-contents" v-if="instance">
      <header>
        <div
          class="background"
          :style="{
            'background-image': `url(${packSplashArt})`,
          }"
        />
        <pack-meta-heading
          @back="goBack"
          :hidePackDetails="hidePackDetails"
          :versionType="versionType"
          :api-pack="apiPack ? apiPack : undefined"
          :instance="instance"
        />

        <pack-title-header
          v-if="!hidePackDetails"
          :pack-instance="apiPack"
          :instance="instance"
          :isInstalled="true"
          :pack-name="instance.name"
        />
      </header>

      <div class="body" :class="{ 'settings-open': activeTab === tabs.SETTINGS }">
        <pack-body
          @mainAction="launchModPack()"
          @update="update"
          @tabChange="(e) => (activeTab = e)"
          @showVersion="showVersions = true"
          :pack-loading="packLoading"
          :active-tab="activeTab"
          :isInstalled="true"
          :instance="instance"
          :pack-instance="apiPack"
          :allow-offline="offlineAllowed"
          @playOffline="offlineMessageOpen = true"
        />
      </div>

      <closable-panel :open="showVersions" @close="showVersions = false" title="Versions" subtitle="Upgrade or downgrade your pack version">
        <modpack-versions
          v-if="apiPack"
          :versions="apiPack.versions"
          :pack-instance="apiPack"
          :instance="instance"
          @close="showVersions = false"
        />
      </closable-panel>
    </div>
    <p v-else>No modpack found...</p>
    
    <Modal
      :open="offlineMessageOpen"
      :title="$route.query.presentOffline ? 'Unable to update your profile' : 'Play offline'"
      subTitle="Would you like to play in Offline Mode?"
      @closed="() => closeOfflineModel()"
    >
      <Message type="warning" class="mb-6 wysiwyg">
        <p>Please be aware, running in offline mode will mean you can not:</p>
        <ul>
          <li>Play on online servers</li>
          <li>Have a custom skin</li>
          <li>Have any profile specific content in-game</li>
        </ul> 
      </Message>

      <Input fill placeholder="Steve" label="Username" v-model="offlineUserName" class="text-base" />

      <template #footer>
        <div class="flex justify-end">
          <ui-button :icon="faPlay" type="success" @click="() => {
            playOffline()
            closeOfflineModel()
          }">Play offline</ui-button>
        </div>
      </template>
    </Modal>

    <modal
      :open="borkedVersionNotification != null"
      @closed="closeBorked"
      title="Version archived"
      sub-title="You will need to update or downgrade the pack"
      size="medium"
      :external-contents="true"
    >
      <versions-borked-modal
        @closed="closeBorked"
        @action="(e) => updateOrDowngrade(e)"
        :is-downgrade="borkedVersionIsDowngrade"
        :fixed-version="borkedVersionDowngradeId"
        :notification="borkedVersionNotification ?? undefined"
      />
    </modal>
  </div>
</template>