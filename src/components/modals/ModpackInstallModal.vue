<script lang="ts" setup>
import {timeFromNow} from '@/utils/helpers/dateHelpers';
import {getColorForReleaseType} from '@/utils';
import {toTitleCase} from '@/utils/helpers/stringHelpers';
import {isValidVersion} from '@/utils/helpers/packHelpers';
import appPlatform from '@platform';
import {RouterNames} from '@/router';
import { ModalBody, Modal, FTBInput, UiToggle, Selection2, ModalFooter, UiButton } from '@/components/ui';
import { watch, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import ArtworkSelector from '@/components/groups/modpack/components/ArtworkSelector.vue';
import CategorySelector from '@/components/groups/modpack/create/CategorySelector.vue';
import { services } from '@/bootstrap.ts';
import { useModpackStore } from '@/store/modpackStore.ts';
import { ModPack, PackProviders } from '@/core/types/appTypes.ts';
import { faDownload } from '@fortawesome/free-solid-svg-icons';

const modpackStore = useModpackStore();
const router = useRouter();

const {
  open,
  packId,
  provider = "modpacksch",
} = defineProps<{
  open: boolean;
  packId: number;
  provider: PackProviders;
}>()

const apiModpack = ref<ModPack | null>(null);
const selectedVersionId = ref("");
const selectedCategory = ref("Default");

const allowPreRelease = ref(false);
const useAdvanced = ref(false);

const userPackName = ref("");
const userSelectedArtwork = ref<File | null>(null);

const emit = defineEmits<{
  (event: 'close'): void;
}>()

watch(() => open, async (newValue) => {
  if (newValue && !apiModpack.value) {
    apiModpack.value = await modpackStore.getModpack(packId, provider);

    userPackName.value = apiModpack.value?.name ?? "";

    // No stable versions, default to pre-release
    if (!hasStableVersion.value) {
      allowPreRelease.value = true;
      useAdvanced.value = true;
    }

    selectedVersionId.value = restrictedVersions.value[0].id.toString() ?? "";
  }
})

function install() {
  services().instanceInstallController.requestInstall({
    id: packId,
    category: selectedCategory.value,
    version: parseInt(selectedVersionId.value ?? sortedApiVersions.value[0].id),
    // Name fallback but it's not really needed
    name: userPackName.value ?? apiModpack.value?.name ?? "failed-to-name-the-modpack-somehow-" + appPlatform.utils.crypto.randomUUID().split("-")[0],
    logo: userSelectedArtwork.value?.path ?? null, // The backend will default for us.
    private: apiModpack.value?.private ?? false,
    provider,
  })

  emit('close')

  if (router.currentRoute.value.name !== RouterNames.ROOT_LIBRARY) {
    router.push({
      name: RouterNames.ROOT_LIBRARY
    })
  }
}

const packName = apiModpack.value?.name ?? "Loading...";
const restrictedVersions = computed(() => {
  let versions = sortedApiVersions.value;
  if (allowPreRelease.value) {
    versions = versions.filter(e => isValidVersion(e.type, "all"))
  } else {
    versions = versions.filter(e => isValidVersion(e.type, "release"))
  }

  return versions;
})

const sortedApiVersions = computed(() => {
  return (apiModpack.value && apiModpack.value.versions)
    ? [...apiModpack.value.versions].sort((a, b) => b.id - a.id)
    : [];
})

const hasStableVersion = computed(() => {
  return apiModpack.value?.versions
    .some(e => isValidVersion(e.type, "release")) ?? false
})

const hasUnstableVersions = computed(() => {
  return apiModpack.value?.versions
    .some(e => isValidVersion(e.type, "alpha") || isValidVersion(e.type, "beta") || isValidVersion(e.type, "hotfix"))
})

function versions() {
  return restrictedVersions.value
    .sort((a, b) => b.id - a.id)
    .map(e => ({
      value: e.id.toString(),
      label: e.name,
      meta: timeFromNow(e.updated),
      badge: {
        color: getColorForReleaseType(e.type),
        text: toTitleCase(e.type)
      }
    }))
}
</script>

<template>
  <Modal :open="open" @closed="emit('close')" title="Install instance" :sub-title="packName" :external-contents="true">
    <ModalBody>
      <template v-if="apiModpack">
        <ArtworkSelector :pack="apiModpack" class="mb-6" v-model="userSelectedArtwork" />
        <FTBInput label="Name" :placeholder="packName" v-model="userPackName" class="mb-4" />
        
        <CategorySelector class="mb-4" v-model="selectedCategory" />

        <UiToggle label="Show advanced options" v-model="useAdvanced" />
        <Selection2 :open-up="true" v-if="useAdvanced" label="Version" :options="versions()" v-model="selectedVersionId" class="mb-4 mt-6" />
        
        <UiToggle
          v-if="useAdvanced && hasUnstableVersions"
          v-model="allowPreRelease"
          label="Show pre-release builds (Stable by default)" 
          desc="Feeling risky? Enable pre-release builds to get access to Alpha and Beta versions of the Modpack"
        />
      </template>
    </ModalBody>
    <ModalFooter class="flex justify-end">
      <UiButton :wider="true" type="success" :icon="faDownload" @click="install">
        Install
      </UiButton>
    </ModalFooter>
  </Modal>
</template>