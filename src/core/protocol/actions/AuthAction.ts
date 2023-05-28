import { Action, ActionContext, ActionType } from '../protocolActions';
import store from '@/modules/store';
import platform from '@/utils/interface/electron-overwolf';

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
      
      // Failed
      await platform.get.actions.emitAuthenticationUpdate();
      return;
    }

    // Send the credentials!
    await platform.get.actions.emitAuthenticationUpdate(credentials);
  }
}
