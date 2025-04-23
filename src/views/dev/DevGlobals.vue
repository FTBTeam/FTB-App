<script lang="ts" setup>
import { Input, UiButton } from '@/components/ui';
import { alertController } from '@/core/controllers/alertController.ts';
import { dialogsController } from '@/core/controllers/dialogsController.ts';
import { useModalStore } from '@/store/modalStore.ts';
import { useAppStore } from '@/store/appStore.ts';
import { ref } from 'vue';
import appPlatform from '@platform';

const modalStore = useModalStore();
const appStore = useAppStore();

// TODO: Query versions and provide selector
const changelogVersion = ref(appPlatform.config.version === "Unknown" ? "1.27.4" : appPlatform.config.version);

function showToast(type: 'info' | 'error' | 'success' | 'warning', message?: string) {
  alertController[type](message ?? `This is a ${type} toast!`);
}

function showConfirmDialog() {
  dialogsController.createConfirmationDialog("Are you sure", "But no really, are you?")
    .then(e => alertController.info("You clicked " + (e ? "yes" : "no")))
}

function showBackendModal() {
  modalStore.openModal({
    type: 'modal', secret: '', requestId: crypto.randomUUID(),
    title: "Backend modal",
    message: "This is a backend modal, it can be closed by clicking the button below.",
    buttons: [
      {
        name: "Close",
        type: "success",
        message: "close"
      }
    ],
    id: crypto.randomUUID()
  })
}

function showChangelog() {
  appStore.emitter.emit("action/force-changelog-open", changelogVersion.value);
}

function randomLongString() {
  // Generate a long random, space seperated, text based string by generating chars from hex
  // and joining them with a space
  return Array.from({ length: 100 }, () => Math.floor(Math.random() * 16).toString(16))
    .join(' ')
    .toUpperCase();
}
</script>

<template>
  <p class="font-bold uppercase mb-4 text-white/80">Toasts</p>
  <div class="flex gap-4">
    <UiButton type="success" @click="() => showToast('success')">Success</UiButton>
    <UiButton type="info" @click="() => showToast('info')">Info</UiButton>
    <UiButton type="warning" @click="() => showToast('warning')">Warning</UiButton>
    <UiButton type="danger" @click="() => showToast('error')">Error</UiButton>
    <UiButton @click="() => showToast('info', randomLongString())">Long toast</UiButton>
  </div>
  
  <p class="font-bold uppercase mt-8 mb-4 text-white/80">Dialogs</p>
  <div class="flex gap-4">
    <UiButton @click="showConfirmDialog">Show confirm dialog</UiButton>
  </div>

  <p class="font-bold uppercase mt-8 mb-4 text-white/80">Backend modal</p>
  <div class="flex gap-4">
    <UiButton @click="showBackendModal">Open backend modal</UiButton>
  </div>

  <p class="font-bold uppercase mt-8 mb-4 text-white/80">Changelog</p>
  <div class="flex gap-4 items-center">
    <div>
      <Input v-model="changelogVersion" />
    </div>
    <UiButton @click="showChangelog">Trigger Changelog modal</UiButton>
  </div>
</template>