import {MenuItem, MenuOptions} from '@/core/context/contextMenu';
import appPlatform from '@platform';
import {SugaredInstanceJson} from '@/core/types/javaApi';
import {generalInstancePaths} from "@/core/context/menus/instanceMenu.ts";
import {gobbleError} from "@/utils/helpers/asyncHelpers.ts";
import {sendMessage} from "@/core/websockets/websocketsApi.ts";
import {useRunningInstancesStore} from "@/store/runningInstancesStore.ts";

export function runningInstanceMenu(instance: SugaredInstanceJson): MenuOptions {
  const runningInstances = useRunningInstancesStore();

  return [
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
    })),
    {
      title: 'Kill instance',
      color: 'danger',
      async action() {
        await gobbleError(() => {
          sendMessage("instance.kill", {
            uuid: instance.uuid
          })
        });
      },
      hidden: !runningInstances.instances.some(i => i.uuid === instance.uuid)
    }
  ];
}
