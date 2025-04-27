<script lang="ts" setup>
import FindMods from '@/components/groups/instance/FindMods.vue';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {
  BaseData,
  CurseMetadata,
  InstanceJson,
  ModInfo,
  SugaredInstanceJson,
  UpdateAvailable
} from '@/core/types/javaApi';
import {containsIgnoreCase} from '@/utils/helpers/stringHelpers';
import {alertController} from '@/core/controllers/alertController';
import UiButton from '@/components/ui/UiButton.vue';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {JavaFetch} from '@/core/javaFetch';
import Loader from '@/components/ui/Loader.vue';
import UiToggle from '@/components/ui/UiToggle.vue';
import ClosablePanel from '@/components/ui/ClosablePanel.vue';
import {createLogger} from '@/core/logger';
import { ModPack } from '@/core/types/appTypes.ts';
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useAppStore } from '@/store/appStore.ts';
import platform from '@platform'
import { faDownload, faFolder, faPlus, faSearch, faSync } from '@fortawesome/free-solid-svg-icons';
// @ts-ignore (Literally no types :tada:)
import { RecycleScroller } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { Input } from '@/components/ui';
import {packBlacklist} from "@/store/modpackStore.ts";

type ApiMod = {
  fileId: number;
  name: string;
  synopsis: string;
  icon: string;
  curseSlug: string;
  curseProject: number;
  curseFile: number;
  stored: number;
  filename: string;
}

const logger = createLogger("ModpackMods.vue")

const {
  packInstalled,
  instance,
  apiPack,
} = defineProps<{
  packInstalled: boolean;
  instance?: InstanceJson;
  apiPack: ModPack | null;
}>();

const appStore = useAppStore();

const updatingModlist = ref(false);
const togglingShas = ref<string[]>([]);
const hiddenMods = ref<string[]>([]);
const search = ref('');
const modsLoading = ref(false);
const apiMods = ref<ApiMod[]>([]);
const modlist = ref<ModInfo[]>([]);
const searchingForMods = ref(false);

const modUpdates = ref<Record<string, [ModInfo, CurseMetadata]>>({});
const modUpdatesAvailableKeys = ref<string[]>([]);
const updatingModShas = ref<string[]>([]);

// We're forced to track the packets as we don't get the success response in the same packet as the request response
// We also do not get any file data in the response, so we have to track the sha1
const updatingModPacketToShas = ref<Record<string, string>>({});

onMounted(async () => {
  if (packInstalled) {
    appStore.emitter.on('ws/message', onModUpdateEvent)
  }

  if (!packInstalled && apiPack) {
    const latestVersion = apiPack.versions.sort((a, b) => b.id - a.id).find(e => !e.private);
    if (!latestVersion) {
      return;
    }

    const provider = apiPack.provider === "modpacks.ch" ? "modpack" : "curseforge";
    if (!packBlacklist.includes(apiPack.id)) {
      console.log(packBlacklist, apiPack.id)
      const mods = await toggleBeforeAndAfter(
        () => JavaFetch.modpacksCh(`${provider}/${apiPack.id}/${latestVersion.id}/mods`).execute(),
        s => modsLoading.value = s
      );

      const loadedApiMods = mods?.json<{ mods: ApiMod[] }>()?.mods;
      if (!loadedApiMods) {
        return;
      }

      apiMods.value = loadedApiMods.sort((a, b) => (a.name ?? a.filename).localeCompare((b.name ?? b.filename)))
    }
  } else {
    getModList().catch(e => logger.error(e))
  }
});

async function getModList(showAlert = false) {
  try {
    const mods = await toggleBeforeAndAfter(() => sendMessage("instanceMods", {
      uuid: instance?.uuid ?? "",
      _private: instance?._private ?? false,
    }), state => updatingModlist.value = state)

    // try and keep the previous curse data
    if (modlist.value) {
      for (const mod of mods.files) {
        const previous = modlist.value.find(e => e.sha1 === mod.sha1);
        if (previous) {
          mod.curse = previous.curse;
        }
      }
    }

    modlist.value = mods.files;
  } catch (e) {
    alertController.error("Unable to load mods for this instance...")
  }

  if (showAlert) {
    alertController.success('The mods list has been updated')
  }
}

onUnmounted(() => {
  if (packInstalled) {
    appStore.emitter.off('ws/message', onModUpdateEvent)
  }
})

