<script lang="ts" setup>
import { IconDefinition } from '@fortawesome/free-brands-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { ref } from 'vue';

const {
  icon,
  label,
  disabled = false,
  fill = false,
  hint
} = defineProps<{
  icon?: IconDefinition;
  label?: string;
  disabled?: boolean;
  fill?: boolean;
  hint?: string;
}>();

const focused = ref(false);

const elmId = `input-${Math.random().toString(36).substring(2, 9)}`;
</script>

<template>
  <div :class="{ 'w-full': fill }">
    <label :for="elmId" v-if="label" class="inline-block text-sm font-bold uppercase text-white/80 mb-2 transition-color duration-300" :class="{ 'opacity-50 cursor-not-allowed': disabled, 'text-white/100': focused }">
      {{ label }}
    </label>
    <div class="flex gap-2 items-center">
      <div class="relative flex-1">
        <FontAwesomeIcon v-if="icon" :icon="icon" class="absolute left-3 top-1/2 transform -translate-y-1/2 text-white/70 transition-color duration-300" :class="{'text-white/90': focused}" />
        <slot
          :focus="() => focused = true"
          :blur="() => focused = false"
          class="outline-none pr-3 py-2 rounded-lg border border-white/30 focus:border-white/80 transition-color duration-300"
          :class="{
              'pl-9': icon,
              'pl-3': !icon,
              'w-full': fill,
              'hover:border-white/60': !disabled,
              'opacity-50 cursor-not-allowed': disabled,
           }"
        />
      </div>
      <slot name="suffix" />
    </div>
    <small v-if="hint" class="text-xs text-white/60 mt-2 block">
      {{ hint }}
    </small>
  </div>
</template>