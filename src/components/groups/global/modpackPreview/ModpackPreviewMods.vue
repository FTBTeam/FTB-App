<script lang="ts" setup>
import {ModPack, ModpackMod, ModpackVersion} from "@/core/types/appTypes.ts";
import {computed, onMounted, ref, watch} from "vue";
import {Loader, Selection2} from "@/components/ui";
import {alertController} from "@/core/controllers/alertController.ts";
import {useModpackStore} from "@/store/modpackStore.ts";
import {modpackApi} from "@/core/pack-api/modpackApi.ts";
import {Input} from "@/components/ui";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {faQuestion, faSearch} from "@fortawesome/free-solid-svg-icons";
import {prettyByteFormat} from "@/utils";
import {SelectionOptions} from "@/components/ui/Selection2.vue";
import {useGlobalStore} from "@/store/globalStore.ts";

const {
  modpack
} = defineProps<{
  modpack: ModPack
}>()

const modpackStore = useModpackStore();
const globalStore = useGlobalStore();

const version = ref<ModpackVersion | null>(null)
const mods = ref<ModpackMod[] | null>(null)
const loading = ref(false)
const search = ref("")
const sortBy = ref<"name" | "size" | "authors">("name")
const viewMode = ref<'list' | 'grid' | 'table'>("list")
const orderBy = ref<'asc' | 'desc'>("asc")

onMounted(() => {
  loading.value = true;
  loadMods(modpack)
    .finally(() => loading.value = false)
    .catch(e => {
      console.error(e)
      alertController.error("Failed to load mods")
    });
})

watch(() => modpack, (newValue, oldValue) => {
  if (newValue.id === oldValue.id) return;
  
  version.value = null;
  mods.value = null;
  
  loading.value = true;
  loadMods(newValue)
    .finally(() => loading.value = false)
    .catch(e => {
      console.error(e)
      alertController.error("Failed to load mods")
    })
})

async function loadMods(modpack: ModPack) {  
  const latestVersion = modpack.versions.sort((a, b) => b.id - a.id)[0];
  if (!latestVersion) {
    alertController.error("No version found");
    return;
  }
  
  const localVersion = await modpackStore.getVersion(modpack.id, latestVersion.id, modpack.provider === "curseforge" ? "curseforge" : "modpacksch");
  if (!localVersion) {
    alertController.error("No version found");
    return;
  }
  
  version.value = localVersion;
  try {
    const req = await modpackApi.modpacks.getMods(modpack.id, latestVersion.id, modpack.provider === "curseforge" ? "curseforge" : "modpacksch");
    if (req?.status === "success") {
      mods.value = req.mods;
    }
  } catch (e) {
    console.error(e)
    alertController.error("Failed to load mods")
  }
}

const finalMods = computed(() => {
  if (!mods.value) return [];
  
  let sortedMods: ModpackMod[] = [];
  
  if (sortBy.value === "authors") {
    sortedMods = mods.value.sort((a, b) => {
      const aAuthors = (a.curseAuthors ?? []).map(e => e.name).join(", ");
      const bAuthors = (b.curseAuthors ?? []).map(e => e.name).join(", ");
      return orderBy.value === "asc" ? aAuthors.localeCompare(bAuthors) : bAuthors.localeCompare(aAuthors);
    });
  } else if (sortBy.value === "size") {
    sortedMods = mods.value.sort((a, b) => {
      const aSize = a.size ?? 0;
      const bSize = b.size ?? 0;
      return orderBy.value === "asc" ? aSize - bSize : bSize - aSize;
    });
  } else {
    sortedMods = mods.value.sort((a, b) => {
      const aName = a.name ?? a.filename;
      const bName = b.name ?? b.filename;
      return orderBy.value === "asc" ? aName.localeCompare(bName) : bName.localeCompare(aName);
    });
  }
  
  // Search filter
  if (search.value) {
    return sortedMods?.filter(mod => {
      const name = mod.name ?? mod.filename;
      return name.toLowerCase().includes(search.value.toLowerCase());
    });
  }
  
  return sortedMods;
})

function iconClicked(mod: ModpackMod) {
  globalStore.openImagePreview({ url: mod.icon ?? "", name: mod.name });
}

const sortOptions: SelectionOptions = [
  { label: "Name", value: "name" },
  { label: "Size", value: "size" },
  { label: "Authors", value: "authors" }
]

const viewOptions: SelectionOptions = [
  { label: "List", value: "list" },
  { label: "Grid", value: "grid" },
  { label: "Table", value: "table" }
]

const orderOptions: SelectionOptions = [
  { label: "Ascending", value: "asc" },
  { label: "Descending", value: "desc" }
]
</script>

