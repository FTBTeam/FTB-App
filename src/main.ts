import "./styles.css"
import '@/assets/fonts.scss';
import '@/assets/global.scss';
import 'wysiwyg.css/wysiwyg.css';
import 'balloon-css/balloon.css';
import '@/assets/liboverrides.scss';

import appPlatform from '@platform';

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

logger.info("Creating vue instance");

createApp(App)
  .use(router)
  .use(pinia)
  .use(VueVirtualScroller)
  .mount('#app')

appPlatform.setupApp();