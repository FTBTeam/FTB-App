import {ContextMenu, MenuOptions} from '@/core/context/menus/contextMenu';
import {faArrowRotateRight, faDownload, faFolder} from '@fortawesome/free-solid-svg-icons';
import {SugaredInstanceJson} from "@/core/types/javaApi";
import platform from "@platform";

type Context = {
  instance: SugaredInstanceJson,
  hasModUpdates: boolean,
  actions: {
    refreshMods: () => void,
    updateAll: () => void
  }
}

export class ModpackModsMenu extends ContextMenu<Context> {
  name(): String {
    return 'Modpack Mods Menu';
  }

  options(): MenuOptions<Context> {
    return [
      {
        title: 'Open Mods folder',
        icon: faFolder,
        async action(context) {
          const { instance} = context;
          if (instance.rootDirs.includes("mods")) {
            await platform.io.openFinder(platform.io.pathJoin(instance.path, "mods"));
          } else {
            await platform.io.openFinder(instance.path);
          }
        }
      },
      {
        title: 'Refresh Mods list',
        icon: faArrowRotateRight,
        async action(context) {
          context.actions.refreshMods();
        }
      },
      {
        title: 'Update All Mods',
        icon: faDownload,
        async action(context) {
          context.actions.updateAll();
        },
        predicate(context) {
          return !context.instance.locked && context.hasModUpdates
        }
      }
    ];
  }
}