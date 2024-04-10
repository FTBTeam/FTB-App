import {Action, ActionContext, ActionType} from '../protocolActions';
import {alertController} from '@/core/controllers/alertController';
import {emitter} from '@/utils';

export class AuthAction implements Action {
  namespace: ActionType = 'auth';
  action = 'process';

  async run(context: ActionContext) {
    const credentials = context.query.get('credentials');
    
    if (credentials == null) {
      alertController.error('Failed to login due to missing credentials...')
      
      // Failed
      emitter.emit("authentication.callback")
      return;
    }

    // Send the credentials!
    emitter.emit("authentication.callback", credentials)
  }
}
