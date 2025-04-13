<script lang="ts" setup>
import PackMetaHeading from '@/components/groups/modpack/PackMetaHeading.vue';
import PackTitleHeader from '@/components/groups/modpack/PackTitleHeader.vue';
import {ModpackPageTabs} from '@/views/InstancePage.vue';
import ModpackVersions from '@/components/groups/instance/ModpackVersions.vue';
import PackBody from '@/components/groups/modpack/PackBody.vue';
import { ClosablePanel, Loader, UiMessage } from '@/components/ui';
import ModpackInstallModal from '@/components/modals/ModpackInstallModal.vue';
import {resolveArtwork, typeIdToProvider} from '@/utils/helpers/packHelpers';
import {createLogger} from '@/core/logger';
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ModPack } from '@/core/types/appTypes.ts';
import { useModpackStore } from '@/store/modpackStore.ts';
import { faWarning } from '@fortawesome/free-solid-svg-icons';

const modpackStore = useModpackStore();

const logger = createLogger("ModpackPage.vue")
const router = useRouter();

const activeTab = ref<ModpackPageTabs>(ModpackPageTabs.OVERVIEW);
const showVersions = ref(false);

const showInstallBox = ref(false);
const loading = ref(true);
const packTypeId = ref(0);

const currentModpack = ref<ModPack | null>(null);
const error = ref('');

onMounted(async () => {
  const modpackId = router.currentRoute.value.query.modpackid as string;
  const packID: number = parseInt(modpackId, 10);
  
  const packType = router.currentRoute.value.query.type as string;
  packTypeId.value = parseInt(packType, 10);

  try {
    logger.debug("Loading modpack", packID, packTypeId.value)
    const modpack = await modpackStore.getModpack(packID, typeIdToProvider(packTypeId.value));

    if (modpack) {
      const copyModpack = Object.assign({}, modpack);

      if (copyModpack.versions) {
        copyModpack.versions = copyModpack.versions.sort((a, b) => b.id - a.id)
      }
      
      currentModpack.value = copyModpack;
    }
  } catch (e) {
    logger.error("Failed to load modpack", e)
    loading.value = false;
    error.value =
      "Unable to find this modpack, it's possible something has failed to load. Try again in a few minutes...";
    return;
  }

  const showInstall = router.currentRoute.value.query.showInstall;
  if (showInstall === 'true') {
    logger.debug("Showing install box")
    showInstallBox.value = true;
  }

  loading.value = false;
});

const packSplashArt = resolveArtwork(currentModpack.value, 'splash');
const packType = typeIdToProvider(packTypeId.value);
</script>

<template>
  <div class="pack-container" v-if="!loading">
    <div class="pack-page">
      <div v-if="!currentModpack && !loading">
        <UiMessage :icon="faWarning" type="danger" class="m-6">
          {{ error ? error : 'Something has gone wrong...' }}
        </UiMessage>
      </div>
      <div class="pack-page-contents" v-if="currentModpack">
        <header>
          <div
            class="background"
            :style="{
            'background-image': `url(${packSplashArt})`,
          }"
          ></div>
          
          <pack-meta-heading
            @back="$router.back()"
            :hidePackDetails="false"
            versionType="release"
            :api-pack="currentModpack"
          />

          <PackTitleHeader :pack-instance="currentModpack" :pack-name="currentModpack.name" />
        </header>

        <div class="body">
          <pack-body
            @mainAction="showInstallBox = true"
            @update="() => {}"
            @tabChange="(e) => (activeTab = e)"
            @showVersion="showVersions = true"
            :searchingForMods="false"
            :active-tab="activeTab"
            :isInstalled="false"
            :instance="undefined"
            :pack-instance="currentModpack"
            :updating-mod-list="false"
          />
        </div>
      </div>
      <closable-panel v-if="currentModpack" :open="showVersions" @close="showVersions = false" title="Versions" subtitle="Upgrade or downgrade your pack version">
        <ModpackVersions
          :versions="currentModpack.versions"
          :pack-instance="currentModpack"
          :instance="null"
          :current="null"
          @close="showVersions = false"
        />
      </closable-panel>
    </div>
    
    <modpack-install-modal v-if="currentModpack" :pack-id="currentModpack.id" :provider="packType" :open="showInstallBox" @close="showInstallBox = false" />
  </div>
  <loader class="mt-20" v-else />
</template>