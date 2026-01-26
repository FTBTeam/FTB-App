<script lang="ts" setup>
import FindMods from '@/components/groups/instance/FindMods.vue';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {
  AllRichModData,
  BaseData,
  CurseMetadata,
  InstanceJson,
  ModInfo,
  UpdateAvailable
} from '@/core/types/javaApi';
import {containsIgnoreCase} from '@/utils/helpers/stringHelpers';
import {alertController} from '@/core/controllers/alertController';
import UiButton from '@/components/ui/UiButton.vue';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import UiToggle from '@/components/ui/UiToggle.vue';
import ClosablePanel from '@/components/ui/ClosablePanel.vue';
import {createLogger} from '@/core/logger';
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useAppStore } from '@/store/appStore.ts';
import {
  faCircleNotch,
  faDownload, faEllipsisVertical,
  faFilter,
  faPlus,
  faSearch,
} from '@fortawesome/free-solid-svg-icons';
// @ts-ignore (Literally no types :tada:)
import { RecycleScroller } from 'vue-virtual-scroller'
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { Input } from '@/components/ui';
import {prettyByteFormat} from "@/utils";
import UiSelect from "@/components/ui/select/UiSelect.vue";
import {AppContextController} from "@/core/context/contextController.ts";
import {ContextMenus} from "@/core/context/contextMenus.ts";

const logger = createLogger("ModpackMods.vue")

const {
  instance,
} = defineProps<{
  instance: InstanceJson;
}>();

const filters = [
  { value: "None", key: "none" },
  { value: "Enabled", key: "enabled" },
  { value: "Disabled", key: "disabled" },
]

const appStore = useAppStore();

const updatingModlist = ref(false);
const togglingShas = ref<string[]>([]);
const search = ref('');
const modlist = ref<ModInfo[]>([]);
const searchingForMods = ref(false);
const filterType = ref('none');

const modUpdates = ref<Record<string, [ModInfo, CurseMetadata]>>({});
const modUpdatesAvailableKeys = ref<string[]>([]);
const updatingModShas = ref<string[]>([]);

// We're forced to track the packets as we don't get the success response in the same packet as the request response
// We also do not get any file data in the response, so we have to track the sha1
const updatingModPacketToShas = ref<Record<string, string>>({});

onMounted(async () => {
  appStore.emitter.on('ws/message', onModUpdateEvent)
  getModList().catch(e => logger.error(e))
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
  appStore.emitter.off('ws/message', onModUpdateEvent)
})

function onModUpdateEvent(data: BaseData & any) {
  if (data.type !== "instanceModUpdate" && data.type !== "instanceInstallModReply" && data.type !== "instanceAllModRichData") {
    return;
  }

  if (data.type === "instanceAllModRichData") {
    const typedData = data as AllRichModData;
    for (const richDataContainer of typedData.richModData) {
      const { file, richData } = richDataContainer;
      if (!richData) {
        continue;
      }
      
      const mod = modlist.value.find(e => e.sha1 === file.sha1);
      if (mod) {
        mod.curse = richData;
      }
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
      await getModList(false) // This isn't really something that happens
    }
  }
}

// TODO: This has basically no feedback. Awful UX.
function updateAll() {
  for (const key of modUpdatesAvailableKeys.value) {
    updateMod(key).catch(e => logger.error(e))
  }
}

async function toggleMod(file: ModInfo) {
  togglingShas.value.push(file.sha1)
  try {
    const toggleQuery = await sendMessage("instanceModToggle", {
      uuid: instance!.uuid,
      fileName: file.enabled ? file.fileName : file.fileName + '.disabled',
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
}

const packMods = computed(() => {
  let results = modlist.value.filter(e => search.value === '' ? true : containsIgnoreCase(e.curse?.name ?? e.fileName, search.value));
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
  
  if (filterType.value === 'enabled') {
    results = results.filter(e => e.enabled);
  } else if (filterType.value === 'disabled') {
    results = results.filter(e => !e.enabled);
  }
  
  results.sort((a, b) => (a.curse?.name ?? a.fileName).localeCompare((b.curse?.name ?? b.fileName)))

  return results
    .reduce((itt, e, index) => {
      return [...itt, {
        index,
        ...e
      }]
    }, [] as ModInfo[]);
});

const installedMods = computed<[number, number][]>(() => {
  return packMods.value?.filter(e => e.curse).map(e => [e.curse.curseProject, e.curse.curseFile])
}) 
</script>

<template>
  <div class="modpack-mods">
    <div class="flex mb-8 gap-4 items-center relative z-50">
      <Input :icon="faSearch" class="flex-1" fill placeholder="Search..." v-model="search" />
      
      <ui-button type="success" @click="searchingForMods = true" :icon="faPlus" :data-balloon-length="instance.locked ? 'medium' : undefined" :aria-label="instance.locked ? 'This instance is locked, to add more content you will need to unlock it in settings.' : 'Add more mods'" :disabled="instance.locked" />
      <ui-button @click.prevent.stop="(e) => AppContextController.openMenu(ContextMenus.MODPACK_MODS_MENU, e, () => ({ 
        instance: instance,
        hasModUpdates: modUpdatesAvailableKeys.length,
        actions: {
          refreshMods: () => getModList(true),
          updateAll: () => updateAll(),
        }
      }))" :icon="faEllipsisVertical">Options</ui-button>
      <ui-select :options="filters" :icon="faFilter" placeholder="Filter" placement="bottom-end" v-model="filterType" />
    </div>
    
    <div class="mods">      
      <p class="text-center italic opacity-80" v-if="!packMods.length">{{ instance.name }} does not have any mods installed</p>
      <recycle-scroller v-else class="complex-mod mod" :items="packMods" :item-size="54" key-field="index" v-slot="{ item }">
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
            <p class="text-sm opacity-75 select-text" v-if="item.curse?.name">{{ prettyByteFormat(item.size) }} - {{item.fileName}}</p>
            <p class="text-sm opacity-75 select-text text-muted italic" v-else>{{ prettyByteFormat(item.size) }}</p>
          </div>
          
          <div class="meta flex gap-6 items-center">
            <div class="update" v-if="!instance.locked && modUpdatesAvailableKeys.includes(item.sha1) && !updatingModShas.includes(item.sha1)" :aria-label="`Update available (${modUpdates[item.sha1][0].fileName} -> ${modUpdates[item.sha1][1].name})`" data-balloon-pos="down-right" @click="updateMod(item.sha1)">
              <FontAwesomeIcon :icon="faDownload" :fixed-width="true" />
            </div>
            <div class="updating" v-if="!instance.locked && updatingModShas.includes(item.sha1)">
              <FontAwesomeIcon :spin="true" :icon="faCircleNotch" :fixed-width="true" />
            </div>
            <ui-toggle class="mr-1" :use-model="false" v-if="item.fileName !== ''"  @input="() => toggleMod(item)" :value="item.enabled" :disabled="togglingShas.includes(item.sha1)" />
          </div>
        </div>
      </recycle-scroller>
    </div>

    <closable-panel
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
