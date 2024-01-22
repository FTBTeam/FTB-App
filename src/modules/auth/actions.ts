import {ActionTree} from 'vuex';
import {AuthState} from './types';
import {RootState} from '@/types';
import {sendMessage} from '@/core/websockets/websocketsApi';
import {gobbleError} from '@/utils/helpers/asyncHelpers';
import {createLogger} from '@/core/logger';

export interface FriendRequestResponse {
  status: string;
  message: string;
  hash?: string;
}

const logger = createLogger("auth/actions.ts");

export const actions: ActionTree<AuthState, RootState> = {
  logout({ rootState, commit, dispatch }): void {
    commit('storeAuthDetails', null);
    const settings = rootState.settings?.settings;
    if (settings !== undefined) {
      // TODO: FIX ME (REMOVED FROM SETTINGS)
      // if (settings.sessionString !== undefined && settings.sessionString.length > 0) {
      //   settings.sessionString = '';
      //   dispatch('settings/saveSettings', settings, { root: true });
      // }
    }
  },
  setWindow({ rootState, commit }, data: boolean): void {
    commit('setFriendsWindow', data);
  },
  async setSessionID({ rootState, commit, dispatch }, payload: any): Promise<void> {
    commit('storeSession', payload);
    commit('startLoggingIn');
    
    let response;
    try {
      const req = await fetch(`https://minetogether.io/api/me`, {
        headers: {
          'App-Sess-ID': payload,
          Accept: 'application/json',
        }
      })
      
      response = await req.json();
    } catch (err) {
      logger.error(err);
      commit('loggedIn');
      return;
    }
    

    const user = response;
    if (user.accounts.find((s: any) => s.identityProvider === 'mcauth') !== undefined) {
      const mc = user.accounts.find((s: any) => s.identityProvider === 'mcauth');
      try {
        const profileReq = await fetch(`https://api.creeper.host/minetogether/profile`, {
          headers: {
            'Content-Type': 'application/json',
          },
          method: 'POST',
          body: JSON.stringify({ target: mc.userName }),
        })

        const profileData = await profileReq.json();
        user.mc = profileData.profileData[mc.userName];
      } catch (err) {
        logger.error(err)
        commit('loggedIn');
      }
    }
    // platfrom.get.actions.setUser(
    //   platfrom.isElectron() ? { user } : { token: payload, 'app-auth': (response as any)['app-auth'] },
    // );
    dispatch('storeAuthDetails', user);
    commit('loggedIn');
  },
  async getNewSession({ rootState, commit, dispatch }, payload: any): Promise<void> {
    commit('startLoggingIn');
    let response;
    let request;
    try {
      request = await fetch("https://minetogether.io/api/me", {
        headers: {
          'App-Auth': payload,
          Accept: 'application/json',
        },
      });
      
      response = await request.json();
    } catch (err) {
      commit('loggedIn');
      return;
    }
    const user = response;
    if (user.accounts.find((s: any) => s.identityProvider === 'mcauth') !== undefined) {
      const mc = user.accounts.find((s: any) => s.identityProvider === 'mcauth');
      try {
        const profileRequest = await fetch(`https://api.creeper.host/minetogether/profile`, {
          headers: {
            'Content-Type': 'application/json',
          },
          method: 'POST',
          body: JSON.stringify({ target: mc.userName }),
        })

        const profile = await profileRequest.json();
        user.mc = profile.profileData[mc.userName];
      } catch (err) {
        logger.error(err)
        commit('loggedIn');
      }
    }

    // platfrom.get.actions.setUser(
    //   platfrom.isElectron() ? { user } : { token: request.headers.get('app-token'), 'app-auth': payload },
    // );
    dispatch('storeAuthDetails', user);
    commit('storeSession', request.headers.get('app-token'));
    commit('loggedIn');
  },
  async storeAuthDetails({ rootState, commit, dispatch }, payload: any) {
    payload.friendCode = '';
    commit('storeAuthDetails', payload);
    if (payload === null) {
      await gobbleError(() => sendMessage("storeAuthDetails", {
        mpKey: '',
        mpSecret: '',
        s3Bucket: '',
        s3Host: '',
        s3Key: '',
        s3Secret: '',
        mtHash: '',
      }, 100))
    } else {
      let s3Bucket = '';
      let s3Host = '';
      let s3Key = '';
      let s3Secret = '';
      let mpKey = '';
      if (payload.activePlan !== null && payload.activePlan !== undefined) {
        const fields = payload.activePlan.customFields.customfield;
        s3Bucket = fields.find((f: any) => f.name === 'S3 Bucket').value;
        s3Host = fields.find((f: any) => f.name === 'S3 Server').value;
        s3Key = fields.find((f: any) => f.name === 'S3 Key').value;
        s3Secret = fields.find((f: any) => f.name === 'S3 Secret').value;
      }
      if (payload.attributes.modpackschkey !== undefined) {
        mpKey = payload.attributes.modpackschkey[0];
      }
      let mtHash = '';
      if (payload.mc !== undefined && payload.mc.hash !== undefined && payload.mc.hash.long !== undefined) {
        mtHash = payload.mc.hash.long;
      }
      
      // No response expected 
      await gobbleError(() => sendMessage("storeAuthDetails", {
        mpKey, mpSecret: '', s3Bucket, s3Host, s3Key, s3Secret, mtHash
      }, 100))
      
      // Refresh instances
      dispatch('v2/instances/loadInstances', undefined, {
        root: true
      })

      // TODO: (M#01) Add back in some way
      // dispatch('modpacks/getPrivatePacks', {}, { root: true });
      // if (rootState.settings?.settings.enableChat) {
      //   dispatch('connectToIRC');
      // }
    }
  }
};
