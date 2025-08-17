<script lang="ts" setup>
import { UiButton, Input, ModalBody, ModalFooter } from '@/components/ui';
import CategorySelector from '@/components/groups/modpack/create/CategorySelector.vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { RouterNames } from '@/router';
import { sendMessage } from '@/core/websockets/websocketsApi';
import { alertController } from '@/core/controllers/alertController';
import { equalsIgnoreCase } from '@/utils/helpers/stringHelpers';
import { useInstanceStore } from '@/store/instancesStore.ts';
import { onMounted, ref } from 'vue';
import { safeNavigate } from '@/utils';
import { faCopy, faSpinner } from '@fortawesome/free-solid-svg-icons';

const {
  instanceName,
  uuid,
  category
} = defineProps<{
  instanceName: string;
  uuid: string;
  category: string;
}>();

const emit = defineEmits<{
  (e: 'finished'): void;
}>();

const instanceStore = useInstanceStore();

const newName = ref('');
const newCategory = ref('');

const working = ref(false);
const done = ref(false);
const status = ref('');

onMounted(() => {
  const duplicateCount = getDuplicateNameCount();
  if (duplicateCount > 1) {
    newName.value = instanceName + ' (copy ' + (duplicateCount + 1) + ')';
  } else {
    newName.value = instanceName + ' (copy)';
  }
  newCategory.value = category;
})

// TODO: Move to instance controller duplicate method
async function duplicate() {
  if (working.value) {
    return;
  }

  working.value = true;
  status.value = 'Starting duplication';

  const result = await sendMessage("duplicateInstance", {
    uuid: uuid,
    newName: newName.value,
    category: newCategory.value
  }, 1_000 * 60 * 5); // 5 minutes (this should be more than long enough!)

  if (!result.success) {
    working.value = false;
    alertController.error(result.message)
    return;
  }

  status.value = 'Refreshing modpacks';
  instanceStore.addInstance(result.instance)

  status.value = '';
  done.value = false;
  working.value = false;
  newName.value = '';

  await safeNavigate(RouterNames.ROOT_LIBRARY)
  emit('finished');
}

function getDuplicateNameCount() {
  return instanceStore.instances.filter(i => equalsIgnoreCase(i.name, instanceName)).length
}
</script>

<template>
  <div>
    <ModalBody>
      Duplicating {{ instanceName }} will copy all of the contents of this pack to a new instance.

      <Input
        v-model="newName"
        class="mt-4 mb-4"
        label="New instance name"
        :disabled="working || done"
        fill
      />
      
      <CategorySelector v-model="newCategory" />
    </ModalBody>
    <ModalFooter class="flex justify-end">
      <UiButton
        :disabled="working || done"
        color="primary"
        @click="duplicate"
      >
        <template v-if="!working && !done">
          <FontAwesomeIcon :icon="faCopy" class="mr-2" size="1x" />
          Duplicate
        </template>
        <template v-else>
          <FontAwesomeIcon :icon="faSpinner" class="mr-2" spin size="1x" />
          {{ status }}
        </template>
      </UiButton>
    </ModalFooter>
  </div>
</template>
