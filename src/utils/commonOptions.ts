import {SelectionOptions} from '@/components/ui/Selection2.vue';

export const ReleaseChannelOptions = (includeUnset = false) => [
  {
    value: 'release',
    label: 'Release',
    meta: '⭐️',
  },
  {
    value: 'beta',
    label: 'Beta',
    meta: 'β',
  },
  {
    value: 'alpha',
    label: 'Alpha',
    meta: '⚠️',
  },
  ...(includeUnset ? [{
    value: 'unset',
    label: 'Use app default',
  }] : []),
] as SelectionOptions;