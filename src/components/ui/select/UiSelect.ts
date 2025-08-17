// T extends {key: string, value: string}
export type UiSelectOption<T> = T & {
  key: string,
  value: string,
};

export type BasicUiSelectOption = UiSelectOption<{}>;