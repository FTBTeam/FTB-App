<script lang="ts" setup>
import {Alert} from '@/core/controllers/alertController';
import {Queue} from '@/utils/std/queue';
import { useRouter } from 'vue-router';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { onMounted, onBeforeUnmount, ref, computed } from 'vue';
import {
  faCheck,
  faCheckCircle,
  faCopy,
  faExclamationCircle,
  faInfoCircle,
  faTimesCircle
} from '@fortawesome/free-solid-svg-icons';
import { useAppStore } from '@/store/appStore.ts';
import platform from '@platform';

type AlertWithUuid = Alert & { uuid: string, copied?: boolean, mouseOver?: boolean };

const router = useRouter();
const appStore = useAppStore();

const typeIcons = {
  success: faCheckCircle,
  error: faTimesCircle,
  warning: faExclamationCircle,
  info: faInfoCircle,
}

const alerts = ref<AlertWithUuid[]>([]);
const alertsQueue = ref<Queue<AlertWithUuid>>(new Queue());

onMounted(() => {
  appStore.emitter.on("alert/simple", onSimpleAlert);
})

onBeforeUnmount(() => {
  appStore.emitter.off("alert/simple", onSimpleAlert);
})

function onSimpleAlert(data: Alert) {
  // Submit the alert to the queue
  alertsQueue.value.enqueue({...data, uuid: Math.random().toString(36).substring(7)});

  // Check the queue
  createAlert();
}

function removeAlert(alert: AlertWithUuid, force = false) {
  if (alert.persistent && !force) {
    return;
  }
  
  // Call the callback if it exists
  if (alert.onClose) {
    alert.onClose();
  }
  
  alerts.value = alerts.value.filter(a => a.uuid !== alert.uuid)
}

function createAlert() {
  if (alertsQueue.value.isEmpty()) return;
  if (alerts.value.length > 5) return;

  const alert = alertsQueue.value.dequeue();
  if (!alert) return;

  alerts.value.push(alert);
  waitAndRemove(alert);
}

function waitAndRemove(alert: AlertWithUuid, duration = 5000) {
  if (alert.persistent) return;
  
  setTimeout(() => {
    if (alert.mouseOver) {
      // Queue the removal again but with a shorter duration
      waitAndRemove(alert, 2000);
      return;
    }
    
    removeAlert(alert);

    // Check if there are more alerts to show as they could be in the queue
    createAlert();
  }, duration)
}

const hiddenSidebar = computed(() => {
  return router.currentRoute.value.path.startsWith('/settings');
});

function copy(alert: AlertWithUuid) {
  platform.cb.copy(alert.message);
  alert.copied = true;
  setTimeout(() => {
    // Technically the alert could be removed in this time so we need to check if it still exists
    if (!alert) {
      return;
    }
    
    alert.copied = false;
  }, 2000);
}

const typeToColors = {
  success: 'from-green-600/40',
  error: 'from-red-600/40',
  warning: 'from-orange-600/40',
  info: 'from-blue-600/40',
}
</script>

<template>
  <transition-group name="jump-in" :duration="150" class="alerts-container" :class="{'no-sidebar': hiddenSidebar}" tag="div">
    <div class="alert text-lg rounded-lg"
         v-for="(alert, index) in alerts"
         :key="alert.uuid"
         :class="[alert.type]"
         :style="{zIndex: 5000 - index}"
         @click="removeAlert(alert, true)"
         @mouseover="alert.mouseOver = true"
         @mouseleave="alert.mouseOver = false"
    >
      <div class="pl-3 pr-6 rounded-l-lg icon-container flex items-center justify-center bg-gradient-to-r" :class="`${typeToColors[alert.type]}`">
        <FontAwesomeIcon :icon="typeIcons[alert.type]"/>
      </div>
      <p class="whitespace-pre-wrap py-3 -ml-5 font-medium">{{ alert.message }}</p>
      <div class="flex items-center gap-1 px-4 opacity-80 hover:opacity-100 transition-opacity duration-200">
        <FontAwesomeIcon :icon="alert.copied ? faCheck : faCopy" @click.stop="copy(alert)" />
        <FontAwesomeIcon :icon="faTimesCircle" class="ml-2 p-1" v-if="!alert.persistent" @click.stop="removeAlert(alert, true)" />
      </div>
    </div>
  </transition-group>
</template>

<style lang="scss" scoped>
.alerts-container {
  position: absolute;
  bottom: 1rem;
  left: 86px;
  z-index: 1000;
  display: flex;
  align-items: flex-start;
  flex-direction: column;
  gap: .75rem;
  
  &.no-sidebar {
    left: 1rem;
  }

  .alert {
    position: relative;
    transition: transform .15s ease-in-out, opacity .15s ease-in-out;
    display: flex;
    align-items: stretch;
    background-color: #444444;
    gap: 1rem;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
    cursor: pointer;
    max-width: 710px;

    svg {
      color: white;
    }
  }
  
  .jump-in-enter-from, .jump-in-leave-to {
    opacity: 0;
    transform: scale(.3) translateX(-5px);
  }
}
</style>