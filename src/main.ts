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

// TODO: This might be able to be removed
import 'vue-virtual-scroller/dist/vue-virtual-scroller.css';

// TODO: Don't do this
// new Promise(async () => {
//   const config = await client.discovery(new URL("https://identity.feed-the-beast.com/realms/FTB"), "ftb-app");
//   const response = await client.initiateDeviceAuthorization(config, { scope: "profile" })
//   console.log(response);
// }).catch(e => console.error(e))

import {createLogger} from '@/core/logger';

// Use the relative time module from dayjs
dayjs.extend(relativeTime);

const logger = createLogger("main.ts");
logger.info("Starting app");

const pinia = createPinia();

logger.info("Creating vue instance");

createApp(App)
  .use(router)
  .use(pinia)
  .mount('#app')

appPlatform.setupApp();