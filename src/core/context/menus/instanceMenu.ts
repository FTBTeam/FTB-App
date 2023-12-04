import {ContextMenu, MenuItem, MenuOptions} from '@/core/context/menus/contextMenu';
import {faCog, faFolder, faPlay, faTrash} from '@fortawesome/free-solid-svg-icons';
import platform from '@/utils/interface/electron-overwolf';
import {SugaredInstanceJson} from '@/core/@types/javaApi';
import {InstanceActions} from '@/core/actions/instanceActions';
import {InstanceController} from '@/core/controllers/InstanceController';
import {dialogsController} from '@/core/controllers/dialogsController';

export type InstanceMenuContext = {
  instance: SugaredInstanceJson
}

const paths = [
  ["logs", "Logs"],
  ["crashlogs", "Crash logs"],
  ["mods", "Mods"],
  ["saves", "Worlds"],
  ["config", "Configs"],
  ["screenshots", "Screenshots"],
  ["resourcepacks", "Resource Packs"],
  ["shaderpacks", "Shader Packs"],
  ["backups", "Backups"],
  ["kubejs", "KubeJS"],
  ["scripts", "Scripts"],
]

let folderOptions: MenuItem<InstanceMenuContext>[] = []

folderOptions.push({
  title: 'Instance folder',
  async action(context) {
    await platform.get.io.openFinder(context.instance.path);
  },
})

folderOptions = [...folderOptions, ...paths.map((e) => ({
  title: e[1],
  async action(context) {
    await platform.get.io.openFinder(context.instance.path + "/" + e[0]);
  },
  predicate: context => context.instance.rootDirs?.includes(e[0])
})) as MenuItem<InstanceMenuContext>[]]

export class InstanceMenu extends ContextMenu<InstanceMenuContext> {
  name(): String {
    return 'Instance Menu';
  }

  options(): MenuOptions<InstanceMenuContext> {
    return [
      {
        title: 'Play',
        icon: faPlay,
        async action(context) {
          await InstanceActions.start(context.instance)
        },
        predicate: context => InstanceActions.canStart(context.instance) && !InstanceActions.isUpdating(context.instance)
      },
      {
        title: 'Open folder',
        icon: faFolder,
        async action(context) {
          await platform.get.io.openFinder(context.instance.path);
        },
        children: folderOptions
      },
      {
        title: 'Settings',
        icon: faCog,
        async action(context) {
          await InstanceActions.openSettings(context.instance)
        }
      },
      {
        separator: true,
      },
      {
        title: 'Delete Instance',
        icon: faTrash,
        color: 'danger',
        async action(context) {
          if (!(await dialogsController.createConfirmationDialog("Are you sure you want to delete this instance?", `Are you absolutely sure you want to delete \`${context.instance.name}\`? This action can not be undone, all your saves, configs, custom mods, etc will be removed.`))) return;
          await InstanceController.from(context.instance).deleteInstance();
        }
      }
    ];
  }
}