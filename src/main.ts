import platform from '@/utils/interface/electron-overwolf';
import Vue from 'vue';
import App from './App.vue';
import router from './router';
import { library } from '@fortawesome/fontawesome-svg-core';
import { fas } from '@fortawesome/free-solid-svg-icons';
import { far } from '@fortawesome/free-regular-svg-icons';
import { fab } from '@fortawesome/free-brands-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';

import '@/assets/fonts.scss';
import '@/assets/global.scss';
import '@/assets/tailwind.scss';
import 'wysiwyg.css/wysiwyg.css';

import store from './modules/store';
import FTBButton from '@/components/atoms/input/FTBButton.vue';
import FTBInput from '@/components/atoms/input/FTBInput.vue';
import Popover from '@/components/atoms/Popover.vue';
import Modal from '@/components/atoms/modal/Modal.vue';
import Message from '@/components/atoms/Message.vue';
import { BrowserTracing } from '@sentry/tracing';
import * as Sentry from '@sentry/vue';
import ModalFooter from '@/components/atoms/modal/ModalFooter.vue';
import ModalBody from '@/components/atoms/modal/ModalBody.vue';
import {localiseNumber, toTitleCase} from '@/utils/helpers/stringHelpers';
import {standardDate, standardDateTime, timeFromNow} from '@/utils/helpers/dateHelpers';

// Use the relative time module from dayjs
dayjs.extend(relativeTime);

const appSetup = async () => {
  try {
    await platform.setup();
  } catch (e) {
    console.log('Platform failed resolve deps', e);
  }

  if (process.env.NODE_ENV === 'production') {
    Sentry.init({
      Vue,
      environment: process.env.VUE_APP_PLATFORM,
      dsn: process.env.VUE_APP_SENTRY_DSN,
      integrations: [
        new BrowserTracing({
          routingInstrumentation: Sentry.vueRouterInstrumentation(router),
        }),
      ],
      release: `${platform.get.config.appVersion}-${process.env.VUE_APP_PLATFORM}`,
      initialScope: {
        tags: {
          'release.vue': platform.get.config.webVersion,
          'release.public': platform.get.config.publicVersion,
          'release.platform': await platform.get.utils.getPlatformVersion(),
          'os.arch': platform.get.utils.getOsArch(),
        },
      },
      
      trackComponents: true,
      tracesSampleRate: 1.0,
    });
  }
  
  library.add(fas);
  library.add(far);
  library.add(fab);

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

  Vue.mixin({
    methods: {
      openExternal(event: any) {
        event.preventDefault();
        platform.get.utils.openUrl(event.target.href);
      },
      copyToClipboard(text: string) {
        platform.get.cb.copy(text);
      },
    },
  });

  Vue.filter('dayjs', standardDate);
  Vue.filter('dayjsFull', standardDateTime);
  Vue.filter('dayjsFromNow', timeFromNow);
  Vue.filter('formatNumber', localiseNumber);
  Vue.filter('title', toTitleCase);

  const vm = new Vue({
    router,
    store,
    render: (h: any) => h(App),
  }).$mount('#app');

  platform.get.setupApp(vm);
};

appSetup().catch((e) => console.error(e));
