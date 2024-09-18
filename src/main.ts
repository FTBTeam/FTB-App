import platform from '@/utils/interface/electron-overwolf';
import Vue from 'vue';
import App from './App.vue';
import router from './router';
import {library} from '@fortawesome/fontawesome-svg-core';
import {fas} from '@fortawesome/free-solid-svg-icons';
import {fab} from '@fortawesome/free-brands-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';

import '@/assets/fonts.scss';
import '@/assets/global.scss';
import '@/assets/tailwind.scss';
import 'wysiwyg.css/wysiwyg.css';
import '@/assets/liboverrides.scss';

import 'vue-virtual-scroller/dist/vue-virtual-scroller.css';
import '@/core/controllers/InstanceInstallController';
import store from './modules/store';
import FTBButton from '@/components/atoms/input/FTBButton.vue';
import FTBInput from '@/components/atoms/input/FTBInput.vue';
import Popover from '@/components/atoms/Popover.vue';
import Modal from '@/components/atoms/modal/Modal.vue';
import Message from '@/components/atoms/Message.vue';
// import {BrowserTracing} from '@sentry/tracing';
import ModalFooter from '@/components/atoms/modal/ModalFooter.vue';
import ModalBody from '@/components/atoms/modal/ModalBody.vue';
import {localiseNumber, toTitleCase} from '@/utils/helpers/stringHelpers';
import {standardDate, standardDateTime, timeFromNow} from '@/utils/helpers/dateHelpers';
import VueMixins from '@/core/vueMixins.vue';
// @ts-ignore no typescript package available
import VueNativeSock from 'vue-native-websocket';

// @ts-ignore - no types
import VueVirtualScroller from 'vue-virtual-scroller';
import {createLogger} from '@/core/logger';

// Use the relative time module from dayjs
dayjs.extend(relativeTime);

const logger = createLogger("main.ts");
logger.info("Starting app");

const appSetup = async () => {
  try {
    await platform.setup();
    (window as any).platform = platform;
  } catch (e) {
    logger.error("Failed to setup platform", e);
    console.error(e);
  }
  
  library.add(fas);
  library.add(fab);

  Vue.use(VueVirtualScroller);
  
  // global components
  Vue.component('font-awesome-icon', FontAwesomeIcon);
  Vue.component('ftb-button', FTBButton);
  Vue.component('ftb-input', FTBInput);
  Vue.component('popover', Popover);
  Vue.component('modal', Modal);
  Vue.component('modal-body', ModalBody);
  Vue.component('modal-footer', ModalFooter);
  Vue.component('message', Message);

  Vue.config.productionTip = false;
  Vue.config.devtools = true;
  
  Vue.mixin(VueMixins);

  Vue.filter('dayjs', standardDate);
  Vue.filter('dayjsFull', standardDateTime);
  Vue.filter('dayjsFromNow', timeFromNow);
  Vue.filter('formatNumber', localiseNumber);
  Vue.filter('title', toTitleCase);

  logger.info("Creating vue instance");
  const vm = new Vue({
    router,
    store,
    render: (h: any) => h(App),
  }).$mount('#app');

  // This port isn't used for anything as we manually connect to the websocket later on
  Vue.use(VueNativeSock, 'ws://localhost:13377', {
    format: 'json',
    reconnection: true,
    connectManually: true,
    store
  });
  
  platform.get.setupApp(vm);
};

appSetup().catch(e => logger.error("Failed to setup app", e));
