import {MenuItem, MenuOptions} from '@/core/context/contextMenu';
import {faCode, faCog, faCopy, faFolder, faLocationPin, faPlay, faTrash} from '@fortawesome/free-solid-svg-icons';
import appPlatform from '@platform';
import {SugaredInstanceJson} from '@/core/types/javaApi';
import {InstanceActions} from '@/core/actions/instanceActions';
import {InstanceController} from '@/core/controllers/InstanceController';
import {dialog, dialogsController, form} from '@/core/controllers/dialogsController';
import {useAccountsStore} from "@/store/accountsStore.ts";
import {z} from "zod";
import {alertController} from "@/core/controllers/alertController.ts";
import Router, {RouterNames} from "@/router.ts";

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

export function instanceMenu(instance: SugaredInstanceJson): MenuOptions {
  const accountStore = useAccountsStore();

  return [
    {
      title: 'Play',
      icon: faPlay,
      async action() {
        await InstanceActions.start(instance)
      },
      hidden: !(InstanceActions.canStart(instance) && !InstanceActions.isUpdating(instance) && !InstanceActions.isRunning(instance))
    },
    {
      title: 'Play offline',
      icon: faPlay,
      async action() {
        new Promise<Record<string, string> | null>((resolve) => {
          dialogsController.createDialog(dialog("Play Offline")
            .withCloseAction(() => resolve(null))
            .withSubTitle("Enter a username to play offline")
            .withForm(form(offlineValidator, values => resolve(values))
              .input("username", "Username", accountStore.mcActiveProfile?.username ?? "Offline Player")
              .build())
            .build());
                  }).then((result) => {
          if (result === null)  return;
          
          if (!result?.username) {
            alertController.error(`You must enter a username to play offline.`);
            return;
          }
          
          InstanceController
            .from(instance)
            .playOffline(result.username)
            .catch(error => alertController.error(`Failed to start instance offline: ${error.message}`));
        })
      },
      hidden: !accountStore.mcActiveProfile
    },
    {
      title: 'Pin',
      icon: faLocationPin,
      async action() {
        await InstanceActions.pin(instance, true)
      },
      hidden: instance.pinned
    },
    {
      title: 'Unpin',
      icon: faLocationPin,
      async action() {
        await InstanceActions.pin(instance, false)
      },
      hidden: !instance.pinned
    },
    {
      title: 'Open folder',
      icon: faFolder,
      async action() {
        await appPlatform.io.openFinder(instance.path);
      },
      children: [
        {
          title: 'Instance folder',
          async action() {
            await appPlatform.io.openFinder(instance.path);
          },
        },
        ...generalInstancePaths.map(([dir, title]): MenuItem => ({
          title,
          async action() {
            await appPlatform.io.openFinder(appPlatform.io.pathJoin(instance.path, dir));
          },
          hidden: !instance.rootDirs?.includes(dir)
        }))
      ]
    },
    {
      separator: true,
    },
    {
      title: 'Duplicate Instance',
      icon: faCopy,
      async action() {
        InstanceController.from(instance).openDuplicateDialog()
      }
    },
    {
      title: 'Settings',
      icon: faCog,
      async action() {
        await InstanceActions.openSettings(instance)
      }
    },
    {
      separator: true,
    },
    {
      title: 'Delete Instance',
      icon: faTrash,
      color: 'danger',
      async action() {
        dialogsController.createConfirmationDialog("Are you sure you want to delete this instance?", `Are you absolutely sure you want to delete \`${instance.name}\`? This action can not be undone, all your saves, configs, custom mods, etc will be removed.`)
          .then((result) => {
            if (!result) return;

            Router.push({name: RouterNames.ROOT_LIBRARY})
            InstanceController.from(instance)
              .deleteInstance()
              .catch(console.error)
          })
      }
    },
    {
      separator: true,
    },
    {
      title: 'Copy ID',
      icon: faCode,
      async action() {
        appPlatform.cb.copy(instance.uuid);
        alertController.success(`Copied instance ID`);
      }
    }
  ];
}
