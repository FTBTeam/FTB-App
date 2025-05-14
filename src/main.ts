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
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';

import { createLogger } from '@/core/logger';
import { initStateProcessor } from '@/core/controllers/runningStateProcessor.ts';
import { createGtag } from 'vue-gtag';

/**
 * Setup Google Analytics, with page tracking.
 * @Docs: https://matteo-gabriele.gitbook.io/vue-gtag
 * */
const gtag = createGtag({
  tagId: 'G-EP6FWM6LG9',
  pageTracker: {
    router, 
  },
})

// Use the relative time module from dayjs
dayjs.extend(relativeTime);

const logger = createLogger("main.ts");
logger.info("Starting app");

const pinia = createPinia(); 

logger.info("Creating vue instance");

createApp(App)
  .use(router)
  .use(pinia)
  .use(gtag)
  .mount('#app')
  .$nextTick(() => {
    initStateProcessor();
  }).catch(console.error)

appPlatform.setupApp();