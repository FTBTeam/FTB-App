import {Ad} from '@/core/@types/external/metaApi.types';
import {RequiresWs} from '@/core/controllers/RequiresWs';
import {consoleBadButNoLogger} from '@/utils';
import {JavaFetch} from '@/core/javaFetch';
import {sendMessage} from '@/core/websockets/websocketsApi';
import store from '@/modules/store';
import {constants} from '@/core/constants';

type AdsEndpointResponse = {
  ads: Ad[];
  active: string[];
}

/**
 * Controller for the ads provider on Electron. This system is designed to be very defensive and ensure that the ads
 * are always valid and safe to display. We also wrap any video ads in a caching system to ensure that the videos are
 * cached, so they are not continuously downloaded.
 */
class ChAdController extends RequiresWs {
  private static FALLBACK_AD = 'https://dist.creeper.host/FTB2/cdn/app/assets/ch-ad-app.webm';
  private static API_ENDPOINT = `${constants.metaApi}/v1/app/ads/all`;
  
  private static VALID_VIDEO_EXTENSIONS = ['webm', 'mp4'];

  private videoCache: any = {};
  
  private activeAds: Ad[] = [
    {
      id: 'fallback',
      type: 'video',
      asset: ChAdController.FALLBACK_AD,
      link: 'https://go.ftb.team/ch-app',
      priority: 3000
    }
  ];
  
  constructor() {
    super();
  }

  onConnected(): void {
    this.loadAds()
      .catch(e => {
        consoleBadButNoLogger("E", e);
        this.syncToVuex()
      })
    
    setTimeout(() => {
      this.syncToVuex()
    }, 1000 * 60) // If the ads fail to load, sync the fallback ad after 1 minute
  }
  
  async loadAds() {
    // We need to wait for the WS to be ready as the networking and video cache are handled by it
    const req = await JavaFetch.create(ChAdController.API_ENDPOINT)
      .execute();
    
    if (!req) {
      consoleBadButNoLogger("E", "Failed to fetch ads");
      return;
    }
    
    const data: AdsEndpointResponse = await req.json();
    if (!data.ads || !data.active) {
      consoleBadButNoLogger("W", "Ads endpoint returned invalid data");
      return;
    }
    
    await this.processAds(data);
  }

  /**
   * Very defensively checked method to ensure the cache does not get in the way of the ads loading.
   * 
   * This method takes the loaded ads and processes them, caching videos and ensuring the ads are valid.
   */
  private async processAds(data: AdsEndpointResponse) {
    const {ads, active} = data;
    const activeAds = ads.filter(ad => active.includes(ad.id));
    
    if (activeAds.length === 0) {
      consoleBadButNoLogger("W", "No active ads found");
      return;
    }
    
    // Process videos 
    for (const ad of activeAds) {
      if (ad.type !== 'video') continue;
      if (!ad.asset) {
        consoleBadButNoLogger("W", "Video ad has no asset", ad);
        continue;
      }
      
      const ext = ad.asset.split('.').pop();
      if (!ext || !ChAdController.VALID_VIDEO_EXTENSIONS.includes(ext)) {
        consoleBadButNoLogger("W", "Video ad has invalid extension", ad);
        continue;
      }
      
      if (this.videoCache[ad.id]) {
        ad.asset = this.videoCache[ad.id];
        continue;
      }
      
      try {
        const videoReq = await sendMessage('videoCache', {
          url: ad.asset,
          fileName: `${ad.id}.${ext}`,
        })
        
        if (videoReq.location) {
          ad.asset = videoReq.location;
          this.videoCache[ad.id] = videoReq.location;
        }
      } catch (e) {
        consoleBadButNoLogger("W", "Failed to cache video", ad);
      }
    }

    this.activeAds = [...activeAds].sort((a, b) => b.priority - a.priority);
    
    this.syncToVuex();
  }
  
  get getActiveAds() {
    return this.activeAds;
  }
  
  private syncToVuex() {
    store.dispatch('v2/ads/sync', this.getActiveAds, {root: true});
  }
}

export const chAdController = new ChAdController();