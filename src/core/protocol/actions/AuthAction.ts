import { Action, ActionContext, ActionType } from '../protocolActions';
import { wsTimeoutWrapperTyped } from '@/utils';
import store from '@/modules/store';
import platform from '@/utils/interface/electron-overwolf';
import {loginWithMicrosoft} from '@/utils/auth/authentication';

export class AuthAction implements Action {
  namespace: ActionType = 'auth';
  action = 'process';

  async run(context: ActionContext) {
    const credentials = context.query.get('credentials');
    
    if (credentials == null) {
      await store.dispatch('showAlert', {
        type: 'danger',
        title: 'Unable to login',
        message: `Failed to login due to missing credentials...`,
      });
      return;
    }
    
    const result = await loginWithMicrosoft(credentials);
    if (result.success) {
      await store.dispatch('core/loadProfiles');
      await platform.get.actions.closeAuthWindow(true);
      return;
    }
      
    await platform.get.actions.closeAuthWindow(false);
    await store.dispatch('showAlert', {
      type: 'danger',
      title: 'Unable to login',
      message: `Failed to login due to: ${result.response}`,
    });
  }
}
