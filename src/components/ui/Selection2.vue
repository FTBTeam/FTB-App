<script lang="ts" setup>
import {createLogger} from '@/core/logger';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { computed, onMounted, onUnmounted, ref, useTemplateRef } from 'vue';
import { faChevronDown } from '@fortawesome/free-solid-svg-icons';
import { IconDefinition } from '@fortawesome/free-brands-svg-icons';

export type SelectionOptions = SelectionOption[];
export type SelectionOption = {
  label: string;
  value: any;
  badge?: {
    color?: string;
    text: string;
    icon?: IconDefinition
  };
  meta?: any;
};

const logger = createLogger(`Selection2.vue`);
const {
  label = '',
  icon = null,
  direction = 'left',
  minWidth = 0,
  placeholder = 'Select option',
  options = [],
  badgeEnd = false,
  allowDeselect = false,
  openUp = false,
  disabled = false,
} = defineProps<{
  label?: string;
  icon?: IconDefinition | null;
  direction?: 'left' | 'right';
  minWidth?: number;
  placeholder?: string;
  options?: SelectionOptions;
  badgeEnd?: boolean;
  allowDeselect?: boolean;
  openUp?: boolean;
  disabled?: boolean;
}>()

const value = defineModel<string | number | null>({
  default: null
})

const emit = defineEmits<{
  (e: 'updated', value: string | null): void
}>()

const open = ref(false);
const selectionRef = useTemplateRef('selection')
const id = Math.random().toString(36).substring(2, 9);

onMounted(() => {
  if (options.length) {
    // Check for duplicate keys and warn
    const keys = options.map(o => o.value);
    const uniqueKeys = new Set(keys);
    if (keys.length !== uniqueKeys.size) {
      logger.warn("Duplicate keys detected in selection options for: " + label);
    }
  }

  document.addEventListener('click', handleDocumentClick);
})

onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick);
})

const selected = computed(() => {
  return (options ?? []).find(o => o.value === value.value) ?? null
});

function handleDocumentClick(event: any) {
  // Check if the click was inside the selection box and that selection box has the same id as this one
  const closest = event.target.closest('.selection');
  if (closest && closest.id === id) {
    return;
  }

  open.value = false;
}

function select(option: SelectionOption) {
  if (option.value === value) {
    if (allowDeselect) {
      value.value = null;
      emit('updated', null)
    }
    
    open.value = false;
    selectionRef.value?.blur();
    return;
  }
  
  value.value = option.value;
  emit('updated', option.value)
  open.value = false;
  selectionRef.value?.blur();
}
</script>

<template>
  <div class="select-box" :class="{disabled}">
    <div class="label" v-if="label && !icon">{{ label }}</div>
    <div class="selection" :class="{ open }" ref="selection" :id="id" @click="open = !open">
      <div class="main" v-if="!icon">
        <div class="item with-empty" v-if="!selected">
          <div class="badge empty">_</div>
          {{ placeholder }}
        </div>
        <div class="item" v-else>
          <div class="badge" :style="{ backgroundColor: selected.badge.color }" v-if="selected.badge">
            {{ selected.badge.text }}
          </div>
          <div v-else class="badge empty">_</div>
          <div class="text">{{ selected.label }}</div>
          <div class="meta" v-if="selected.meta">{{ selected.meta }}</div>
        </div>

        <FontAwesomeIcon class="arrow" :icon="faChevronDown" />
      </div>
      
      <div class="main-icon" v-else>
        <FontAwesomeIcon :fixed-width="true" :icon="icon" />
      </div>

      <div class="dropdown" @click.stop :class="{'open-up': openUp, [direction]: true}" :style="{width: (minWidth === 0 ? undefined : minWidth + 'px')}">
        <div class="item list-item" :class="{'no-badge': !option.badge}" v-for="(option, index) in options" :key="index" @click="() => select(option)">
          <div v-if="!badgeEnd && option.badge" class="badge" :style="{ backgroundColor: option.badge.color }">
            <FontAwesomeIcon v-if="option.badge.icon" :icon="option.badge.icon" class="mr-1" />
            {{ option.badge.text }}
          </div>
          <div class="text">{{ option.label }}</div>
          <div class="meta" v-if="option.meta">{{ option.meta }}</div>
          <!-- Eww -->
          <div v-if="badgeEnd && option.badge" class="badge end" :style="{ backgroundColor: option.badge.color }">
            {{ option.badge.text }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use "sass:color";

.select-box {
  &.disabled {
    opacity: 0.5;
    pointer-events: none;
  }
  
  .label {
    margin-bottom: 0.5rem;
  }

  .selection {
    background: #252525;
    border-radius: 5px;
    padding: 0.55rem;
    cursor: pointer;
    position: relative;
    border: 1px solid #1b1b1b;

    .main {
      padding-right: 2rem;
    }

    .arrow {
      position: absolute;
      top: 50%;
      transform: translateY(-50%);
      right: 1rem;
      transition: transform 0.25s ease-in-out;
    }

    &.open {
      .dropdown {
        top: 120%;
        opacity: 1;
        visibility: visible;
        pointer-events: auto;
      }

      .arrow {
        transform: translateY(-50%) rotateZ(180deg);
      }
    }

    .dropdown {
      // prevent weirdness
      pointer-events: none;
      cursor: default;
      position: absolute;
      top: 150%;
      left: 0;
      background: var(--color-background);
      width: 100%;
      border-radius: 5px;
      box-shadow: 0 5px 0.5rem rgba(black, 0.2);
      border: 1px solid color.adjust(black, $lightness: 10%);
      z-index: 1000;
      padding: 1rem;
      max-height: 240px;
      overflow-y: auto;

      visibility: hidden;
      opacity: 0;

      transition: visibility 0.25s ease-in-out, opacity 0.25s ease-in-out, top 0.25s ease-in-out;

      &.right {
        right: 0;
        left: unset;
      }

      &.open-up {
        top: auto;
        bottom: 120%;
      }
    }

    .item {
      display: flex;
      align-items: center;
      cursor: pointer;

      &.list-item:not(:last-child) {
        margin-bottom: 1rem;
      }

      &.list-item {
        position: relative;

        &::after {
          position: absolute;
          content: '';
          top: -20%;
          left: -1.5%;
          width: 103%;
          height: 140%;
          background-color: var(--color-navbar);
          z-index: -1;
          border-radius: 5px;
          visibility: hidden;
          opacity: 0;
          transition: visibility 0.25s ease-in-out, opacity 0.25s ease-in-out;
        }

        &:hover::after {
          visibility: visible;
          opacity: 1;
        }
      }

      &.no-badge {
        padding: 0.1rem 0;
      }

      .badge {
        padding: 0.1rem 0.5rem;
        border-radius: 3px;
        font-size: 0.875rem;
        margin-right: 1rem;

        &.end {
          margin-right: 0;
          margin-left: 1rem;
        }

        &.empty {
          width: 0;
          padding: 0.2rem 0;
          margin-right: 0;
          background: transparent !important;
          visibility: hidden;
        }
      }

      .text {
        flex: 1;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-weight: bold;
      }

      .meta {
        margin-left: 1rem;
        opacity: .8;
      }
    }
  }
}
</style>
