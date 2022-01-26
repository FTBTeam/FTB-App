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

import '@/assets/global.scss';
import '@/assets/tailwind.scss';

import store from './modules/store';
import FTBButton from '@/components/atoms/input/FTBButton.vue';
import FTBInput from '@/components/atoms/input/FTBInput.vue';
import Popover from '@/components/atoms/Popover.vue';

// Use the relative time module from dayjs
dayjs.extend(relativeTime);

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
  Object.keys(classMap).map(key => ({
    type: 'output',
    regex: new RegExp(`<${key}(.*)>`, 'g'),
    // @ts-ignore
    replace: `<${key} class="${classMap[key]}" $1>`,
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

Vue.filter('dayjs', (value: any) => (value ? dayjs(value).format('Do MMMM YYYY') : ''));
Vue.filter('dayjsFromNow', (value: any) => (value ? dayjs().from(dayjs(value), true) : 'Never'));
Vue.filter('formatNumber', (value: number) => (value ? value.toLocaleString() : '0'));
Vue.filter('title', (value: string) => (!value ? '' : value[0].toUpperCase() + value.slice(1)));

(async () => {
  try {
    await platform.setup();
  } catch (e) {
    console.log('Platform failed resolve deps', e);
  }

  const vm = new Vue({
    router,
    store,
    render: (h: any) => h(App),
  }).$mount('#app');

  platform.get.setupApp(vm);
})();
