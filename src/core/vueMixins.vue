<script lang="ts">
import {Component, Vue} from 'vue-property-decorator';
import platform, {PlatformType} from '@/utils/interface/electron-overwolf';

@Component
export default class VueMixins extends Vue {
  appPlatform: PlatformType = platform;
  
  openExternal(event: any) {
    event.preventDefault();
    let urlTarget = event.target;
    
    if (event.target.tagName !== 'A') {
      // Get the closest parent link
      urlTarget = event.target.closest('a');
    }
    
    platform.get.utils.openUrl(urlTarget.href);
  }
  
  copyToClipboard(text: string) {
    platform.get.cb.copy(text);
  }
}
</script>
