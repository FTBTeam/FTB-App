import Vue from 'vue';
import App from './App.vue';
import router from './router';
import {shell} from 'electron';
import {library} from '@fortawesome/fontawesome-svg-core';
import {fas} from '@fortawesome/free-solid-svg-icons';
import {far} from '@fortawesome/free-regular-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/vue-fontawesome';
// @ts-ignore
import VueNativeSock from 'vue-native-websocket';
// @ts-ignore
import vueMoment from 'vue-moment';
// @ts-ignore
// import VueMarkdown from 'vue-markdown';
import VueShowdown, {showdown} from 'vue-showdown';
import moment from 'moment';
// @ts-ignore
import vSelectMenu from 'v-selectmenu';
import '@/assets/tailwind.scss';

import store from './store';


const classMap: object = {
    h1: 'text-4xl',
    h2: 'text-3xl',
    h3: 'text-2xl',
    h4: 'text-xl',
    h5: 'text-lg',
    h6: 'title is-6',
    em: 'italic',
    ul: 'list-inside',
    a: 'leading-none cursor-pointer underline hover:bold',
    // li: 'ui item'
};

const styleMap = {
    // h3: 'padding-top: 1.5rem; margin-bottom: .5rem; margin-left: 0;'
};

const attributeMap = {
    // a: '@click="openExternal"',
};


showdown.extension('classMap', Object.keys(classMap).map((key) => ({
    type: 'output',
    regex: new RegExp(`<${key}(.*)>`, 'g'),
    // @ts-ignore
    replace: `<${key} class="${classMap[key]}" $1>`,
})));

showdown.extension('attribMap', Object.keys(attributeMap).map((key) => ({
    type: 'output',
    regex: new RegExp(`<${key}(.*)>`, 'g'),
    // @ts-ignore
    replace: `<${key} ${attributeMap[key]} $1>`,
})));

showdown.extension('newLine', () => [{
    type: 'output',
    regex: new RegExp(`\n`, 'g'),
    replace: '<br>',
}])


library.add(fas);
library.add(far);
Vue.use(vueMoment);
Vue.use(vSelectMenu, {language: 'en'});
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
Vue.component('font-awesome-icon', FontAwesomeIcon);
Vue.use(VueNativeSock, 'ws://localhost:13377', {store, format: 'json', reconnection: true});

Vue.config.productionTip = false;
Vue.config.devtools = true;

Vue.mixin({
    methods: {
        openExternal(event: any) {
            event.preventDefault();
            const link = event.target.href;
            shell.openExternal(link);
        },
    },
});

// tslint:disable-next-line:only-arrow-functions
Vue.filter('moment', function(value: any) {
    if (!value) { return ''; }
    value = value.toString();
    return moment.unix(value).format('Do MMMM YYYY');
});


new Vue({
    router,
    store,
    components: {
        // 'vue-showdown': VueShowdown
    },
    render: (h: any) => h(App),
}).$mount('#app');