<template>
  <Loader v-if="loading" />
  <div v-else>
    <div class="mb-8 flex justify-between items-center">
      <div class="w-3/8">
        <Input :icon="faSearch" fill v-model="search" placeholder="Search..." />
      </div>
      <div class="flex gap-2">
        <Selection2 direction="right" :min-width="180" :options="viewOptions" v-model="viewMode" placeholder="List"></Selection2>
        <Selection2 direction="right" :min-width="180" :options="sortOptions" v-model="sortBy" placeholder="Sort"></Selection2>
        <Selection2 direction="right" :min-width="180" :options="orderOptions" v-model="orderBy" placeholder="Order"></Selection2>
      </div>
    </div>

    <div class="grid grid-cols-4 gap-6" v-if="viewMode === 'grid'">
      <div v-for="(mod, index) in finalMods" :key="index">
        <div class="bg-black/20 cursor-pointer aspect-square flex items-center justify-center rounded-lg relative">
          <img @click="iconClicked(mod)" class="rounded-lg cursor-pointer" v-if="mod.icon" :src="mod.icon" />

          <div v-if="mod.curseAuthors" class="absolute bottom-2 left-2 flex gap-1 flex-wrap items-start">
            <a @click.stop class="bg-black/70 backdrop-blur rounded px-1 text-sm text-white/80" target="_blank" :href="author.url" v-for="(author, index) in mod.curseAuthors.slice(0, 2)" :key="index">
              {{ (index === 0 ? 'By ' : '') + author.name }}
            </a>
          </div>
        </div>
        <a :href="`https://www.curseforge.com/minecraft/mc-mods/${mod.curseSlug}`" target="_blank" :title="mod.name ?? mod.filename" class="font-bold mt-2 text-lg mb-1 line-clamp-2">{{ mod.name ?? mod.filename }}</a>
        <p :title="mod.synopsis ?? '' + mod.size" class="line-clamp-2 opacity-85">{{mod.synopsis ?? prettyByteFormat(mod.size)}}</p>
      </div>
    </div>

    <div v-if="viewMode === 'table'">
      <div class="flex gap-6 mb-6">
        <div class="w-8"></div>
        <div class="w-2/4">Name / About</div>
        <div class="w-1/4">File Name / Size</div>
        <div class="w-1/4">Authors</div>
      </div>
      <div class="flex flex-col gap-3">
        <div class="flex gap-6" v-for="(mod, index) in finalMods" :key="index">
          <div class="w-8">
            <img @click="iconClicked(mod)" :src="mod.icon" v-if="mod.icon" alt="Mod Icon" class="w-8 h-8 rounded-lg" />
          </div>
          <div class="w-2/4" v-if="mod.name">
            <a :href="`https://www.curseforge.com/minecraft/mc-mods/${mod.curseSlug}`" target="_blank">{{mod.name}}</a>
            <div :title="mod.synopsis" class="text-sm opacity-85 line-clamp-1" v-if="mod.synopsis">{{mod.synopsis}}</div>
          </div>
          <div class="w-2/4" v-if="!mod.name">
            <div :title="mod.filename" class="line-clamp-1">{{mod.filename}}</div>
            <div class="text-sm opacity-85">Not found on CurseForge</div>
          </div>
          <div class="w-1/4">
            <div :title="mod.filename" class="line-clamp-1 w-full break-all">{{mod.filename}}</div>
            <div :title="'' + mod.size" class="text-sm opacity-85">{{prettyByteFormat(mod.size)}}</div>
          </div>
          <div class="w-1/4 break-all">{{(mod.curseAuthors ?? []).map(e => e.name).slice(0, 2).join(', ')}}</div>
        </div>
      </div>
    </div>

    <div class="flex flex-col gap-6" v-if="viewMode === 'list'">
      <div v-for="(mod, index) in finalMods" :key="index" class="flex items-center gap-6">
        <template v-if="mod.curseProject">
          <img @click="iconClicked(mod)" :src="mod.icon" alt="Mod Icon" class="cursor-pointer w-16 h-16 rounded-lg" />
          <div>
            <a class="hover:underline" :href="`https://www.curseforge.com/minecraft/mc-mods/${mod.curseSlug}`" target="_blank">
              <h2 class="text-lg font-semibold">{{ mod.name }}</h2>
            </a>
            <p class="text-white/80 line-clamp-1">{{ mod.synopsis }}</p>
          </div>
        </template>
        <template v-else>
          <div class="w-12 h-12 rounded flex items-center justify-center bg-black/10">
            <FontAwesomeIcon :icon="faQuestion" />
          </div>
          <div>
            <h2 class="text-lg font-semibold">{{ mod.filename }}</h2>
            <p class="text-sm text-white/80 line-clamp-1">Mod not found on CurseForge</p>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>