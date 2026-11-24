<script setup lang="ts">
import {faUser} from "@fortawesome/free-solid-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {computed} from "vue";

const {
  url,
  size = 'small',
  alt = '',
  circle = false
} = defineProps<{
  url?: string;
  size?: 'small' | 'medium' | string,
  alt?: string;
  circle?: boolean;
}>()

const sizeProp = computed(() => {
  if (size === 'medium') {
    return 'w-14 h-14'
  } else if (size === 'small') {
    return 'w-10 h-10'
  } else {
    return size;
  }
})
</script>

<template>
  <div class="flex items-center justify-center overflow-hidden bg-gray-600" :class="{'rounded-full': circle, 'rounded-lg': !circle, [sizeProp]: true}">
    <img v-if="url" :src="url" :alt="alt" >
    <FontAwesomeIcon :icon="faUser" v-else />
  </div>
</template>