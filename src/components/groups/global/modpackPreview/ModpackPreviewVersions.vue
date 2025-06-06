<script lang="ts" setup>
import {ModPack} from "@/core/types/appTypes.ts";
import {computed, onMounted, ref, watch} from "vue";
import {Loader, Selection2} from "@/components/ui";
import {SelectionOption} from "@/components/ui/Selection2.vue";
import {standardDate} from "@/utils/helpers/dateHelpers.ts";
import {modpackApi} from "@/core/pack-api/modpackApi.ts";
import {marked} from "marked";
import {alertController} from "@/core/controllers/alertController.ts";

const {
  modpack
} = defineProps<{
  modpack: ModPack
}>()

const selectedVersion = ref<number>(-1)
const changelogData = ref<string>("")
const loading = ref(false)

onMounted(() => {
  selectAndLoad();
})

watch(() => modpack, (newValue, oldValue) => {
  if (!newValue) {
    selectedVersion.value = -1;
    changelogData.value = "";
    return;
  }
  
  if (newValue.id === oldValue.id) return;

  selectAndLoad();
})

function selectAndLoad(id?: number | string | null) {
  if (typeof id === "string") {
    id = parseInt(id, 10);
  }
  
  let resolvedId;
  if (!id) {
    resolvedId = modpack.versions.sort((a, b) => b.id - a.id)[0]?.id;
  } else {
    resolvedId = id;
  }
  
  const resolvedVersion = modpack.versions.find(e => e.id === resolvedId);
  if (!resolvedVersion) {
    selectedVersion.value = -1;
    changelogData.value = "";
    return;
  }
  
  if (selectedVersion.value !== resolvedVersion.id) {
    selectedVersion.value = resolvedVersion.id;
  }
  
  loading.value = true;
  modpackApi.modpacks.getChangelog(modpack.id, resolvedVersion.id, modpack.provider === "modpacks.ch" ? "modpacksch" : "curseforge")
    .then(async data => changelogData.value = await marked.parse(data?.content ?? ""))
    .catch(e => alertController.error("Failed to load changelog", e))
    .finally(() => loading.value = false)
}

const options = computed<SelectionOption[]>(() => modpack.versions.map(e => ({
  label: e.name,
  value: e.id,
  meta: standardDate(e.updated)
})))
</script>

<template>
  <Selection2 class="mb-8" placeholder="Select version" v-model="selectedVersion" :options="options" @updated="(v) => selectAndLoad(v)" />
  
  <template v-if="!loading">
    <div v-if="changelogData" class="wysiwyg" v-html="changelogData"></div>
    <div v-else>No changelog data for this version</div>
  </template>
  <Loader v-else />
</template>