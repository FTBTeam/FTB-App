import {ContextMenu, MenuOptions} from '@/core/context/menus/contextMenu';
import {faArrowRotateRight, faDatabase, faFileZipper, faFolder} from '@fortawesome/free-solid-svg-icons';
import platform from '@/utils/interface/electron-overwolf';
import store from '@/modules/store';
import {InstanceActions} from '@/core/actions/instanceActions';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {alertController} from '@/core/controllers/alertController';

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
          await platform.get.io.openFinder(platform.get.io.appHome());
        }
      },
      {
        title: "Open instance folder",
        icon: faFolder,
        async action() {
          const settings = store.state.settings?.settings.instanceLocation;
          if (settings) {
            await platform.get.io.openFinder(settings);
          }
        }
      },
      {
        title: "Export app logs",
        icon: faFileZipper,
        async action() {
          const result = await sendMessage("uploadLogs", {});
          if (result.path) {
            await platform.get.io.openFinder(result.path);
            alertController.success("Logs exported successfully, you can find them at " + result.path);
          } else {
            console.log(result);
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
          platform.get.actions.restartApp();
        },
        color: "danger"
      }
    ];
  }
}