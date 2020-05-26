

import {ipcRenderer, shell} from 'electron';
// console = remote.app.console;
// Object.assign(console, remote.app.console.functions);
import Vue from 'vue';
import App from './App.vue';
import router from './router';
import {library} from '@fortawesome/fontawesome-svg-core';
import {fas} from '@fortawesome/free-solid-svg-icons';
import {far} from '@fortawesome/free-regular-svg-icons';
import {fab} from '@fortawesome/free-brands-svg-icons';
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
}]);


library.add(fas);
library.add(far);
library.add(fab);
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

Vue.filter('prettyBytes', function (num:number) {
    // jacked from: https://github.com/sindresorhus/pretty-bytes
    if (isNaN(num)) {
        throw new TypeError('Expected a number');
    }

    var exponent;
    var unit;
    var neg = num < 0;
    var units = ['B', 'kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

    if (neg) {
        num = -num;
    }

    if (num < 1) {
        return (neg ? '-' : '') + num + ' B';
    }

    exponent = Math.min(Math.floor(Math.log(num) / Math.log(1000)), units.length - 1);
    // @ts-ignore
    num = (num / Math.pow(1000, exponent)).toFixed(2) * 1;
    unit = units[exponent];

    return (neg ? '-' : '') + num + ' ' + unit;
});


let vm = new Vue({
    router,
    store,
    components: {
        // 'vue-showdown': VueShowdown
    },
    render: (h: any) => h(App),
}).$mount('#app');

ipcRenderer.send('sendMeSecret');
ipcRenderer.on('hereIsSecret', (event, data) => {
    if(data.port === 13377){
        Vue.use(VueNativeSock, 'ws://localhost:' + data.port, {format: 'json', reconnection: true, connectManually: true});
        vm.$connect();
        vm.$socket.onmessage = (msgData: MessageEvent) => {
            let wsInfo = JSON.parse(msgData.data);
            store.commit('STORE_WS', wsInfo);
            vm.$disconnect();
            let index = Vue._installedPlugins.indexOf(VueNativeSock)
            if(index > -1){
                Vue._installedPlugins.splice(index, 1)
            }
            Vue.use(VueNativeSock, 'ws://localhost:' + wsInfo.port, {store, format: 'json', reconnection: true});
        }
    } else {
        store.commit('STORE_WS', data);
        Vue.use(VueNativeSock, 'ws://localhost:' + data.port, {store, format: 'json', reconnection: true});
    }
});
ipcRenderer.on('hereAuthData', (event, data) => {
    store.commit('auth/storeAuthDetails', {key: data.modpackskey, secret: data.modpackssecret})
})
