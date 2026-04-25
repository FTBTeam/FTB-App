import {AlignedPlacement, Side} from "@floating-ui/utils";
import {IconDefinition} from "@fortawesome/free-brands-svg-icons";

export type UiSelectProps<T> = {
  placement?: Side | AlignedPlacement
  label?: string;
  disabled?: boolean;
  fill?: boolean;
  icon?: IconDefinition;
  options: T[];
  placeholder?: string;
  minWidth?: number;
}

// T extends {key: string, value: string}
export type UiSelectOption<T> = T & {
  key: string,
  value: string,
};

export type BasicUiSelectOption = UiSelectOption<{}>;