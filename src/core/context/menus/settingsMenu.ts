import {ContextMenu, MenuOptions} from '@/core/context/menus/contextMenu';
import {faArrowRotateRight, faDatabase, faFolder} from '@fortawesome/free-solid-svg-icons';
import platform from '@/utils/interface/electron-overwolf';
import store from '@/modules/store';
import {InstanceActions} from '@/core/actions/instanceActions';

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