function onModUpdateEvent(data: BaseData & any) {
  if (data.type !== "instanceModUpdate" && data.type !== "instanceInstallModReply" && data.type !== "instanceModRichData") {
    return;
  }

  if (data.type === "instanceModRichData") {
    const mod = modlist.value.find(e => e.sha1 === data.file.sha1);
    if (mod) {
      mod.curse = data.richData;
    }

    return;
  }

  if (data.type === "instanceInstallModReply") {
    if (data.status === "success") {
      getModList(false)
      if (updatingModPacketToShas.value[data.requestId]) {
        const sha = updatingModPacketToShas.value[data.requestId];
        updatingModShas.value.splice(updatingModShas.value.indexOf(sha), 1);
        delete updatingModPacketToShas.value[data.requestId];
      }
    }

    return;
  }

  const typedPayload = data as UpdateAvailable;
  
  modUpdates.value[typedPayload.file.sha1] = [typedPayload.file, typedPayload.update];
  modUpdatesAvailableKeys.value = Object.keys(modUpdates.value);
}

async function updateMod(key: string) {
  if (!modUpdates.value[key]) {
    return;
  }

  const [mod, update] = modUpdates.value[key];
  const result = await sendMessage("instanceInstallMod", {
    uuid: instance!.uuid,
    modId: update.curseProject,
    versionId: update.curseFile
  })

  if (result.status === "success" || result.status === "processing") {
    if (result.status === "processing") {
      updatingModPacketToShas.value[result.requestId] = mod.sha1;
      updatingModShas.value.push(key);
    } else {
      getModList(false) // This isn't really something that happens
    }
  }
}

function updateAll() {
  for (const key of modUpdatesAvailableKeys.value) {
    updateMod(key).catch(e => logger.error(e))
  }
}

async function openModsFolder() {
  if ((instance as SugaredInstanceJson).rootDirs.includes("mods")) {
    await platform.io.openFinder(platform.io.pathJoin((instance as SugaredInstanceJson).path, "mods"));
  } else {
    await platform.io.openFinder((instance as SugaredInstanceJson).path);
  }
}

async function toggleMod(file: ModInfo) {
  togglingShas.value.push(file.sha1)
  try {
    const toggleQuery = await sendMessage("instanceModToggle", {
      uuid: instance!.uuid,
      fileName: file.fileName,
      fileId: file.fileId
    });

    if (toggleQuery.successful) {
      file.enabled = !file.enabled;
    } else {
      alertController.warning( `Failed to ${!file.enabled ? 'enable' : 'disable'} ${file.fileName ?? (file as any).name}`)
    }
  } catch (e) {
    alertController.warning( `Failed to ${!file.enabled ? 'enable' : 'disable'} ${file.fileName ?? (file as any).name}`)
  } finally {
    togglingShas.value.splice(togglingShas.value.indexOf(file.sha1), 1);
  }
}

watch(search, (value) => {
  onSearch(value);
})

function onSearch(value: string) {
  search.value = value;
  if (value === '') {
    hiddenMods.value = [];
    return;
  }

  if (packMods.value) {
    hiddenMods.value = packMods.value.filter((e) => containsIgnoreCase(e.fileName, value)).map((e) => e.fileName.toLowerCase());
  }

  if (apiMods.value) {
    hiddenMods.value = apiMods.value.filter((e) => containsIgnoreCase(e.name ?? e.filename, value)).map((e) => (e.name ?? e.filename).toLowerCase());
  }
}

const packMods = computed(() => {
  const results = modlist.value.filter(e => search.value === '' ? true : containsIgnoreCase(e.curse?.name ?? e.fileName, search.value));
  if (results.length === 0 && search.value !== '') {
    return [{
      fileId: -1,
      fileName: 'No results found',
      version: '',
      enabled: true,
      size: 0,
      sha1: '',
      curse: {} as any,
      murmurHash: ''
    }]
  }

  return results.sort((a, b) => (a.curse?.name ?? a.fileName).localeCompare((b.curse?.name ?? b.fileName)));
});

const simpleMods = computed(() => {
  const results = apiMods.value.filter(e => search.value === '' ? true : containsIgnoreCase(e.name ?? e.filename, search.value));
  if (results.length === 0 && search.value !== '') {
    return [{
      name: 'No results found',
      synopsis: 'Try searching for something else',
      icon: '',
      fileId: -1,
      curseSlug: '',
      curseProject: -1,
      curseFile: -1,
      stored: -1,
      filename: '',
    }]
  }

  return results.sort((a, b) => (a.name ?? a.filename).localeCompare((b.name ?? b.filename)));
})

const installedMods = computed<[number, number][]>(() => {
  return packMods.value?.filter(e => e.curse).map(e => [e.curse.curseProject, e.curse.curseFile])
}) 
</script>

