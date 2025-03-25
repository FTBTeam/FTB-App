import {ModalBox} from '@/types';

export interface SocketState {
  modal: ModalBox | null;
  exitCallback: ((data: any) => void) | undefined;
}

export interface Socket {
  isConnected: boolean;
  message: string;
  reconnectError: boolean;
}
