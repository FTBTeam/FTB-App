<template>
  <div class="flex flex-col">
    <div class="mb-2 font-bold">{{title}}</div>
    <p class="mb-4 flex-1">{{desc}}</p>
    
    <ui-button :icon="faRefresh" type="warning" fullWidth @click="emit('clicked')" :working="working">
      {{buttonTitle}}
    </ui-button>
  </div>
</template>

<script setup lang="ts">
import UiButton from "../../ui/UiButton.vue";
import {faRefresh} from "@fortawesome/free-solid-svg-icons";

const {
  title,
  buttonTitle,
  desc,
  working
} = defineProps<{
  title: string;
  buttonTitle: string;
  desc: string;
  working: boolean;
}>();

const emit = defineEmits<{
  (e: 'clicked'): void
}>()
</script>
