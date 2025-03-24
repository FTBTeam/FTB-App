<script lang="ts" setup>
import platform from '@/utils/interface/electron-overwolf';
import {getColorForReleaseType, parseMarkdown} from '@/utils';
import {typeIdToProvider} from '@/utils/helpers/packHelpers';
import {InstanceJson} from '@/core/types/javaApi';
import {RouterNames} from '@/router';
import {modpackApi} from '@/core/pack-api/modpackApi';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import Selection2 from '@/components/ui/Selection2.vue';
import dayjs from 'dayjs';
import {alertController} from '@/core/controllers/alertController';
import UiButton from '@/components/ui/UiButton.vue';
import {createLogger} from '@/core/logger';
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { services } from '@/bootstrap.ts';
import { ModPack, Versions } from '@/core/types/appTypes.ts';

const logger = createLogger("ModpackVersions.vue")

const {
  versions,
  packInstance,
  instance
} = defineProps<{
  versions: Versions[];
  packInstance: ModPack;
  instance: InstanceJson;
}>()

const router = useRouter();

const changelogs = ref<Record<string, string>>({});
const currentVersion = ref<Versions | null>(null);
const activeLog = ref(-1);
const loading = ref(true);
const version = ref<number | string>(-1);

onMounted(() => {
  const sortedVersion = versions.sort((a, b) => b.id - a.id);

  const currentId = instance?.versionId ?? packInstance?.versions[0]?.id ?? -1;
  const lcurrent = sortedVersion.find(e => e.id === currentId)?.id ?? sortedVersion[0].id;
  version.value = lcurrent;

  // get the first log
  fetchLog(lcurrent)
    .then((data) => {
      changelogs.value['' + lcurrent] = data;
      setActive(lcurrent);
    })
    .catch(e => logger.error(e))
})

onUnmounted(() => {
  changelogs.value = {};
})

watch(version, async (newValue) => {
  newValue = parseInt(newValue as string);
  if (changelogs.value['' + newValue]) {
    setActive(newValue);
    return;
  }

  changelogs.value['' + newValue] = await fetchLog(newValue);
  setActive(newValue);
});

function setActive(versionId: number) {
  activeLog.value = versionId;
  currentVersion.value = versions.find((e) => e.id === activeLog.value) ?? null;
}

async function fetchLog(versionId: number) {
  try {
    const changelog = await toggleBeforeAndAfter(
      () => modpackApi.modpacks.getChangelog(packInstance.id, versionId, isCursePack ? "curseforge" : "modpacksch"),
      state => loading.value = state
    )

    return changelog?.content ?? `No changelog available for this version`;
  } catch (e) {
    logger.error(e)
    alertController.warning("Unable to load changelog")
    return "Unable to locate changelog for this version";
  }
}

function isOlderVersion(version: number) {
  return instance?.versionId > version;
}

function update() {
  if (!instance || !currentVersion.value) {
    // Non-instances can't be updated
    return;
  }

  services.instanceInstallController.requestUpdate(instance, currentVersion.value, typeIdToProvider(instance.packType));
  router.push({
    name: RouterNames.ROOT_LIBRARY
  })
}

const packVersions = computed(() => {
  return [...versions].sort((a, b) => b.id - a.id).map(e => ({
    value: e.id,
    label: e.name + (instance?.versionId === e.id ? ' (Current)' : ''),
    meta: dayjs.unix(e.updated).format("DD MMMM YYYY, HH:mm"),
    badge: {color: getColorForReleaseType(e.type), text: e.type}
  })) ?? []
});

const isCursePack = computed(() => {
  return packInstance.type.toLowerCase() == "curseforge" || packInstance.id > 1000;
})
</script>

<template>
  <div class="pack-versions">
    <div class="aside mb-6">
      <selection2
        :badge-end="true"
        :options="packVersions"
        v-model="version"
      />
    </div>
    <div class="main flex pb-8 flex-col" :key="activeLog">
      <div class="heading flex items-center mb-4" v-if="currentVersion">
        <div class="content flex-1 mr-4">
          <p class="opacity-50">Changelog for</p>
          <div class="font-bold name text-xl">{{ currentVersion.name }}</div>
        </div>
        <div class="buttons flex text-sm gap-2">
          <ui-button type="info" size="small" icon="server" v-if="instance && instance.packType === 0" @click="() => platform.get.utils.openUrl(`https://go.ftb.team/serverfiles`)">Server files</ui-button>
          <ui-button
            :wider="true"
            v-if="instance && currentVersion && instance.versionId !== activeLog && currentVersion.type.toLowerCase() !== 'archived'" 
            :type="isOlderVersion(currentVersion.id) ? 'warning' : 'success'" size="small" icon="download" @click="update">
            {{ isOlderVersion(currentVersion.id) ? 'Downgrade' : 'Update' }}
          </ui-button>
        </div>
      </div>
      <div class="body-contents flex-1 select-text">
        <div v-if="loading" class="loading"><font-awesome-icon icon="spinner" class="mr-2" spin /> Loading...</div>
        <div
          v-else-if="currentVersion && currentVersion.type.toLowerCase() !== 'archived'"
          class="bg-orange-400 text-orange-900 font-bold px-4 py-4 rounded mb-6"
        >         
          <font-awesome-icon icon="triangle-exclamation" size="xl" class="mr-4" /> Always backup your worlds before updating.
        </div>
        <message class="mb-4 shadow-lg mr-4 mt-2" type="danger" v-if="currentVersion && currentVersion.type.toLowerCase() === 'archived'">
          <p>
            This version has been archived! This typically means the update contained a fatal error causing the version
            to be too unstable for players to use.
          </p>
        </message>
        
        <div class="wysiwyg" v-if="!loading && changelogs[activeLog] && changelogs[activeLog] !== ''" v-html="parseMarkdown(changelogs[activeLog])" />
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.pack-versions {
  height: 100%;
  position: relative;
  font-size: 0.875rem;

  line-height: 1.8em;

  .aside,
  .main {
    > p {
      font-size: 1.25rem;
    }
  }

  .main {
    flex: 1;
  }

  .closer {
    cursor: pointer;
    padding: 0.5rem 0.8rem;
    font-size: 1.2rem;
    position: absolute;
    top: -1rem;
    right: -2rem;
    opacity: 0.5;
    transition: opacity 0.2s ease-in-out;
    &:hover {
      opacity: 1;
    }
  }
}
</style>
