<script lang="ts" setup>
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {faEllipsis} from "@fortawesome/free-solid-svg-icons";
import {AppContextController} from "@/core/context/contextController.ts";
import {ContextMenus} from "@/core/context/contextMenus.ts";

const { isMinecraft = true, isPlayingAsCard = false, profile, subtext, onDelete, onSelect, active } = defineProps<{
  isMinecraft?: boolean;
  isPlayingAsCard?: boolean;
  profile: {
    name: string;
    avatarUrl?: string;
  };
  subtext?: string;
  onDelete: () => void;
  onSelect: () => void;
  active: boolean;
}>()

function openContext(e: any) {
  const targetElm = e.target.getBoundingClientRect 
    ? e.currentTarget.getBoundingClientRect()
    : {height: 0, width: 0, x: e.clientX + 5, y: e.clientY + 5};
  
  AppContextController.openMenu(ContextMenus.PROFILE_MENU, {
    x: targetElm.x + targetElm.width + 5,
    y: targetElm.y
  }, () => ({
    profile: {
      uuid: "",
    }
  }))
}
</script>

<template>
  <div 
    class="item flex gap-4 cursor-pointer items-center"
    :class="{'border-green-500! bg-green-500/5': active && !isPlayingAsCard, 'p-4 border border-white/20 rounded-lg': !isPlayingAsCard}" 
    @click="onSelect"
  >
    <img v-if="profile.avatarUrl" :src="profile.avatarUrl" alt="Profile Avatar" class="mr-2" :class="{'w-10 h-10': !isPlayingAsCard, 'w-14 h-14': isPlayingAsCard, 'rounded': isMinecraft, 'rounded-full': !isMinecraft}" />
    <div v-else class="w-10 h-10 bg-gray-500 flex items-center justify-center mr-2" :class="{'rounded': isMinecraft, 'rounded-full': !isMinecraft}">
      <FontAwesomeIcon icon="fa-solid fa-user" class="text-white" />
    </div>
    
    <div class="flex-1">
      <div class="font-bold text-lg">{{ profile.name }}</div>
      <div class="text-sm" v-if="subtext">{{ subtext }}</div>
    </div>
    <div class="px-2 py-2 hover:bg-white/10 rounded transition-opacity duration-200" @click.stop="openContext">
      <FontAwesomeIcon fixed-width :icon="faEllipsis" />
    </div>
  </div>
</template>