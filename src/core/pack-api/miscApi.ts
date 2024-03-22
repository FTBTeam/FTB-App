import {createLogger} from '@/core/logger';
import {modpackApi} from '@/core/pack-api/modpackApi';

const logger = createLogger("miscApi.ts");

export interface NormalisedVersion {
  name: string;
  type: 'release' | 'snapshot' | 'alpha';
}

class MiscApiImpl {
  private static readonly MINECRAFT_MODPACKS_ID = 81;
  private static readonly MINECRAFT_VERSIONS = 'https://launchermeta.mojang.com/mc/game/version_manifest.json'; 
  
  async getMinecraftVersions() {
    const official = await this.getMincraftFromOfficial();
    if (official) {
      return official;
    }
    
    const ours = await this.getMinecraftFromOurs();
    if (ours) {
      return ours;
    }
    
    return null;
  }
  
  private async getMincraftFromOfficial() {
    try {
      const req = await fetch(MiscApiImpl.MINECRAFT_VERSIONS);
      const data = await req.json();
      if (!data || !data.versions) {
        return null;
      }
      
      // Normalize the versions
      const versionData: NormalisedVersion[] = [];
      for (const version of data.versions) {
        // This isn't correct but modpacksch doesn't give us the correct data so we have to normalize it to the same wrong data
        const type = version.type === 'release' ? 'release' : 'snapshot';
        versionData.push({
          name: version.id,
          type
        });
      }
      
      return versionData;
    } catch (e) {
      logger.error("Failed to get Minecraft versions from official", e);
      return null;
    }
  }
  
  private async getMinecraftFromOurs() {
    try {
      const req = await modpackApi.modpacks.getModpack(MiscApiImpl.MINECRAFT_MODPACKS_ID);
      if (!req || !req.versions) {
        return null;
      }
      
      // Sort the data as it's backwards...
      const sortedData = req.versions.sort((a, b) => {
        return b.id - a.id;
      });
      
      // Normalize the versions
      const versionData: NormalisedVersion[] = [];
      for (const version of sortedData) {
        versionData.push({
          name: version.name,
          type: version.type.toLowerCase() === 'release' ? 'release' : 'snapshot'
        });
      }
      
      return versionData;
    } catch (e) {
      logger.error("Failed to get Minecraft versions from ours", e);
      return null;
    }
  }
}

export const MiscApi = new MiscApiImpl();