import Vue from 'vue';
import Router from 'vue-router';
import Home from '@/views/Home.vue';
import MainApp from './views/MainApp.vue';
import ChatWindow from './views/ChatWindow.vue';

Vue.use(Router);

export enum RouterNames {
  HOME = 'home',
  ROOT_LIBRARY = 'modpacks',
  ROOT_BROWSE_PACKS = 'browseModpacks',
  ROOT_NEWS = 'news',
  ROOT_LOCAL_PACK = 'instancepage',
  ROOT_PREVIEW_PACK = 'modpackpage',
  ROOT_INSTALL_PACK = 'installingpage',
  ROOT_LAUNCH_PACK = 'launchingpage',
  ROOT_THIRDPARTY = 'thirdparty',
  ROOT_DISCOVER = 'discover',
  ROOT_SERVER = 'server',
  SETTINGS_INSTANCE = 'instance-settings',
  SETTINGS_DOWNLOAD = 'download-settings',
  SETTINGS_APP = 'app-settings',
  SETTINGS_INTEGRATION = 'integrations',
  SETTINGS_INFO = 'app-info',
  SETTINGS_APP_LICENSE = 'license',
  SETTINGS_MT_INTEGRATION = 'MTIntegration',
  CHAT = 'chat',
  DEVELOPER = 'dev',
}

export default new Router({
  mode: 'hash',
  routes: [
    {
      path: '/',
      component: MainApp,
      children: [
        {
          path: '/',
          name: RouterNames.HOME,
          component: Home,
        },
        {
          path: '/modpacks',
          name: RouterNames.ROOT_LIBRARY,
          component: () => import(/* webpackChunkName: "modpacks" */ './views/Library.vue'),
        },
        {
          path: '/browseModpacks/:search?',
          name: RouterNames.ROOT_BROWSE_PACKS,
          component: () => import(/* webpackChunkName: "modpacks" */ './views/BrowseModpacks.vue'),
        },
        {
          path: '/news',
          name: RouterNames.ROOT_NEWS,
          component: () => import(/* webpackChunkName: "news" */ './views/News.vue'),
        },
        {
          path: '/settings',
          component: () => import(/* webpackChunkName: "settings" */ './views/Settings.vue'),
          children: [
            {
              path: '',
              name: RouterNames.SETTINGS_INSTANCE,
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/InstanceSettings.vue'),
            },
            {
              path: 'download-settings',
              name: RouterNames.SETTINGS_DOWNLOAD,
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/DownloadSettings.vue'),
            },
            {
              path: 'app-settings',
              name: RouterNames.SETTINGS_APP,
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/AppSettings.vue'),
            },
            {
              path: 'integrations',
              name: RouterNames.SETTINGS_INTEGRATION,
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/Integrations.vue'),
            },
            {
              path: 'app-info',
              name: RouterNames.SETTINGS_INFO,
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/AppInfo.vue'),
            },
            {
              path: 'app-info/license',
              name: RouterNames.SETTINGS_APP_LICENSE,
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/License.vue'),
            },
            {
              path: 'profile',
              name: RouterNames.SETTINGS_MT_INTEGRATION,
              component: () => import(/* webpackChunkName: "profile" */ './views/Settings/MTIntegration.vue'),
            },
          ],
        },
        {
          path: '/instancepage',
          name: RouterNames.ROOT_LOCAL_PACK,
          component: () => import(/* webpackChunkName: "instancepage" */ './views/InstancePage.vue'),
        },
        {
          path: '/modpackpage',
          name: RouterNames.ROOT_PREVIEW_PACK,
          component: () => import(/* webpackChunkName: "modpackpage" */ './views/ModpackPage.vue'),
        },
        {
          path: '/installing',
          name: RouterNames.ROOT_INSTALL_PACK,
          component: () => import(/* webpackChunkName: "installingpage" */ './views/InstallingPage.vue'),
        },
        {
          path: '/launching',
          name: RouterNames.ROOT_LAUNCH_PACK,
          component: () => import(/* webpackChunkName: "launchingpage" */ './views/LaunchingPage.vue'),
        },
        {
          path: '/thirdparty',
          name: RouterNames.ROOT_THIRDPARTY,
          component: () => import(/* webpackChunkName: "thirdparty" */ './views/ComingSoon.vue'),
        },
        {
          path: '/discover',
          name: RouterNames.ROOT_DISCOVER,
          component: () => import(/* webpackChunkName: "discovery" */ './views/DiscoverPage.vue'),
        },
        {
          path: '/server',
          name: RouterNames.ROOT_SERVER,
          component: () => import(/* webpackChunkName: "server" */ './views/ServerLandingPage.vue'),
        },
        {
          path: '/dev',
          name: RouterNames.DEVELOPER,
          component: () => import(/* webpackChunkName: "nothingtoseehere" */ './views/DeveloperPage.vue'),
        },
      ],
    },
    {
      path: '/chat',
      name: RouterNames.CHAT,
      component: ChatWindow,
    },
  ],
});
