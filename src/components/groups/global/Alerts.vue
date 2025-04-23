<script lang="ts" setup>
import {Alert} from '@/core/controllers/alertController';
import {Queue} from '@/utils/std/queue';
import { useRouter } from 'vue-router';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { onMounted, onBeforeUnmount, ref, computed } from 'vue';
import { faCheckCircle, faExclamationCircle, faInfoCircle, faTimesCircle } from '@fortawesome/free-solid-svg-icons';
import { useAppStore } from '@/store/appStore.ts';

type AlertWithUuid = Alert & { uuid: string }

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

function removeAlert(alert: AlertWithUuid) {
  alerts.value = alerts.value.filter(a => a.uuid !== alert.uuid)
}

function createAlert() {
  if (alertsQueue.value.isEmpty()) return;
  if (alerts.value.length > 5) return;

  const alert = alertsQueue.value.dequeue();
  if (!alert) return;

  alerts.value.push(alert);

  setTimeout(() => {
    removeAlert(alert);

    // Check if there are more alerts to show as they could be in the queue
    createAlert();
  }, 5000)
}

const hiddenSidebar = computed(() => {
  return router.currentRoute.value.path.startsWith('/settings');
});
</script>

<template>
  <transition-group name="jump-in" :duration="150" class="alerts-container" :class="{'no-sidebar': hiddenSidebar}" v-if="alerts.length" tag="div">
    <div class="alert"
         v-for="(alert, index) in alerts"
         :key="alert.uuid"
         :class="[alert.type]"
         :style="{zIndex: 5000 - index}"
         @click="removeAlert(alert)"
    >
      <FontAwesomeIcon :icon="typeIcons[alert.type]"/>
      {{ alert.message }}
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
    align-items: center;
    background-color: #444444;
    padding: .7rem .8rem;
    gap: .6rem;
    border-radius: 5px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.4);
    cursor: pointer;
    
    max-width: 710px;
    
    svg {
      color: white;
    }
    
    &.success {
      background-color: var(--color-green-600);
    }
    
    &.error {
      background-color: var(--color-red-600);
    }
    
    &.warning {
      background-color: var(--color-orange-600);
    }
    
    &.info {
      background-color: var(--color-blue-600);
    }
  }
  
  .jump-in-enter, .jump-in-leave-to {
    opacity: 0;
    transform: scale(.3) translateX(-5px);
  }
}
</style>