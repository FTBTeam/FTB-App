import { ModalBox } from '@/types';

export interface SocketState {
    firstStart: boolean;
    socket: Socket;
    messages: {[index: string]: (data: any) => void};
    modal: ModalBox | null; 
}

export interface Socket {
    isConnected: boolean;
    message: string;
    reconnectError: boolean;
}
