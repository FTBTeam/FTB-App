import { Action, ActionContext, ActionType } from '../protocolActions';
import { wsTimeoutWrapperTyped } from '@/utils';
import store from '@/modules/store';
import platform from '@/utils/interface/electron-overwolf';

export class AuthAction implements Action {
  namespace: ActionType = 'auth';
  action = 'process';

  async run(context: ActionContext) {
    // const res = await platform.get.actions.openMsAuth();

    const credentials = context.query.get('credentials');
    
    const responseRaw: any = await fetch('https://msauth.feed-the-beast.com/v1/retrieve', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        credentials,
      }),
    });

    const response: any = (await responseRaw.json()).data;
    console.log(response);

    if (!response || !response.liveAccessToken || !response.liveRefreshToken || !response.liveExpires) {
      // this.$emit('error', 'Failed to retrieve essential information, please try again.');
      return;
    }

    const res = await wsTimeoutWrapperTyped<any, { success: boolean }>({
      type: 'profiles.ms.authenticate',
      ...response,
    });

    if (res.success) {
      await store.dispatch('core/loadProfiles');
      await platform.get.actions.closeAuthWindow(true);
    }
  }
}
