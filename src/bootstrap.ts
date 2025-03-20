import mitt, {Emitter} from 'mitt';
import { InstanceInstallController } from '@/core/controllers/InstanceInstallController.ts';
import { WebsocketController } from '@/core/controllers/websocketController.ts';
import { initStateProcessor } from '@/core/state/misc/runningStateProcessor.ts';

export type EmitEvents = {
  "ws/connected": void,
  "ws/disconnected": void
  "ws/message": any
}

let services: {
  emitter: Emitter<EmitEvents>,
  websocket: WebsocketController,
  instanceInstallController: InstanceInstallController
};

export function bootstrapLoad() {
  const emitter = mitt<EmitEvents>();
  const websocketController = new WebsocketController(emitter);
  
  services = {
    emitter,
    websocket: websocketController,
    instanceInstallController: new InstanceInstallController(emitter, websocketController)
  }

  initStateProcessor();
}

export {
  services
}