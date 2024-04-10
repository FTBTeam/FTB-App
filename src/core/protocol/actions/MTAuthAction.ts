import {Action, ActionContext, ActionType} from '../protocolActions';
import {alertController} from '@/core/controllers/alertController';
import {emitter} from '@/utils';

// ftb://mauth/process?app-auth=&token=
export class MTAuthAction implements Action {
  namespace: ActionType = 'mauth';
  action = 'process';

  async run(context: ActionContext) {
    const authToken = context.query.get('app-auth');
    const fullToken = context.query.get('token');

    if (fullToken == null || authToken == null) {
      alertController.error('Failed to login due to missing credentials...');
      return;
    }

    // Send the credentials!
    emitter.emit('mt:auth-callback', {
      token: fullToken,
      appToken: authToken,
    });
  }
}
