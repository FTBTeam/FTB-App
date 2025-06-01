import {ContextMenu, MenuItem, MenuOptions} from '@/core/context/menus/contextMenu';
import appPlatform from '@platform';
import {SugaredInstanceJson} from '@/core/types/javaApi';
import {generalInstancePaths} from "@/core/context/menus/instanceMenu.ts";
import {gobbleError} from "@/utils/helpers/asyncHelpers.ts";
import {sendMessage} from "@/core/websockets/websocketsApi.ts";

export type RunningInstanceMenuContext = {
  instance: SugaredInstanceJson
}

export class RunningInstanceOptionsMenu extends ContextMenu<RunningInstanceMenuContext> {
  name(): String {
    return 'Running Instance Menu';
  }

  options(): MenuOptions<RunningInstanceMenuContext> {
    return [
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
      })) as MenuItem<RunningInstanceMenuContext>[],
      {
        separator: true 
      },
      {
        title: 'Kill instance',
        color: 'danger',
        async action(context) {
          await gobbleError(() => {
            sendMessage("instance.kill", {
              uuid: context.instance.uuid
            })
          });
        },
      }
    ];
  }
}