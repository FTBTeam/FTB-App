import {ref} from "vue";
import appPlatform from "@platform";
import {alertController} from "@/core/controllers/alertController.ts";

export function useCopyToClipboard() {
  const copied = ref(false)
  const timeoutRef = ref<number | null>(null);
  
  function copy(text: string) {
    alertController.success("Copied to clipboard!")
    appPlatform.cb.copy(text)
    if (timeoutRef.value !== null) {
      clearTimeout(timeoutRef.value)
    }
    
    timeoutRef.value = setTimeout(() => {
      copied.value = false;
    }, 1000 * 3) as any as number // 3 seconds
  }
  
  return {
    copy,
    copied
  }
}