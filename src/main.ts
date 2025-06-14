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
import { constants } from "@/core/constants.ts";

/**
 * Helper function for Google Analytics to transform the app URL (file:///.....) to a "real" url.
 */
function transformAppUrl(pathFull: string): string {
  if (pathFull.startsWith('file:///')) {
    const path = pathFull.split('index.html')[1] || '';
    return `https://app.feed-the-beast.com/${path}`;
  }
  return pathFull;
}

/**
 * Setup Google Analytics, with page tracking.
 * @Docs: https://matteo-gabriele.gitbook.io/vue-gtag
 * */
const gtag = constants.isDevelopment ? null : createGtag({
  tagId: 'G-EP6FWM6LG9',
  pageTracker: {
    router,
    template: () => ({
      page_title: router.currentRoute.value.name?.toString(),
      page_path: router.currentRoute.value.path,
      page_location: transformAppUrl(window.location.href)      
    })
  },
  config: {
    'cookie_domain': 'none'
  },
})


// Use the relative time module from dayjs
dayjs.extend(relativeTime);

const logger = createLogger("main.ts");

logger.info("Starting app");
logger.info("Constants", constants);

const pinia = createPinia();
logger.info("Creating pinia store");

logger.info("Creating vue instance");

const app = createApp(App);

app
  .use(router)
  .use(pinia)

if (gtag) {
  app.use(gtag);
}

app.mount('#app')
  .$nextTick(() => {
    initStateProcessor();
  }).catch(console.error)

appPlatform.setupApp();