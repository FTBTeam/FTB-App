<template>
  <transition-group name="jump-in" duration="150" class="alerts-container" :class="{'no-sidebar': hiddenSidebar}" v-if="alerts.length" tag="div">
    <div class="alert"
         v-for="(alert, index) in alerts"
         :key="alert.uuid"
         :class="[alert.type]"
         :style="{zIndex: 5000 - index}"
         @click="removeAlert(alert)"
    >
      <font-awesome-icon :icon="typeIcons[alert.type]"/>
      {{ alert.message }}
    </div>
  </transition-group>
</template>

<script lang="ts">
import {Alert} from '@/core/controllers/alertController';
import {Queue} from '@/utils/std/queue';
import {emitter} from '@/utils';

type AlertWithUuid = Alert & { uuid: string }

@Component
export default class Alerts extends Vue {
  typeIcons = {
    success: "check-circle",
    error: "times-circle",
    warning: "exclamation-circle",
    info: "info-circle",
  }
  
  alerts: AlertWithUuid[] = [];
  alertsQueue: Queue<AlertWithUuid> = new Queue();
  
  mounted() {
    (emitter as any).on("alert.simple", this.onSimpleAlert);
  }
  
  destroyed() {
    (emitter as any).off("alert.simple", this.onSimpleAlert);
  }
  
  onSimpleAlert(data: Alert) {
    // Submit the alert to the queue
    this.alertsQueue.enqueue({...data, uuid: Math.random().toString(36).substring(7)});

    // Check the queue
    this.createAlert();
  }

  removeAlert(alert: AlertWithUuid) {
    this.alerts = this.alerts.filter(a => a.uuid !== alert.uuid)
  }
  
  createAlert() {
    if (this.alertsQueue.isEmpty()) return;
    if (this.alerts.length > 5) return;
    
    const alert = this.alertsQueue.dequeue();
    if (!alert) return;
    
    this.alerts.push(alert);
    
    setTimeout(() => {
      this.removeAlert(alert);
      
      // Check if there are more alerts to show as they could be in the queue
      this.createAlert();
    }, 5000)
  }

  get hiddenSidebar() {
    return this.$route.path.startsWith('/settings');
  }
}
</script>

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
      background-color: var(--color-success-button);
    }
    
    &.error {
      background-color: var(--color-danger-button);
    }
    
    &.warning {
      background-color: var(--color-warning-button);
    }
    
    &.info {
      background-color: var(--color-info-button);
    }
  }
  
  .jump-in-enter, .jump-in-leave-to {
    opacity: 0;
    transform: scale(.3) translateX(-5px);
  }
}
</style>