import {UiSelectOption} from "@/components/ui/select/UiSelect.ts";

export const ReleaseChannelOptions = (includeUnset = false) => [
  {
    key: 'release',
    value: 'Release',
    icon: '⭐'
  },
  {
    key: 'beta',
    value: 'Beta',
    icon: 'β'
  },
  {
    key: 'alpha',
    value: 'Alpha',
    icon: '⚠'
  },
  ...(includeUnset ? [{
    key: 'unset',
    value: 'Use app default',
  }] : []),
] as UiSelectOption<{ icon: string }>[];