import {ContextMenu, InstanceMenu, SettingMenu} from '@/core/context/menus';
import {ContextMenus} from '@/core/context/contextMenus';
import {emitter} from '@/utils';

class ContextController {
  private readonly registry: Map<string, ContextMenu<any>> = new Map();

  constructor() {
    this.register(ContextMenus.INSTANCE_MENU, new InstanceMenu());
    this.register(ContextMenus.NAV_SETTINGS_MENU, new SettingMenu());
  }

  private register<T>(name: ContextMenus, menu: ContextMenu<T>) {
    if (this.registry.has(name)) {
      throw new Error(`Context Controller already has a menu with the name of ${name}`)
    }

    this.registry.set(name, menu);
  }

  /**
   * TODO: strengthen the type of context
   */
  openMenu<T>(name: ContextMenus, pointer: PointerEvent | MouseEvent, context: () => T) {
    const menu = this.registry.get(name);
    
    if (!menu) {
      throw new Error(`Attempted to a menu that does not exist ${name}`)
    }

    emitter.emit("context-menu-open", {
      pointer: pointer,
      context: context,
      menu: menu
    })
  }
}

export const AppContextController = new ContextController();