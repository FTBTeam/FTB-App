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


// 2. Specify a file with the types you want to augment
//    Vue has the constructor type in types/vue.d.ts
declare module 'electron' {
  // 3. Declare augmentation for Vue
  interface App {
    console: any;
  }
}