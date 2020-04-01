export interface SocketState {
    socket: Socket;
    messages: {[index: string]: (data: any) => void};
}

export interface Socket {
    isConnected: boolean;
    message: string;
    reconnectError: boolean;
}
