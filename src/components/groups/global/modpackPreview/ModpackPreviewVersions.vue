<script lang="ts" setup>
import {ModPack} from "@/core/types/appTypes.ts";
import {computed, onMounted, ref, watch} from "vue";
import {Loader} from "@/components/ui";
import {standardDate} from "@/utils/helpers/dateHelpers.ts";
import {modpackApi} from "@/core/pack-api/modpackApi.ts";
import {marked} from "marked";
import {alertController} from "@/core/controllers/alertController.ts";
import {UiSelectOption} from "@/components/ui/select/UiSelect.ts";
import UiSelect from "@/components/ui/select/UiSelect.vue";

const {
  modpack
} = defineProps<{
  modpack: ModPack
}>()

const selectedVersion = ref<string>("-1")
const changelogData = ref<string>("")
const loading = ref(false)

onMounted(() => {
  selectAndLoad();
})

watch(() => modpack, (newValue, oldValue) => {
  if (!newValue) {
    selectedVersion.value = "-1";
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
    selectedVersion.value = "-1";
    changelogData.value = "";
    return;
  }
  
  if (selectedVersion.value !== resolvedVersion.id.toString()) {
    selectedVersion.value = resolvedVersion.id.toString();
  }
  
  loading.value = true;
  modpackApi.modpacks.getChangelog(modpack.id, resolvedVersion.id, modpack.provider === "modpacks.ch" ? "modpacksch" : "curseforge")
    .then(async data => changelogData.value = await marked.parse(data?.content ?? ""))
    .catch(e => alertController.error("Failed to load changelog", e))
    .finally(() => loading.value = false)
}

const options = computed(() => modpack.versions.map(e => ({
  key: e.id.toString(),
  value: e.name,
  date: standardDate(e.updated)
}) as UiSelectOption<{ date: string }>))
</script>

<template>
  <UiSelect :options="options" v-model="selectedVersion" @update:modelValue="(v) => selectAndLoad(v)" class="mb-8" placeholder="Select version" :min-width="260">
    <template #option="{ option, clazz }">
      <div class="flex items-center justify-between" :class="clazz">
        <div class="flex-1">{{ option.value }}</div>
        <div class="text-white/80">{{ option.date }}</div>
      </div>
    </template>
  </UiSelect>
  
  <template v-if="!loading">
    <div v-if="changelogData" class="wysiwyg" v-html="changelogData"></div>
    <div v-else>No changelog data for this version</div>
  </template>
  <Loader v-else />
</template>