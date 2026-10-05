import { Action, ActionContext, ActionType } from '../protocolActions';
import {InstanceActions} from "@/core/actions/instanceActions.ts";
import {useInstanceStore} from "@/store/instancesStore.ts";
import {alertController} from "@/core/controllers/alertController.ts";

export class StartInstanceAction implements Action {
  namespace: ActionType = 'instance';
  action = 'start';

  async run(context: ActionContext) {
    const packUuid = context.query.get('uuid') || context.args[0];

    if (packUuid == null) {
      return;
    }

    const instances = useInstanceStore();
    const instance = instances.instances.find(e => e.uuid === packUuid);
    if (!instance) {
      console.error(`No instance found with uuid ${packUuid}`);
      return;
    }
    
    InstanceActions.start(instance)
      .catch(error => {
        console.error(`Failed to start instance ${packUuid}`, error);
        alertController.error(`Failed to start instance ${instance.name}: ${error.message || error}`);
      })
  }
}
