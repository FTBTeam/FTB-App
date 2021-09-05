import Vue from 'vue';
import Router from 'vue-router';
import Home from '@/views/Home.vue';
import MainApp from './views/MainApp.vue';
import ChatWindow from './views/ChatWindow.vue';

Vue.use(Router);
export default new Router({
  mode: 'hash',
  routes: [
    {
      path: '/',
      component: MainApp,
      children: [
        {
          path: '/',
          name: 'home',
          component: Home,
        },
        {
          path: '/modpacks',
          name: 'modpacks',
          component: () => import(/* webpackChunkName: "modpacks" */ './views/Library.vue'),
        },
        {
          path: '/browseModpacks/:search?',
          name: 'browseModpacks',
          component: () => import(/* webpackChunkName: "modpacks" */ './views/BrowseModpacks.vue'),
        },
        {
          path: '/news',
          name: 'news',
          component: () => import(/* webpackChunkName: "news" */ './views/News.vue'),
        },
        {
          path: '/settings',
          name: 'settings',
          component: () => import(/* webpackChunkName: "settings" */ './views/Settings.vue'),
          children: [
            {
              path: '',
              name: 'instance-settings',
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/InstanceSettings.vue'),
            },
            {
              path: 'download-settings',
              name: 'download-settings',
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/DownloadSettings.vue'),
            },
            {
              path: 'app-settings',
              name: 'app-settings',
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/AppSettings.vue'),
            },
            {
              path: 'app-info',
              name: 'app-info',
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/AppInfo.vue'),
            },
            {
              path: 'app-info/license',
              name: 'license',
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/License.vue'),
            },
          ],
        },
        {
          path: '/instancepage',
          name: 'instancepage',
          component: () => import(/* webpackChunkName: "instancepage" */ './views/InstancePage.vue'),
        },
        {
          path: '/modpackpage',
          name: 'modpackpage',
          component: () => import(/* webpackChunkName: "modpackpage" */ './views/ModpackPage.vue'),
        },
        {
          path: '/installing',
          name: 'installingpage',
          component: () => import(/* webpackChunkName: "installingpage" */ './views/InstallingPage.vue'),
        },
        {
          path: '/launching',
          name: 'launchingpage',
          component: () => import(/* webpackChunkName: "launchingpage" */ './views/LaunchingPage.vue'),
        },
        {
          path: '/thirdparty',
          name: 'thirdparty',
          component: () => import(/* webpackChunkName: "thirdparty" */ './views/ComingSoon.vue'),
        },
        {
          path: '/discover',
          name: 'discover',
          component: () => import(/* webpackChunkName: "discovery" */ './views/DiscoverPage.vue'),
        },
        {
          path: '/server',
          name: 'server',
          component: () => import(/* webpackChunkName: "server" */ './views/ServerLandingPage.vue'),
        },
        {
          path: '/profile',
          name: 'profile',
          component: () => import(/* webpackChunkName: "profile" */ './views/ProfilePage.vue'),
        },
      ],
    },
    {
      path: '/chat',
      name: 'chat',
      component: ChatWindow,
    },
  ],
});
