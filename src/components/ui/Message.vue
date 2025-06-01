<script lang="ts" setup>
import { IconDefinition } from '@fortawesome/free-brands-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { computed } from 'vue';

type MessageType = 'normal' | 'success' | 'info' | 'warning' | 'danger';

const {
  type = 'normal',
  icon,
  header = '',
} = defineProps<{
  type?: MessageType;
  icon?: IconDefinition;
  header?: string;
}>()

const typedStyles = {
  normal: 'bg-slate-600/20 border-slate-500 text-slate-200',
  success: 'bg-green-600/10 border-green-600 text-green-400',
  info: 'bg-blue-500/10 border-blue-500 text-blue-400',
  warning: 'bg-yellow-500/10 border-yellow-500 text-yellow-400',
  danger: 'bg-red-500/10 border-red-500 text-red-400',
};

const styles = computed(() => {
  return typedStyles[type]
})
</script>

<template>
  <article class="message border flex gap-4 items-start" :class="styles">
    <FontAwesomeIcon class="pt-0.5" fixedWidth size="lg" v-if="icon" :icon="icon" />
    <div class="message-body">
      <p v-if="header" class="mb-1">
        <b>{{ header }}</b>
      </p>
      <slot />
    </div>
  </article>
</template>

<style lang="scss" scoped>
.message {
  padding: 1rem;
  border-radius: .5rem;
}
</style>
