<script lang="ts" setup>
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { computed } from 'vue';
import { faChevronLeft, faChevronRight } from '@fortawesome/free-solid-svg-icons';

const { total, perPage } = defineProps<{
  total: number;
  perPage: number;
}>();

const value = defineModel<number>()

const pages = computed(() => Math.ceil(total / perPage));
const currentPage = value.value ?? 0

</script>

<template>
 <div class="ui-pagination">
    <ul class="flex gap-2">
      <li v-if="currentPage > 1" @click="value = currentPage - 1" aria-label="Previous page" data-balloon-pos="up-left">
        <FontAwesomeIcon :icon="faChevronLeft" />
      </li>

      <template v-if="currentPage > 3">
        <li @click="value = 1" aria-label="First page" data-balloon-pos="up-left">1</li>
        <li class="disabled">...</li>
      </template>
      
      <li v-if="currentPage - 2 > 0" @click="value = currentPage - 2">{{ currentPage - 2 }}</li>
      <li v-if="currentPage - 1 > 0" @click="value = currentPage - 1">{{ currentPage - 1 }}</li>

      <li class="active">{{ currentPage }}</li>
      
      <li v-if="currentPage + 1 <= pages" @click="value = currentPage + 1">{{ currentPage + 1 }}</li>
      <li v-if="currentPage + 2 <= pages" @click="value = currentPage + 2">{{ currentPage + 2 }}</li>
      
      <template v-if="currentPage + 2 < pages">
        <li class="disabled">...</li>
        <li @click="value = pages" aria-label="Last page" data-balloon-pos="up-right">{{ pages }}</li>
      </template>
      
      <li v-if="currentPage < pages" @click="value = currentPage + 1" aria-label="Next page" data-balloon-pos="up-right">
        <FontAwesomeIcon :icon="faChevronRight" />
      </li>
    </ul>
 </div>
</template>

<style lang="scss" scoped>
.ui-pagination {
  li {
    padding: .4rem .8rem;
    background-color: var(--color-background-lighten);
    border-radius: 3px;
    cursor: pointer;
    
    &:hover {
      background-color: var(--color-info-button);
    }
    
    &.active {
      background-color: var(--color-success-button);
      cursor: default;
    }
    
    &.disabled {
      cursor: default;
      background-color: transparent;
      opacity: 0.5;
    }
  }
}
</style>