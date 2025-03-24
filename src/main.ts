import "./styles.css"
import '@/assets/fonts.scss';
import '@/assets/global.scss';
import 'wysiwyg.css/wysiwyg.css';
import 'balloon-css/balloon.css';
import '@/assets/liboverrides.scss';

import platform from '@/utils/interface/electron-overwolf';
import { bootstrapLoad } from '@/bootstrap.ts';

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue';
import router from './router';
import {library} from '@fortawesome/fontawesome-svg-core';
import {fas} from '@fortawesome/free-solid-svg-icons';
import {fab} from '@fortawesome/free-brands-svg-icons';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';

import 'vue-virtual-scroller/dist/vue-virtual-scroller.css';

import VueVirtualScroller from 'vue-virtual-scroller';
import {createLogger} from '@/core/logger';

// Use the relative time module from dayjs
dayjs.extend(relativeTime);

library.add(fas);
library.add(fab);

const logger = createLogger("main.ts");
logger.info("Starting app");

const pinia = createPinia();

platform.setup();
(window as any).platform = platform;

logger.info("Creating vue instance");

createApp(App)
  .use(router)
  .use(pinia)
  .use(VueVirtualScroller)
  .mount('#app')
  .$nextTick(() => {
    bootstrapLoad();
    // Use contextBridge
    // window.ipcRenderer.on('main-process-message', (_event, message) => {
    //   console.log(message)
    // })
    // TODO: [Port] fix me
  }).catch(console.error)

platform.get.setupApp();