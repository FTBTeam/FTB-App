<script lang="ts" setup>
import {SelectionOption} from '@/components/ui/Selection2.vue';
import {ModLoadersResponse, ModLoaderWithPackId} from '@/core/types/modpacks/modloaders';
import {toggleBeforeAndAfter} from '@/utils/helpers/asyncHelpers';
import {JavaFetch} from '@/core/javaFetch';
import { UiToggle, UiMessage, Loader, Selection2 } from '@/components/ui';
import { computed, onMounted, ref, watch } from 'vue';
import { toTitleCase } from '@/utils/helpers/stringHelpers.ts';
import { faExclamation, faInfo } from '@fortawesome/free-solid-svg-icons';
import { createLogger } from '@/core/logger.ts';

const logger = createLogger("ModloaderSelect.vue")

const {
  mcVersion,
  showNone = true,
  showOptional = false,
  provideLatestOption = true
} = defineProps<{
  mcVersion: string;
  showNone?: boolean;
  showOptional?: boolean;
  provideLatestOption?: boolean;
}>()

const availableLoaders = ref<Record<string, ModLoaderWithPackId[]>>({});
const loadingModloaders = ref(false);

const userLoaderProvider = ref("");
const userUseLatestLoader = ref(true);
const userLoaderVersion = ref("");

const emit = defineEmits<{
  (e: 'select', value: [string, ModLoaderWithPackId]): void;
}>()

onMounted(async () => {
  await loadLoaders()
})

watch(() => mcVersion, async () => {
  await loadLoaders();
})

function select(loader: string | null, version: string | null): [string, ModLoaderWithPackId] | null {
  if (loader === null || version === null) {
    return null;
  }

  const loaderProvider = availableLoaders.value[loader];
  if (!loaderProvider) {
    return null;
  }

  const modLoader = loaderProvider.find(e => e.version === version);
  if (!modLoader) {
    return null;
  }

  emit("select", [loader, modLoader]);
  return [loader, modLoader];
}

async function loadLoaders() {
  await toggleBeforeAndAfter(() => loadAvailableLoaders(mcVersion), state => loadingModloaders.value = state);
}

async function loadAvailableLoaders(mcVersion: string) {
  const knownLoaders = ["forge", "neoforge", "fabric"];
  const foundLoadersForVersion = {} as Record<string, ModLoaderWithPackId[]>;

  for (const loader of knownLoaders) {
    const request = await JavaFetch.modpacksCh(`loaders/${mcVersion}/${loader}`).execute()
    if (request?.status !== "success") {
      logger.log(`Failed to fetch loaders for ${loader}`)
      continue;
    }

    const loaderData = request.json<ModLoadersResponse>();
    if (loaderData.total === 0 || !loaderData.loaders) {
      continue;
    }

    foundLoadersForVersion[loader] = loaderData.loaders
      .sort((a, b) => b.id - a.id)
      .map(e => ({...e, packId: loaderData.id, }));
  }

  availableLoaders.value = foundLoadersForVersion;
}

watch(userLoaderProvider, (newValue, oldValue) => {
  if (newValue === oldValue) {
    return;
  }

  if (userLoaderProvider.value === "") {
    userLoaderVersion.value = "";
    select(null, null)
    return;
  }

  userLoaderVersion.value = loaderVersions.value[0]?.value ?? "";
  select(userLoaderProvider.value, userLoaderVersion.value)
})

const loaderVersions = computed(() => {
  if (userLoaderProvider.value === "" || !availableLoaders.value[userLoaderProvider.value]) {
    return []
  }

  return availableLoaders.value[userLoaderProvider.value].map((e: any) => ({
    label: e.version,
    value: e.version,
    meta: e.type
  })) as SelectionOption[]
})

const hasAvailableLoaders = computed(() => Object.keys(availableLoaders.value).length > 0)
</script>

<template>
 <div class="modloaderSelect">
   <UiMessage header="Optional" :icon="faInfo" type="info" class="mb-4" v-if="showOptional && hasAvailableLoaders">
     A Mod Loader will allow you to load mods into Minecraft. Each Mod Loader will have different mods available so we recommend looking at <a>CurseForge</a> to see what's available.
   </UiMessage>
   
   <Loader v-if="loadingModloaders" />
   
   <template v-else-if="hasAvailableLoaders">
     <b class="mb-4 block">Select a Mod Loader</b>

     <div class="loaders mb-6">
       <div class="loader" :class="{active: userLoaderProvider === index}" v-for="(_, index) in availableLoaders" :key="index" @click="userLoaderProvider = index">
         {{ toTitleCase(index) }}
       </div>
       <div class="loader" v-if="showNone" :class="{active: userLoaderProvider === ''}" @click="userLoaderProvider = ''">None</div>
     </div>

     <UiToggle v-if="provideLatestOption && userLoaderProvider !== ''" class="mb-4" label="Use latest Mod Loader version (recommended)" desc="The latest version of each modloader is typically the most stable version" v-model="userUseLatestLoader" />

     <Selection2
       v-if="userLoaderProvider !== ''"
       :open-up="true"
       label="Version"
       class="mb-4"
       :disabled="loaderVersions.length === 0 || (provideLatestOption && userUseLatestLoader)"
       :options="loaderVersions"
       v-model="userLoaderVersion"
       @change="select(userLoaderProvider, userLoaderVersion)"
     />
   </template>

   <UiMessage v-else header="No loaders" :icon="faExclamation" type="warning">
     Sadly we were not able to find any mod loaders for Minecraft {{ mcVersion }}. This is likely due to there being no mod loaders available just yet.
     <br><br>
     You can continue to create an instance without a mod loader and set one up later once one is available.
   </UiMessage>
 </div>
</template>

<style lang="scss" scoped>
.loaders {
  display: flex;

  .loader {
    cursor: pointer;
    flex: 1;
    padding: .8rem 0;
    text-align: center;
    background-color: var(--color-navbar);
    transition: background-color .25s ease-in-out;
    font-weight: bold;

    &:hover {
      background-color: var(--color-light-primary-button);
    }

    &.active {
      background-color: var(--color-primary-button);
    }

    &:first-child {
      border-radius: 8px 0 0 8px;
    }

    &:last-child {
      border-radius: 0 8px 8px 0;
    }

    &:not(:last-child) {
      border-right: 1px solid rgba(white, .05);
    }
  }
}
</style>