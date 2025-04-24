<script lang="ts" setup>
import Loader from '@/components/ui/Loader.vue';
import UiButton from '@/components/ui/UiButton.vue';
import {InstanceJson, SugaredInstanceJson} from '@/core/types/javaApi';
import {parseMarkdown} from '@/utils';
import {typeIdToProvider} from '@/utils/helpers/packHelpers';
import {modpackApi} from '@/core/pack-api/modpackApi';
import {alertController} from '@/core/controllers/alertController';
import {createLogger} from '@/core/logger';
import { ref, watch } from 'vue';
import { Message, Modal, ModalBody, ModalFooter } from '@/components/ui';
import { Versions } from '@/core/types/appTypes.ts';
import { faCheck } from '@fortawesome/free-solid-svg-icons';
import { useAppStore } from '@/store/appStore.ts';

const logger = createLogger("UpdateConfirmModal.vue");

const {
  open,
  localInstance,
  latestVersion
} = defineProps<{
  open: boolean;
  localInstance: InstanceJson | SugaredInstanceJson;
  latestVersion: Versions;
}>()

const emit = defineEmits<{
  (e: 'close'): void;
}>()

const appStore = useAppStore();

const isUnstable = ref(false);
const showConfirm = ref(false);
const loadingChanges = ref(true);
const changes = ref<string | null>(null)

function close() {
  isUnstable.value = false;
  showConfirm.value = false;
  changes.value = null;
  emit('close');
}

watch(() => open, (newValue) => {
  if (!newValue) {
    return;
  }

  if (!latestVersion || !localInstance) {
    logger.error("No latest version or local instance provided, can't open update modal")
    return;
  }

  isUnstable.value = latestVersion?.type.toLowerCase() !== 'release';

  loadingChanges.value = true;
  loadChanges()
    .catch(e => logger.error("Failed to load changes", e))
    .finally(() => (loadingChanges.value = false));
})

function update() {
  if (!latestVersion) {
    logger.error("No latest version provided, can't update")
    return;
  }
  
  appStore.controllers.install.requestUpdate(
    localInstance,
    latestVersion,
    typeIdToProvider(localInstance.packType)
  )

  close();
}

async function loadChanges() {
  if (!latestVersion) {
    logger.error("No latest version provided, can't load changes")
    return;
  }

  try {
    const packReq = await modpackApi.modpacks.getChangelog(localInstance.id, latestVersion.id, typeIdToProvider(localInstance.packType));
    if (packReq === null) {
      changes.value = null;
      return;
    }
    
    changes.value = packReq.content;
  } catch (e) {
    console.error(e)
    alertController.error("Failed to load changelog...")
  }
}
</script>

<template>
  <Modal
    :open="open"
    :title="`Update ${localInstance.version} to ${latestVersion.name}`"
    sub-title="Are you sure you want to update to the latest version of this pack?"
    :externalContents="true"
    @closed="close"
  >
    <ModalBody>
      <Message type="danger" class="mb-4" v-if="isUnstable">
        <p>
          <b class="font-bold">WARNING!</b> You're about to update to an unstable version of the pack. Bugs are
          expected and likely.
        </p>
      </Message>

      <Loader key="changes-loading" v-if="loadingChanges" title="Fetching Changelog" />
      <div key="changes-loading-else" v-else>
        <p>
          This version comes with the following changes, if you are sure you want to update, click confirm, otherwise
          close this message.
        </p>

        <Message v-if="changes === null" key="changes-available" type="danger" class="mt-6">
          Unable to find a changelog for {{ latestVersion.name }}... this doesn't seem right 🤔. You can still update
          even if the changelog isn't loading.
        </Message>

        <div v-else class="wysiwyg changes mt-6" v-html="parseMarkdown(changes)" />
      </div>
    </ModalBody>
    <ModalFooter class="flex justify-end">
      <UiButton :wider="true" :icon="faCheck" @click="update" type="success">
        Confirm
      </UiButton>
    </ModalFooter>
  </Modal>
</template>
