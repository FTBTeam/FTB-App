// This must be done as early as possible
import platform from '@/utils/interface/electron-overwolf';

import { ModPack } from './modules/modpacks/types';


import {clipboard, ipcRenderer, shell} from 'electron';
// console = remote.app.console;
// Object.assign(console, remote.app.console.functions);
import Vue from 'vue';
import App from './App.vue';
import axios from 'axios';
import router from './router';
import {library} from '@fortawesome/fontawesome-svg-core';
import {fas, faToriiGate} from '@fortawesome/free-solid-svg-icons';
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
import { logVerbose } from './utils';
import { getAPIRequest } from './modules/modpacks/actions';



const classMap: object = {
    h1: 'text-4xl',
    h2: 'text-3xl',
    h3: 'text-2xl',
    h4: 'text-xl',
    h5: 'text-lg',
    h6: 'title is-6',
    em: 'italic',
    ul: 'list-inside',
    a: 'text-gray-400 hover:text-white leading-none cursor-pointer hover:underline',
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
// Vue.use(vueMoment);
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
        copyToClipboard(text: string) {
            clipboard.writeText(text);
        },
    },
});

Vue.filter('moment', (value: any) => {
    if (!value) { return ''; }
    value = value.toString();
    return moment.unix(value).format('Do MMMM YYYY');
});

Vue.filter('momentFromNow', (value: any) => {
    if (!value) { return ''; }
    value = value.toString();
    return moment.duration(moment.unix(value).diff(moment())).humanize(true);
});
Vue.filter('formatNumber', (value: number) => {
    if (!value) {return ''; }
    return new Intl.NumberFormat().format(value);
});

