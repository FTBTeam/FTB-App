export interface RootState {
    version: string;
    alert: Alert | null;
    wsPort: number;
    wsSecret: string;
}

export interface Alert {
    type: string;
    title: string;
    message: string;
}

export interface ModalBox {
  title: string;
  message: string;
  buttons: Button[];
}

export interface Button {
  name: string;
  message: string;
  colour: string;
}

// 2. Specify a file with the types you want to augment
//    Vue has the constructor type in types/vue.d.ts
declare module 'electron' {
  // 3. Declare augmentation for Vue
  interface App {
    console: any;
  }
}


declare module 'vue/types/vue' {
  interface Vue {
    $connect: () => {};
    $socket: any;
    $disconnect: () => {};
  }
  interface VueConstructor {
    _installedPlugins: any;
  }
}
