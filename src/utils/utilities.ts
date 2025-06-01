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