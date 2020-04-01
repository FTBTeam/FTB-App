
export function debounce(func: () => void, wait: number): () => void {
    let timeout: number | undefined;
    return function() {
         clearTimeout(timeout);
         // @ts-ignore
         timeout = setTimeout(() => {
            // @ts-ignore
             func.apply(this, arguments);
         }, wait);
    };
}

export async function asyncForEach(items: any[], callback: (item: any) => Promise<any>): Promise<any> {
    for (let i = 0; i < items.length; i++) {
        await callback(items[i]);
    }
}
