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
import { constants } from "@/core/constants.ts";


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

app.mount('#app')
  .$nextTick(() => {
    initStateProcessor();
  }).catch(console.error)

appPlatform.setupApp();