import mitt, {Emitter} from 'mitt';
import { InstanceInstallController } from '@/core/controllers/InstanceInstallController.ts';
import { WebsocketController } from '@/core/controllers/websocketController.ts';
import { initStateProcessor } from '@/core/controllers/runningStateProcessor.ts';

export type EmitEvents = {
  "ws/connected": void,
  "ws/disconnected": void
  "ws/message": any
}

let ourServices: {
  emitter: Emitter<EmitEvents>,
  websocket: WebsocketController,
  instanceInstallController: InstanceInstallController
};

export function services() {
  if (!ourServices) {
    loadServices()
  }
  
  return ourServices;
}

function loadServices() {
  const emitter = mitt<EmitEvents>();
  const websocketController = new WebsocketController(emitter);

  ourServices = {
    emitter,
    websocket: websocketController,
    instanceInstallController: new InstanceInstallController(emitter)
  }

  initStateProcessor();
}