Vue.filter('prettyBytes', (num: number) => {
    // jacked from: https://github.com/sindresorhus/pretty-bytes
    if (isNaN(num)) {
        throw new TypeError('Expected a number');
    }

    let exponent;
    let unit;
    const neg = num < 0;
    const units = ['B', 'kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

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


const vm = new Vue({
    router,
    store,
    components: {
        // 'vue-showdown': VueShowdown
    },
    render: (h: any) => h(App),
}).$mount('#app');
axios.get(`https://minetogether.io/api/adPool`).then((res) => {
    try {
        const id = parseInt(res.data, 10);
        if (id !== -1) {
            window.adPoolID = null;
        } else {
            window.adPoolID = id;
        }
    } catch (err) {
        window.adPoolID = null;
    }
}).catch(() => {
    window.adPoolID = null;
});
if (router.currentRoute.name !== 'chat') {
    store.dispatch('registerModProgressCallback', (data: any) => {
        if (data.messageType === 'message') {
            if (data.message === 'init') {
                store.commit('modpacks/setLaunchProgress', []);
                if (data.instance) {
                    router.push({name: 'launchingpage', query: {uuid: data.instance}});
                }
            } else {
                if (router.currentRoute.name === 'launchingpage') {
                    router.replace({name: 'instancepage', query: {uuid: data.instance}});
                }
                store.commit('modpacks/setLaunchProgress', undefined);
            }
        } else if (data.messageType === 'progress') {
            if (router.currentRoute.name !== 'launchingpage') {
                router.push({name: 'launchingpage', query: {uuid: data.instance}});
            }
            if (data.clientData.bars) {
                store.commit('modpacks/setLaunchProgress', data.clientData.bars);
            }
        } else if (data.messageType === 'clientDisconnect') {
            if (router.currentRoute.name === 'launchingpage') {
                router.replace({name: 'instancepage', query: {uuid: data.instance}});
            }
        }
    });
    }

ipcRenderer.send('sendMeSecret');
ipcRenderer.on('hereIsSecret', (event, data) => {
    if (data.port === 13377 && !data.isDevMode) {
        Vue.use(VueNativeSock, 'ws://localhost:' + data.port, {format: 'json', reconnection: true, connectManually: true});
        vm.$connect();
        vm.$socket.onmessage = (msgData: MessageEvent) => {
            const wsInfo = JSON.parse(msgData.data);
            store.commit('STORE_WS', wsInfo);
            vm.$disconnect();
            const index = Vue._installedPlugins.indexOf(VueNativeSock);
            if (index > -1) {
                Vue._installedPlugins.splice(index, 1);
            }
            Vue.use(VueNativeSock, 'ws://localhost:' + wsInfo.port, {store, format: 'json', reconnection: true});
            ipcRenderer.send('updateSecret', wsInfo);
        };
    } else {
        store.commit('STORE_WS', data);
        Vue.use(VueNativeSock, 'ws://localhost:' + data.port, {store, format: 'json', reconnection: true});
    }
});
ipcRenderer.send('gimmeAuthData');
ipcRenderer.on('hereAuthData', (event, data) => {
    store.commit('auth/storeAuthDetails', data, {root: true});
});
ipcRenderer.on('setFriendsWindow', (event, data) => {
    store.dispatch('auth/setWindow', data, {root: true});
});
ipcRenderer.on('setSessionString', (event, data) => {
    const settings = store.state.settings?.settings;
    if (settings !== undefined) {
        settings.sessionString = data;
    }
    store.dispatch('settings/saveSettings', settings, {root: true});
});
ipcRenderer.on('getNewSession', (event, data) => {
    store.dispatch('auth/getNewSession', data, {root: true});
});
ipcRenderer.on('setSessionID', (event, data) => {
    store.dispatch('auth/setSessionID', data, {root: true});
});
ipcRenderer.on('blockFriend', (event, data) => {
    const settings = store.state.settings?.settings;
    if (settings !== undefined && settings.blockedUsers === undefined) {
        settings.blockedUsers = [];
    }
    if (typeof settings?.blockedUsers !== 'string') {
        settings?.blockedUsers.push(data);
    }
    store.dispatch('settings/saveSettings', settings, {root: true});
});
ipcRenderer.on('openModpack', (event, data) => {
    const {name, id} = data;
    getAPIRequest(
        store.state,
        `modpack/search/8?term=${name}`,
    )
        .then((response) => response.json())
        .then(async (data) => {
        if (data.status === 'error') {
            return;
        }
        const packIDs = data.packs;
        if (packIDs == null) {
            return;
        }
        if (packIDs.length === 0) {
            return;
        }
        for (let i = 0; i < packIDs.length; i++) {
            const packID = packIDs[i];
            const pack: ModPack = await store.dispatch('modpacks/fetchModpack', packID, {root: true});
            if (pack !== undefined) {
                const foundVersion = pack.versions.find((v) => v.mtgID === id);
                if (foundVersion !== undefined) {
                    router.push({name: 'modpackpage', query: {modpackid: packID}});
                    return;
                }
            }
        }
        })
        .catch((err) => {
        console.error(err);
        });
});
ipcRenderer.on('parseProtocolURL', (event, data) => {
    let protocolURL = data;
    if (protocolURL === undefined) {
        return;
    }
    protocolURL = protocolURL.substring(6, protocolURL.length);
    const parts = protocolURL.split('/');
    const command = parts[0];
    const args = parts.slice(1, parts.length);
    if (command === 'modpack') {
        if (args.length === 0) {
            return;
        }
        logVerbose(store.state, 'Received modpack protocol message', args);
        const modpackID = args[0];
        if (args.length === 1) {
            // Navigate to page for modpack
            logVerbose(store.state, 'Navigating to page for modpack', modpackID);
            router.push({name: 'modpackpage', query: {modpackid: modpackID}});
        } else if (args.length === 2) {
            if (args[1] === 'install') {
                // Popup install for modpack
                logVerbose(store.state, 'Popping up install for modpack', modpackID);
                router.push({name: 'modpackpage', query: {modpackid: modpackID, showInstall: 'true'}});
            }
        } else if (args.length === 3) {
            if (args[2] === 'install') {
                // Popup install for modpack with version default selected
                router.push({name: 'modpackpage', query: {modpackid: modpackID, showInstall: 'true', version: args[1]}});
            }
        }
    } else if (command === 'instance') {
        if (args.length === 0) {
            return;
        }
        const instanceID = args[0];
        if (args.length === 1) {
            // Open instance page
            router.push({name: 'instancepage', query: {uuid: instanceID}});
        } else if (args.length === 2) {
            // Start instance
            router.push({name: 'instancepage', query: {uuid: instanceID, shouldPlay: 'true'}});
        }
    } else if (command === 'server') {
        if (args.length === 0) {
            return;
        }
        const serverID = args[0];
        router.push({name: 'server', query: {serverid: serverID}});
    }
});
ipcRenderer.on('sendWebsocket', (event, data) => {
    console.log('Request received to send ', data);
    const messageID = Math.round(Math.random() * 1000);
    data.requestId = messageID;
    data.secret = store.state.wsSecret;
    vm.$socket.sendObj(data);
});
