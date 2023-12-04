import {IconDefinition} from '@fortawesome/free-brands-svg-icons';

export type MenuItem<T> = {
  title: string;
  icon?: IconDefinition;
  color?: 'info' | 'warning' | 'danger' | 'success';
  description?: string;
  action: (context: T) => void;
  predicate?: (context: T) => boolean;
  children?: MenuOptions<T>
}

export type MenuOptions<T> = (MenuItem<T> | {separator: boolean})[]

export abstract class ContextMenu<T> {
  abstract name(): String;

  abstract options(): MenuOptions<T>;
}
  