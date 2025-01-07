import Vue from 'vue';
import Router from 'vue-router';
import Home from '@/views/Home.vue';
import MainApp from './views/MainApp.vue';
import {createLogger} from '@/core/logger';

Vue.use(Router);

export enum RouterNames {
  HOME = 'home',
  ROOT_LIBRARY = 'modpacks',
  ROOT_BROWSE_PACKS = 'browseModpacks',
  ROOT_BLOG = 'blog',
  ROOT_FAVORITES = 'favorites',
  ROOT_LOCAL_PACK = 'instancepage',
  ROOT_PREVIEW_PACK = 'modpackpage',
  ROOT_RUNNING_INSTANCE = 'running-instance',
  SETTINGS_INSTANCE = 'instance-settings',
  SETTINGS_ADVANCED = "advanced-settings",
  SETTINGS_DOWNLOAD = 'download-settings',
  SETTINGS_APP = 'app-settings',
  SETTINGS_APP_LICENSE = 'license',
  SETTINGS_MT_INTEGRATION = 'MTIntegration',
  SETTINGS_PROXY = 'app-proxy',
  SETTINGS_CHANGELOGS = 'changelogs',
  SETTINGS_PRIVACY = 'privacy',
  SUPPORT = 'support-index',
}

const router = new Router({
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
          component: () => import(/* webpackChunkName: "library" */ './views/Library.vue'),
        },
        {
          path: '/browseModpacks/:search?',
          name: RouterNames.ROOT_BROWSE_PACKS,
          component: () => import(/* webpackChunkName: "search" */ './views/BrowseModpacks.vue'),
        },
        {
          path: '/blog',
          name: RouterNames.ROOT_BLOG,
          component: () => import(/* webpackChunkName: "blog" */ './views/Blog.vue'),
        },
        {
          path: '/favorites',
          name: RouterNames.ROOT_FAVORITES,
          component: () => import(/* webpackChunkName: "favorites" */ './views/Favorites.vue'),
        },
        {
          path: '/settings',
          component: () => import(/* webpackChunkName: "settings" */ './views/Settings/Settings.vue'),
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
              path: 'app-info/license',
              name: RouterNames.SETTINGS_APP_LICENSE,
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/License.vue'),
            },
            {
              path: 'profile',
              name: RouterNames.SETTINGS_MT_INTEGRATION,
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/MTIntegration.vue'),
            },
            {
              path: 'proxy',
              name: RouterNames.SETTINGS_PROXY,
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/ProxySettings.vue'),
            },
            {
              path: 'changelogs',
              name: RouterNames.SETTINGS_CHANGELOGS,
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/Changelogs.vue'),
            },
            {
              path: 'privacy',
              name: RouterNames.SETTINGS_PRIVACY,
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/Privacy.vue'),
            },
            {
              path: 'advanced',
              name: RouterNames.SETTINGS_ADVANCED,
              component: () => import(/* webpackChunkName: "settings" */ './views/Settings/AdvancedSettings.vue'),
            }
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
          path: '/running/:uuid',
          name: RouterNames.ROOT_RUNNING_INSTANCE,
          component: () => import(/* webpackChunkName: "launchingpage" */ './views/RunningInstance.vue'),
        },
        {
          path: '/support',
          component: () => import(/* webpackChunkName: "support" */ './views/Support/SupportBase.vue'),
          children: [
            {
              path: '',
              name: RouterNames.SUPPORT,
              component: () => import(/* webpackChunkName: "support" */ './views/Support/SupportIndex.vue'),
            },
          ],
        },
      ],
    },
    // Fallback route for 404
    {
      path: '*',
      redirect: '/',
    }
  ],
  scrollBehavior: (to, from, savedPosition) => {
    if (savedPosition) {
      return {
        ...savedPosition,
        behavior: 'smooth',
      }
    }
    
    if (to.hash) {
      return {
        selector: to.hash,
        behavior: 'smooth',
      }
    }
    
    return {
      selector: '.app-content',
      behavior: 'smooth',
      x: 0,
      y: 0
    }
  }
});

const logger = createLogger('router.ts');
router.beforeEach((to, from, next) => {
  logger.debug(`Navigating from ${from.path} to ${to.path}`);
  next();
});

export default router;