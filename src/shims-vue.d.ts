declare module '*.vue' {
  import Vue from 'vue';
  export default Vue;
}

declare module '*.png';

declare module '*.svg' {
  const filePath: string;

  export default filePath;
}
