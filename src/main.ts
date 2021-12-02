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
import vueMoment from 'vue-moment';
// @ts-ignore
import VueShowdown, { showdown } from 'vue-showdown';
import moment from 'moment';
// @ts-ignore
import vSelectMenu from 'v-selectmenu';

import '@/assets/global.scss';
import '@/assets/tailwind.scss';

import store from './modules/store';
import FTBButton from '@/components/atoms/input/FTBButton.vue';
import FTBInput from '@/components/atoms/input/FTBInput.vue';

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

const attributeMap = {
  // a: '@click="openExternal"',
};

showdown.extension(
  'classMap',
  Object.keys(classMap).map(key => ({
    type: 'output',
    regex: new RegExp(`<${key}(.*)>`, 'g'),
    // @ts-ignore
    replace: `<${key} class="${classMap[key]}" $1>`,
  })),
);

showdown.extension(
  'attribMap',
  Object.keys(attributeMap).map(key => ({
    type: 'output',
    regex: new RegExp(`<${key}(.*)>`, 'g'),
    // @ts-ignore
    replace: `<${key} ${attributeMap[key]} $1>`,
  })),
);

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

Vue.use(vueMoment as any);
Vue.use(vSelectMenu, { language: 'en' });
Vue.use(VueShowdown, {
  options: {
    emoji: true,
    tables: true,
    underline: true,
    openLinksInNewWindow: true,
    strikethrough: true,
    // simpleLineBreaks: true,
  },
});

// global components
Vue.component('font-awesome-icon', FontAwesomeIcon);
Vue.component('ftb-button', FTBButton);
Vue.component('ftb-input', FTBInput);

Vue.config.productionTip = false;
Vue.config.devtools = true;

Vue.mixin({
  methods: {
    openExternal(event: any) {
      event.preventDefault();
      const link = event.target.href;
      platform.get.utils.openUrl(link);
    },
    copyToClipboard(text: string) {
      platform.get.cb.copy(text);
    },
  },
});

Vue.filter('moment', (value: any) => {
  if (!value) {
    return '';
  }
  value = value.toString();
  return moment.unix(value).format('Do MMMM YYYY');
});

Vue.filter('momentFromNow', (value: any) => {
  if (!value) {
    return '';
  }
  value = value.toString();
  return moment.duration(moment.unix(value).diff(moment())).humanize(true);
});

Vue.filter('formatNumber', (value: number) => {
  if (!value) {
    return '';
  }
  return new Intl.NumberFormat().format(value);
});

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

  if (router.currentRoute.name !== 'chat') {
    store.dispatch('registerModProgressCallback', (data: any) => {
      if (data.messageType === 'message') {
        if (data.message === 'init') {
          store.commit('modpacks/setLaunchProgress', []);
          if (data.instance) {
            router.push({ name: 'launchingpage', query: { uuid: data.instance } });
          }
        } else {
          if (router.currentRoute.name === 'launchingpage') {
            router.replace({ name: 'instancepage', query: { uuid: data.instance } });
          }
          store.commit('modpacks/setLaunchProgress', undefined);
        }
      } else if (data.messageType === 'progress') {
        if (router.currentRoute.name !== 'launchingpage') {
          router.push({ name: 'launchingpage', query: { uuid: data.instance } });
        }
        if (data.clientData.bars) {
          store.commit('modpacks/setLaunchProgress', data.clientData.bars);
        }
      } else if (data.messageType === 'clientDisconnect') {
        if (router.currentRoute.name === 'launchingpage') {
          router.replace({ name: 'instancepage', query: { uuid: data.instance } });
        }
      }
    });
  }

  platform.get.setupApp(vm);
})();
