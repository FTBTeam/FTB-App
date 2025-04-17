import { defineStore } from 'pinia';
import mitt, {type Emitter} from 'mitt';

// TODO: Better typings
export type EmitEvents = {
  "ws/connected": void,
  "ws/disconnected": void
  "ws/message": any,
  "action/context-menu-open": any;
  "alert/simple": any;
}

type AppState = {
  emitter: Emitter<EmitEvents>;
}

export const useAppStore = defineStore("app", {
  state: (): AppState => {
    const emitter = mitt<EmitEvents>();
    
    return {
      emitter: emitter,
    }
  },
  
  actions: {
  }
})