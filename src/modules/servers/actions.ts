import { ActionTree } from 'vuex';
import { Server, ServersState } from './types';
import { RootState } from '@/types';
import axios, { AxiosResponse } from 'axios';
import { queryServer } from '@/utils';

export interface ServerListResponse {
  status: string;
  servers: Server[];
}

export const actions: ActionTree<ServersState, RootState> = {
  async getServers(
    { rootState, commit },
    payload: {
      id: number;
      onSingleDone: (server: Server) => void;
    },
  ): Promise<void> {
    let req;
    try {
      req = await axios.post<ServerListResponse>(
        'https://api.creeper.host/minetogether/list',
        { projectid: payload.id },
        {
          headers: {
            'Content-Type': 'application/json',
          },
        },
      );
    } catch (e) {}

    if (!req || !req.data.servers) {
      return;
    }

    await Promise.all(
      req.data.servers.map(async (e: Server) => {
        try {
          const queryRes = await queryServer(e.ip);
          if (queryRes) {
            const server = { ...e, protoResponse: queryRes };
            commit('addServer', { id: payload.id, server });
            payload.onSingleDone(server);
          }
        } catch {}
      }),
    );
  },
  /**
   * @deprecated DON'T use, it's bad
   */
  fetchServers({ rootState, commit, dispatch, state }, id): Promise<void> {
    commit('setLoading', true);
    return axios
      .post<ServerListResponse>(
        'https://api.creeper.host/minetogether/list',
        { projectid: id },
        {
          headers: {
            'Content-Type': 'application/json',
          },
        },
      )
      .then(async (response: AxiosResponse<ServerListResponse>) => {
        const servers = response.data.servers;
        await Promise.all(
          servers.map(async server => {
            try {
              server.protoResponse = await queryServer(server.ip);
              commit('updateServer', { id: id, server });
            } catch {}
          }),
        );
        commit('loadServers', { id: id, servers });
        commit('setLoading', false);
      })
      .catch(err => {
        commit('setLoading', false);
        console.error(err);
      });
  },
  fetchFeaturedServers({ rootState, commit, dispatch, state }): Promise<void> {
    commit('setLoading', true);
    return axios
      .get<ServerListResponse>('https://api.creeper.host/minetogether/list', {
        headers: {
          'Content-Type': 'application/json',
        },
      })
      .then((response: AxiosResponse<ServerListResponse>) => {
        const servers = response.data.servers;
        servers.forEach(async server => {
          server.protoResponse = await queryServer(server.ip);
          commit('updateServer', { id: 'featured', server });
        });
        commit('loadServers', { id: 'featured', servers });
        commit('setLoading', false);
      })
      .catch(err => {
        commit('setLoading', false);
        console.error(err);
      });
  },
};
