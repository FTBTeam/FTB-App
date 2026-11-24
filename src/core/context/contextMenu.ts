import {shallowRef} from 'vue';
import {IconDefinition} from '@fortawesome/fontawesome-svg-core';
import {createLogger} from '@/core/logger.ts';

const logger = createLogger("contextMenu.ts");

export type MenuItem = {
  title: string;
  icon?: IconDefinition;
  color?: 'info' | 'warning' | 'danger' | 'success';
  description?: string;
  /** Left out of the menu. Worked out when the menu opens. */
  hidden?: boolean;
  /** Runs after the menu closes, so a slow action or a dialog it opens doesn't hold the menu open */
  action?: () => void | Promise<void>;
  children?: MenuOptions;
}

export type MenuSeparator = { separator: true };

export type MenuOptions = (MenuItem | MenuSeparator)[];

/** Where to open: at the pointer, beside an element, or at a point in the window */
export type MenuAnchor = MouseEvent | Element | { x: number; y: number };

export type OpenContextMenu = {
  items: MenuOptions;
  pos: { x: number; y: number };
}

/** The open menu, rendered by `ContentMenuGlobal`. Only one can be open at a time. */
export const openedContextMenu = shallowRef<OpenContextMenu | null>(null);

/**
 * Opens a context menu. Build the items where the menu is opened, so actions can use that component's own state and
 * functions directly.
 */
export function openContextMenu(at: MenuAnchor, items: MenuOptions) {
  openedContextMenu.value = {
    items,
    pos: position(at)
  };
}

export function closeContextMenu() {
  openedContextMenu.value = null;
}

/** Closes the menu, then runs the item's action */
export function runMenuItem(item: MenuItem) {
  closeContextMenu();
  if (!item.action) {
    return;
  }

  Promise.resolve()
    .then(() => item.action!())
    .catch((e) => logger.error(`Context menu action "${item.title}" failed`, e));
}

export function isSeparator(option: MenuItem | MenuSeparator): option is MenuSeparator {
  return 'separator' in option;
}

function position(at: MenuAnchor): { x: number; y: number } {
  if (at instanceof MouseEvent) {
    return {x: at.clientX + 5, y: at.clientY + 5};
  }

  if (at instanceof Element) {
    const rect = at.getBoundingClientRect();
    return {x: rect.right + 5, y: rect.top};
  }

  return at;
}
