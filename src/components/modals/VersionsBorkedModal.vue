<script lang="ts" setup>
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { UiButton, ModalBody, ModalFooter } from '@/components/ui';
import { faCheck, faTimes } from '@fortawesome/free-solid-svg-icons';

const {
  isDowngrade = true,
  fixedVersion,
  notification,
} = defineProps<{
  isDowngrade: boolean;
  fixedVersion: number | null;
  notification?: string;
}>()
</script>

<template>
  <div>
    <ModalBody>
      <p v-if="fixedVersion">
        The version of the modpack you are currently on has been archived. This means something has gone wrong with this
        version in an unrecoverable way. In this instance, we recommend you
        {{ isDowngrade ? 'downgrade' : 'update' }} the modpack.
      </p>

      <p v-else>
        The version of the modpack you're currently on has been archived. Unfortunately we do not currently have a safe
        version to update or downgrade to. In this case, we recommend you update as soon as an update is available
        (Which should be very soon).
        <span class="block mb-2" />
        You can continue to play the pack but note it may be unstable or not able to start.
      </p>

      <div class="notification mt-4" v-if="notification">
        <b class="font-bold mb-1 block">Message from developer</b>
        <p>{{ notification }}</p>
      </div>
    </ModalBody>
    <ModalFooter class="flex justify-end">
      <UiButton
        class="py-2 px-4"
        :type="fixedVersion != null ? 'danger' : 'warning'"
        @click="$emit('closed')"
      >
        <FontAwesomeIcon :icon="faTimes" class="mr-2" size="1x" />
        {{ fixedVersion !== null ? 'Ignore' : 'Close' }}
      </UiButton>
      <UiButton
        v-if="fixedVersion != null"
        class="py-2 px-8 ml-4"
        type="primary"
        @click="$emit('action', fixedVersion)"
      >
        <FontAwesomeIcon :icon="faCheck" class="mr-2" size="1x" />
        {{ isDowngrade ? 'Downgrade pack' : 'Update pack' }}
      </UiButton>
    </ModalFooter>
  </div>
</template>