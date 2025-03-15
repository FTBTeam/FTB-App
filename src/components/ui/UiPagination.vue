<template>
 <div class="ui-pagination">
    <ul class="flex gap-2">
      <li v-if="currentPage > 1" @click="input(currentPage - 1)" aria-label="Previous page" data-balloon-pos="up-left">
        <font-awesome-icon icon="chevron-left" />
      </li>

      <template v-if="currentPage > 3">
        <li @click="input(1)" aria-label="First page" data-balloon-pos="up-left">1</li>
        <li class="disabled">...</li>
      </template>
      
      <li v-if="currentPage - 2 > 0" @click="input(currentPage - 2)">{{ currentPage - 2 }}</li>
      <li v-if="currentPage - 1 > 0" @click="input(currentPage - 1)">{{ currentPage - 1 }}</li>

      <li class="active">{{ currentPage }}</li>
      
      <li v-if="currentPage + 1 <= pages" @click="input(currentPage + 1)">{{ currentPage + 1 }}</li>
      <li v-if="currentPage + 2 <= pages" @click="input(currentPage + 2)">{{ currentPage + 2 }}</li>
      
      <template v-if="currentPage + 2 < pages">
        <li class="disabled">...</li>
        <li @click="input(pages)" aria-label="Last page" data-balloon-pos="up-right">{{ pages }}</li>
      </template>
      
      <li v-if="currentPage < pages" @click="input(currentPage + 1)" aria-label="Next page" data-balloon-pos="up-right">
        <font-awesome-icon icon="chevron-right" />
      </li>
    </ul>
 </div>
</template>

<script lang="ts">
import {Component, Emit, Prop, Vue} from 'vue-property-decorator';

@Component
export default class UiPagination extends Vue {
  @Prop() value!: number;
  @Prop() total!: number;
  @Prop() perPage!: number;
  
  @Emit() input(value: number) {}
  
  get pages() {
    return Math.ceil(this.total / this.perPage);
  }
  
  get currentPage() {
    return this.value;
  }
}
</script>

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