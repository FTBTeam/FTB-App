import { SettingsState } from './modules/settings/types';
import { RootState } from '@/types';

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

export function logVerbose(state: RootState | SettingsState, ...message: any[]) {
    // @ts-ignore
    if (state.settings?.settings === undefined) {
        // @ts-ignore
        if (state.settings?.verbose === true || state.settings?.verbose === 'true') {
            console.log('[DEBUG]', ...message);
        }
    } else {
        // @ts-ignore
        if (state.settings?.settings.verbose === true || state.settings?.settings.verbose === 'true') {
            console.log('[DEBUG]', ...message);
        }
    }
}
