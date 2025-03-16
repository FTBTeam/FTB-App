<script lang="ts" setup>
import { ModPack, ModpackVersion, PackProviders } from '@/modules/modpacks/types';
import PackMetaHeading from '@/components/groups/modpack/PackMetaHeading.vue';
import PackTitleHeader from '@/components/groups/modpack/PackTitleHeader.vue';
import {ModpackPageTabs} from '@/views/InstancePage.vue';
import ModpackVersions from '@/components/groups/instance/ModpackVersions.vue';
import PackBody from '@/components/groups/modpack/PackBody.vue';
import ClosablePanel from '@/components/ui/ClosablePanel.vue';
import ModpackInstallModal from '@/components/modals/ModpackInstallModal.vue';
import {resolveArtwork, typeIdToProvider} from '@/utils/helpers/packHelpers';
import {ns} from '@/core/state/appState';
import {GetModpack, GetModpackVersion} from '@/core/state/modpacks/modpacksState';
import Loader from '@/components/ui/Loader.vue';
import {createLogger} from '@/core/logger';
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';

// TODO: [port] Fix me
// @Action("getModpack", ns("v2/modpacks")) getModpack!: GetModpack;
// @Action("getVersion", ns("v2/modpacks")) getVersion!: GetModpackVersion;
function getModpack(): ModPack {}
function getVersion(): ModpackVersion {}

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
  const modpackId = router.currentRoute.value.modpackid as string;
  const packID: number = parseInt(modpackId, 10);
  
  const packType = router.currentRoute.value.type as string;
  packTypeId.value = parseInt(packType, 10);

  try {
    logger.debug("Loading modpack", packID, this.packTypeId)
    const modpack = await getModpack({
      id: packID,
      provider: typeIdToProvider(this.packTypeId)
    })

    if (modpack) {
      const copyModpack = Object.assign({}, modpack);

      if (copyModpack.versions) {
        copyModpack.versions = copyModpack.versions.sort((a, b) => b.id - a.id)
      }
      
      currentModpack.value = copyModpack;
    }
  } catch (error) {
    logger.error("Failed to load modpack", error)
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
        <message icon="warning" type="danger" class="m-6">
          {{ error ? error : 'Something has gone wrong...' }}
        </message>
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

          <pack-title-header :pack-instance="currentModpack" :pack-name="currentModpack.name" />
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
            :instance="null"
            :pack-instance="currentModpack"
            :updating-mod-list="false"
          />
        </div>
      </div>
      <closable-panel :open="showVersions" @close="showVersions = false" title="Versions" subtitle="Upgrade or downgrade your pack version">
        <modpack-versions
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