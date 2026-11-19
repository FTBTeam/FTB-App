import {ContextMenu, MenuOptions} from "@/core/context/menus/contextMenu.ts";
import {
  faArrowsLeftRight,
  faRefresh, faTrash
} from "@fortawesome/free-solid-svg-icons";

export type ProfileMenuContext = {
  profile: {
    uuid: string;
    notLoggedIn?: boolean;
  }
}

export class ProfileMenu extends ContextMenu<ProfileMenuContext> {
  name(): String {
    return 'Profile Menu';
  }

  options(): MenuOptions<ProfileMenuContext> {
    return [
      {
        title: 'Switch to this account',
        icon: faArrowsLeftRight,
        async action(context) {

        }
      },
      {
        title: 'Sign in again',
        icon: faRefresh,
        async action(context) {

        },
        predicate(context) {
          return context.profile.notLoggedIn ?? false;
        }
      },
      {
        title: 'Remove profile',
        icon: faTrash,
        color: 'danger',
        async action(context) {
          
        },
      }
    ];
  }
}