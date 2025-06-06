import { defineStore } from 'pinia';
import mitt, {type Emitter} from 'mitt';
import { InstanceInstallController } from '@/core/controllers/InstanceInstallController.ts';

export type EmitEvents = {
  "ws/connected": void,
  "ws/disconnected": void
  "ws/message": any,
  "action/context-menu-open": any;
  "action/force-changelog-open": string
  "alert/simple": any;
}

type AppState = {
  emitter: Emitter<EmitEvents>;
  controllers: {
    install: InstanceInstallController
  }
}

export const useAppStore = defineStore("app", {
  state: (): AppState => {
    const emitter = mitt<EmitEvents>();
    
    return {
      emitter: emitter,
      controllers: {
        install: new InstanceInstallController(emitter)
      }
    }
  },
  
  actions: {
  }
})