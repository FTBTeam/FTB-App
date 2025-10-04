import {ContextMenu, MenuItem, MenuOptions} from '@/core/context/menus/contextMenu';
import {faCog, faCopy, faFolder, faLocationPin, faPlay, faTrash} from '@fortawesome/free-solid-svg-icons';
import appPlatform from '@platform';
import {SugaredInstanceJson} from '@/core/types/javaApi';
import {InstanceActions} from '@/core/actions/instanceActions';
import {InstanceController} from '@/core/controllers/InstanceController';
import {dialog, dialogsController, form} from '@/core/controllers/dialogsController';
import {useAccountsStore} from "@/store/accountsStore.ts";
import {z} from "zod";
import {alertController} from "@/core/controllers/alertController.ts";

export type InstanceMenuContext = {
  instance: SugaredInstanceJson
}

export const generalInstancePaths = [
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

const offlineValidator = z.object({
  username: z.string().min(1, "Username is required")
})

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
        predicate: context => InstanceActions.canStart(context.instance) && !InstanceActions.isUpdating(context.instance) && !InstanceActions.isRunning(context.instance)
      },
      {
        title: 'Play offline',
        icon: faPlay,
        async action(context) {
          const accountStore = useAccountsStore();
          
          new Promise<Record<string, string> | null>((resolve) => {
            dialogsController.createDialog(dialog("Play Offline")
              .withCloseAction(() => resolve(null))
              .withSubTitle("Enter a username to play offline")
              .withForm(form(offlineValidator, values => resolve(values))
                .input("username", "Username", accountStore.mcActiveProfile?.username ?? "Offline Player")
                .build())
              .build());
          // Don't stall up the action as this will cause the context menu to not close
          }).then((result) => {
            if (result === null)  return;
            
            if (!result?.username) {
              alertController.error(`You must enter a username to play offline.`);
              return;
            }
            
            InstanceController
              .from(context.instance)
              .playOffline(result.username)
              .catch(error => alertController.error(`Failed to start instance offline: ${error.message}`));
          })
        },
        predicate: _ => {
          const accountStore = useAccountsStore();
          return !!accountStore.mcActiveProfile;
        }
      },
      {
        title: 'Pin',
        icon: faLocationPin,
        async action(context) {
          await InstanceActions.pin(context.instance, true)
        },
        predicate: context => !context.instance.pinned
      },
      {
        title: 'Unpin',
        icon: faLocationPin,
        async action(context) {
          await InstanceActions.pin(context.instance, false)
        },
        predicate: context => context.instance.pinned
      },
      {
        title: 'Open folder',
        icon: faFolder,
        async action(context) {
          await appPlatform.io.openFinder(context.instance.path);
        },
        children: [
          {
            title: 'Instance folder',
            async action(context) {
              await appPlatform.io.openFinder(context.instance.path);
            },
          },
          ...generalInstancePaths.map((e) => ({
            title: e[1],
            async action(context) {
              await appPlatform.io.openFinder(appPlatform.io.pathJoin(context.instance.path, e[0]));
            },
            predicate: context => context.instance.rootDirs?.includes(e[0])
          })) as MenuItem<InstanceMenuContext>[]
        ]
      },
      {
        separator: true,
      },
      {
        title: 'Duplicate Instance',
        icon: faCopy,
        async action(context) {
          InstanceController.from(context.instance).openDuplicateDialog()
        }
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
          dialogsController.createConfirmationDialog("Are you sure you want to delete this instance?", `Are you absolutely sure you want to delete \`${context.instance.name}\`? This action can not be undone, all your saves, configs, custom mods, etc will be removed.`)
            .then((result) => {
              if (!result) return;
              
              InstanceController.from(context.instance)
                .deleteInstance()
                .catch(console.error)
            })
        }
      }
    ];
  }
}