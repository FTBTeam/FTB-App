export function debounce(func: () => void, wait: number) {
  let timeout: NodeJS.Timeout | undefined;
  
  function stop() {
    if (!timeout) {
      return;
    }
    
    clearTimeout(timeout);
  }
  
  function run() {
    clearTimeout(timeout);

    timeout = setTimeout(() => {
      func();
    }, wait);
  }
  
  return {
    run,
    stop
  }
}

export function clamp(num: number, min: number, max: number): number {
  return Math.min(Math.max(num, min), max);
}