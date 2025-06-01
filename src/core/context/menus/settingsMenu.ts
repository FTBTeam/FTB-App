import {ContextMenu, MenuOptions} from '@/core/context/menus/contextMenu';
import {faArrowRotateRight, faDatabase, faFileZipper, faFolder} from '@fortawesome/free-solid-svg-icons';
import {InstanceActions} from '@/core/actions/instanceActions';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {alertController} from '@/core/controllers/alertController';
import appPlatform from "@platform"
import { useAppSettings } from '@/store/appSettingsStore.ts';
import { createLogger } from '@/core/logger.ts';

const logger = createLogger("settingsMenu.ts")

export class SettingMenu extends ContextMenu<{ }> {
  name(): String {
    return 'Setting Menu';
  }

  options(): MenuOptions<{}> {
    return [
      {
        title: 'Open App folder',
        icon: faFolder,
        async action() {
          await appPlatform.io.openFinder(appPlatform.io.appHome());
        }
      },
      {
        title: "Open instance folder",
        icon: faFolder,
        async action() {
          const appSettings = useAppSettings();
          const settings = appSettings.rootSettings?.instanceLocation;
          if (settings) {
            await appPlatform.io.openFinder(settings);
          }
        }
      },
      {
        title: "Export app logs",
        icon: faFileZipper,
        async action() {
          const result = await sendMessage("uploadLogs", {});
          if (result.path) {
            await appPlatform.io.openFinder(result.path);
            alertController.success("Logs exported successfully, you can find them at " + result.path);
          } else {
            logger.log(result);
            alertController.error("Failed to export logs, please let us know in our Discord / Github");
          }
        }
      },
      {
        separator: true
      },
      {
        title: 'Refresh cache',
        icon: faDatabase,
        async action() {
          await InstanceActions.clearInstanceCache()
        },
        color: "warning"
      },
      {
        title: 'Restart app',
        icon: faArrowRotateRight,
        action() {
          appPlatform.actions.restartApp();
        },
        color: "danger"
      }
    ];
  }
}