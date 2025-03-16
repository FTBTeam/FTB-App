import { InstanceInstallController } from '@/core/controllers/InstanceInstallController.ts';
import mitt from 'mitt';

const emitter = mitt();

const services = {
  emitter,
  instanceInstallController: new InstanceInstallController(emitter)
}

export function earlyForceLoad() {}

export {
  services
}