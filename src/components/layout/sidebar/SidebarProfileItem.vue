<script lang="ts" setup>
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {faTrash} from "@fortawesome/free-solid-svg-icons";

const { profile, subtext, onDelete, onSelect, active } = defineProps<{
  profile: {
    name: string;
    avatarUrl?: string;
  };
  subtext?: string;
  onDelete: () => void;
  onSelect: () => void;
  active: boolean
}>()
</script>

<template>
  <div 
    class="item flex gap-4 p-4 border border-white/20 rounded cursor-pointer items-center"
    :class="{'!border-green-500 bg-green-500/5': active}" 
    @click="onSelect"
  >
    <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="Profile Avatar" class="w-10 h-10 rounded-full mr-2" />
    <div v-else class="w-10 h-10 rounded-full bg-gray-500 flex items-center justify-center mr-2">
      <FontAwesomeIcon icon="fa-solid fa-user" class="text-white" />
    </div>
    
    <div class="flex-1">
      <div class="font-semibold">{{ profile.name }}</div>
      <div class="text-sm" v-if="subtext">{{ subtext }}</div>
    </div>
    <div class="edit-action px-1.5 py-0.5 border border-red-500 bg-red-500/30 hover:bg-red-500/70 rounded opacity-0 transition-opacity duration-200" @click.stop="onDelete">
      <FontAwesomeIcon fixed-width size="xs" :icon="faTrash" />
    </div>
  </div>
</template>

<style scoped>
.item:hover {
  
  .edit-action {
    opacity: 1;
  }
}
</style>