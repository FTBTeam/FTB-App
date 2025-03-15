import { onMounted, onUnmounted } from 'vue';

export function useAttachDomEvent<E>(event: string, handler: (event: E) => void | Promise<void>) {
  onMounted(() => {
    document.addEventListener(event, handler)
  })
  
  onUnmounted(() => {
    document.removeEventListener(event, handler)
  })
}