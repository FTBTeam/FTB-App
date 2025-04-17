import { InstanceInstallController } from '@/core/controllers/InstanceInstallController.ts';
import { initStateProcessor } from '@/core/controllers/runningStateProcessor.ts';

let ourServices: {
  instanceInstallController: InstanceInstallController
};

export function services() {
  if (!ourServices) {
    loadServices()
  }
  
  return ourServices;
}

function loadServices() {
  ourServices = {
    instanceInstallController: new InstanceInstallController()
  }

  initStateProcessor();
}