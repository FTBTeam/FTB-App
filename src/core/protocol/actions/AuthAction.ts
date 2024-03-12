import { Action, ActionContext, ActionType } from '../protocolActions';
import store from '@/modules/store';
import platform from '@/utils/interface/electron-overwolf';
import {alertController} from '@/core/controllers/alertController';

export class AuthAction implements Action {
  namespace: ActionType = 'auth';
  action = 'process';

  async run(context: ActionContext) {
    const credentials = context.query.get('credentials');
    
    if (credentials == null) {
      alertController.error('Failed to login due to missing credentials...')
      
      // Failed
      platform.get.actions.emitAuthenticationUpdate();
      return;
    }

    // Send the credentials!
    platform.get.actions.emitAuthenticationUpdate(credentials);
  }
}
