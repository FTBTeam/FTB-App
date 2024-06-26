import {constants} from '@/core/constants';

const minecraftHeadsApi = constants.mcHeadApi;

export function getMinecraftHead(uuid: string | null) {
  if (!uuid) {
    uuid = 'c06f89064c8a49119c29ea1dbd1aab82'; // Minecraft Steve head
  }
  
  return minecraftHeadsApi.replace('{uuid}', uuid);
}

