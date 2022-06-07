import platform from '@/utils/interface/electron-overwolf';

import Vue from 'vue';
import App from './App.vue';
import router from './router';
import { library } from '@fortawesome/fontawesome-svg-core';
import { fas } from '@fortawesome/free-solid-svg-icons';
import { far } from '@fortawesome/free-regular-svg-icons';
import { fab } from '@fortawesome/free-brands-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
// @ts-ignore
import VueShowdown, { showdown } from 'vue-showdown';
// @ts-ignore
import vSelectMenu from 'v-selectmenu';
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
import Modal from '@/components/atoms/Modal.vue';
import Message from '@/components/atoms/Message.vue';
import { BrowserTracing } from '@sentry/tracing';
import * as Sentry from '@sentry/vue';

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

  const classMap: object = {
    h1: 'text-4xl',
    h2: 'text-3xl',
    h3: 'text-2xl',
    h4: 'text-xl',
    h5: 'text-lg',
    h6: 'title is-6',
    em: 'italic',
    ul: 'list-inside list-disc mt-2 mb-4',
    a: 'text-gray-400 hover:text-white leading-none cursor-pointer hover:underline',
    p: 'my-2',
  };

  /*const attributeMap = {
    // a: '@click="openExternal"',
  };*/

  showdown.extension(
    'classMap',
    Object.keys(classMap).map((key) => ({
      type: 'output',
      regex: new RegExp(`<${key}(.*)>`, 'g'),
      // @ts-ignore
      replace: `<${key} class='${classMap[key]}' $1>`,
    })),
  );

  // showdown.extension(
  //   'attribMap',
  //   Object.keys(attributeMap).map(key => ({
  //     type: 'output',
  //     regex: new RegExp(`<${key}(.*)>`, 'g'),
  //     // @ts-ignore
  //     replace: `<${key} ${attributeMap[key]} $1>`,
  //   })),
  // );

  showdown.extension('newLine', () => [
    {
      type: 'output',
      regex: new RegExp(`\n`, 'g'),
      replace: '<br>',
    },
  ]);

  library.add(fas);
  library.add(far);
  library.add(fab);

  Vue.use(vSelectMenu, { language: 'en' });
  Vue.use(VueShowdown, {
    options: {
      emoji: true,
      tables: true,
      underline: true,
      openLinksInNewWindow: true,
      strikethrough: true,
    },
  });

  // global components
  Vue.component('font-awesome-icon', FontAwesomeIcon);
  Vue.component('ftb-button', FTBButton);
  Vue.component('ftb-input', FTBInput);
  Vue.component('popover', Popover);
  Vue.component('modal', Modal);
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

  Vue.filter('dayjs', (value: any) => (value ? dayjs.unix(value).format('DD MMMM YYYY') : ''));
  Vue.filter('dayjsFromNow', (value: any) => {
    if (!value) return 'Never';

    if (typeof value === 'number') {
      return dayjs.unix(value).fromNow(true) + ' ago';
    }

    return value ? dayjs().from(dayjs(value), true) + ' ago' : 'Never';
  });
  Vue.filter('formatNumber', (value: number) => (value ? value.toLocaleString() : '0'));
  Vue.filter('title', (value: string) => (!value ? '' : value[0].toUpperCase() + value.slice(1)));

  const vm = new Vue({
    router,
    store,
    render: (h: any) => h(App),
  }).$mount('#app');

  platform.get.setupApp(vm);
};

appSetup().catch((e) => console.error(e));