<template>
  <div class="modpack-mods">
    <div class="flex mb-8 gap-4 items-center relative z-50">
      <Input :icon="faSearch" fill placeholder="Search..." v-model="search" />
      <template v-if="packInstalled && instance">
        <ui-button type="success" @click="searchingForMods = true" :icon="faPlus" :data-balloon-length="instance.locked ? 'medium' : undefined" :aria-label="instance.locked ? 'This instance is locked, to add more content you will need to unlock it in settings.' : 'Add more mods'" :disabled="instance.locked" />
        <ui-button type="info" @click="updateAll" :icon="faDownload" :data-balloon-length="instance.locked ? 'medium' : undefined" :aria-label="instance.locked ? 'This instance is locked, to add more content you will need to unlock it in settings.' : 'Update all mods'" :disabled="instance.locked || modUpdatesAvailableKeys.length === 0" />
        <ui-button type="info" :icon="faSync" aria-label="Refresh mod list" aria-label-pos="down-right" :disabled="updatingModlist" @click="getModList(true)" />
        <ui-button type="info" @click="openModsFolder" :icon="faFolder" data-balloon-length="medium" aria-label="Open mods folder" data-balloon-pos="down-right" />
      </template>
    </div>
    
    <div class="mods">
      <loader v-if="!packInstalled && modsLoading" />
      <template v-if="!packInstalled">
        <recycle-scroller :items="simpleMods" key-field="fileId" :item-size="70" class="select-text scroller" v-slot="{ item }">
          <div class="flex items-center gap-6 mb-4" :key="item.fileId">
            <div class="flex gap-6 items-start flex-1">
              <img v-if="item.icon" :src="item.icon" class="rounded mt-2" width="40" alt="">
              <div class="placeholder bg-black rounded mt-2" style="width: 40px; height: 40px" v-else-if="item.fileName !== ''"></div>
              <div class="main">
                <b class="mb-1 block">{{item.name || item.filename}}</b>
                <p class="only-one-line">{{item.synopsis}}</p>
              </div>
            </div>
            <div>
              <a target="_blank" rel="noopener" class="curse-btn cursor-pointer" aria-label="Open on CurseForge" data-balloon-pos="down-right" v-if="item.curseSlug" :href="`https://curseforge.com/minecraft/mc-mods/${item.curseSlug}`">
                <img src="../../../assets/curse-logo.svg" width="24" alt="Open on CurseForge" />
              </a>
            </div>
          </div>
        </recycle-scroller>
      </template>
      
      <template v-if="packInstalled && instance">
        <p class="text-center italic opacity-80" v-if="!packMods.length">{{ instance.name }} does not have any mods installed</p>
        <recycle-scroller v-else class="complex-mod mod" :items="packMods" :item-size="70" key-field="sha1" v-slot="{ item }">
          <div class="flex gap-6 items-center mb-4">
            <img v-if="item.curse && item.curse.icon" :src="item.curse.icon" class="rounded" width="40" alt="">
            <div class="placeholder bg-black rounded mt-2" style="width: 40px; height: 40px" v-else-if="item.fileName !== ''"></div>
            
            <div class="main flex-1 transition-opacity duration-200" :class="{'opacity-50': !item.enabled}">
              <div class="mb-1 block select-text font-bold">
                <div v-if="item.curse?.name" class="flex gap-2 items-center">
                  <a target="_blank" rel="noopener" class="curse-btn cursor-pointer hover:underline" aria-label="Open on CurseForge" data-balloon-pos="down-left" :href="`https://curseforge.com/minecraft/mc-mods/${item.curse.slug}`">
                    {{item.curse.name}}
                  </a>
                </div>
                <template v-else>
                  {{item.fileName}}
                </template>
              </div>
              <p class="text-sm opacity-75 select-text" v-if="item.curse?.name">{{item.fileName}}</p>
              <p class="text-sm opacity-75 text-muted italic" v-else>(Not found on CurseForge)</p>
            </div>
            
            <div class="meta flex gap-6 items-center">
              <div class="update" v-if="!instance.locked && modUpdatesAvailableKeys.includes(item.sha1) && !updatingModShas.includes(item.sha1)" :aria-label="`Update available (${modUpdates[item.sha1][0].fileName} -> ${modUpdates[item.sha1][1].name})`" data-balloon-pos="down-right" @click="updateMod(item.sha1)">
                <FontAwesomeIcon :icon="faDownload" :fixed-width="true" />
              </div>
              <div class="updating" v-if="!instance.locked && updatingModShas.includes(item.sha1)">
                <FontAwesomeIcon :spin="true" icon="circle-notch" :fixed-width="true" />
              </div>
              <ui-toggle class="mr-1" v-if="item.fileName !== ''"  @input="() => toggleMod(item)" :value="item.enabled" :disabled="togglingShas.includes(item.sha1)" />
            </div>
          </div>
        </recycle-scroller>
      </template>
    </div>

    <closable-panel
      v-if="packInstalled"
      :open="searchingForMods"
      @close="searchingForMods = false"
      title="Search for mods"
      subtitle="Powered by CurseForge"
    >
      <find-mods :instance="instance" :installed-mods="installedMods" @modInstalled="getModList" />
    </closable-panel>
  </div>
</template>

<style scoped lang="scss">
.only-one-line {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
