import {ContextMenu, InstanceMenu, RunningInstanceOptionsMenu, SettingMenu} from '@/core/context/menus';
import {ContextMenus} from '@/core/context/contextMenus';
import {useAppStore} from '@/store/appStore.ts';
import {ModpackModsMenu} from "@/core/context/menus/modpackModsMenu.ts";
import {ProfileMenu} from "@/core/context/menus/profileMenu.ts";

class ContextController {
  private readonly registry: Map<string, ContextMenu<any>> = new Map();

  constructor() {
    this.register(ContextMenus.INSTANCE_MENU, new InstanceMenu());
    this.register(ContextMenus.NAV_SETTINGS_MENU, new SettingMenu());
    this.register(ContextMenus.RUNNING_INSTANCE_OPTIONS_MENU, new RunningInstanceOptionsMenu())
    this.register(ContextMenus.MODPACK_MODS_MENU, new ModpackModsMenu());
    this.register(ContextMenus.PROFILE_MENU, new ProfileMenu())
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
  openMenu<T>(name: ContextMenus, pointer: PointerEvent | MouseEvent | {x: number, y: number}, context: () => T) {
    const menu = this.registry.get(name);
    if (!menu) {
      throw new Error(`Attempted to a menu that does not exist ${name}`)
    }

    const appStore = useAppStore();
    appStore.emitter.emit("action/context-menu-open", {
      pos: {
        x: 'clientX' in pointer ? pointer.clientX + 5 : pointer.x,
        y: 'clientY' in pointer ? pointer.clientY + 5 : pointer.y
      },
      context: context,
      menu: menu
    })
  }
}

export const AppContextController = new ContextController();