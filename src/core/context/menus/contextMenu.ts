import {IconDefinition} from '@fortawesome/free-brands-svg-icons';

export type MenuItemComputed<T> = {
  title: string;
  icon?: IconDefinition;
  color?: 'info' | 'warning' | 'danger' | 'success';
  description?: string;
  action: (context: T) => void;
  children?: MenuOptions<T>
}

export type MenuItem<T> = MenuItemComputed<T> & {
  predicate?: (context: T) => boolean;
}

export type MenuOptions<T> = (MenuItem<T> | {separator: boolean})[]

export abstract class ContextMenu<T> {
  abstract name(): String;

  abstract options(): MenuOptions<T>;
}
  