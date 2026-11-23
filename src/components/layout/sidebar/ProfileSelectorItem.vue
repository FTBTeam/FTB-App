<script setup lang="ts">
import Avatar from "@/components/layout/sidebar/Avatar.vue";
import {faEllipsis} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";

const {
  name,
  avatar,
  isRounded = false,
  isLarger = false
} = defineProps<{
  name: string;
  avatar?: string;
  isRounded?: boolean;
  isLarger?: boolean;
}>()

const emit = defineEmits<{
  (e: 'click'): void;
  (e: 'options', element: Element): void;
}>();
</script>

<template>
  <div class="flex gap-4 items-center" @click="emit('click')">
    <Avatar :url="avatar" :size="isLarger ? 'medium' : 'small'" :circle="isRounded" />
    <div class="flex-1 flex flex-col">
      <div class="font-bold" :class="isLarger ? 'text-lg' : ''">{{name}}</div>
      <slot />
    </div>
    <div class="px-2 py-2 cursor-pointer hover:bg-white/10 rounded transition-opacity duration-200" @click.stop="emit('options', $event.currentTarget as Element)">
      <FontAwesomeIcon fixed-width :icon="faEllipsis" />
    </div>
  </div>
</template>