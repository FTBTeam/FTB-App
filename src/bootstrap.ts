import mitt from 'mitt';
import { InstanceInstallController } from '@/core/controllers/InstanceInstallController.ts';
import { WebsocketController } from '@/core/controllers/websocketController.ts';

export type EmitEvents = {
  "ws/connected": void,
  "ws/disconnected": void
  "ws/message": string
}

const emitter = mitt<EmitEvents>();
const websocketController = new WebsocketController(emitter);

const services = {
  emitter,
  websocket: websocketController,
  instanceInstallController: new InstanceInstallController(emitter, websocketController)
}

export function bootstrapLoad() {}

export {
  services
}