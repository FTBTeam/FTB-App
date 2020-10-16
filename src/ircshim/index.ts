
import { Console } from 'console';
import { BrowserWindow } from 'electron';

export default class Client {

    private static mapping = new Map<string, any>();
    
    private win: BrowserWindow | null;

    public constructor(win: BrowserWindow | null) {
        this.win = win;
    }

    public connect(info: ConnectInfo) {
        this.sendWebsocket({type: 'ircConnect', host: info.host, port: info.port, nick: info.nick, realname: info.gecos});
    }

    public say(nick: string, message: string) {
        this.sendWebsocket({type: 'ircSendMessage', nick: nick, message: message});
    }

    public whois(nick: string) {
        // lol
        this.sendWebsocket({type: 'ircWhoisRequest', nick: nick});
    }

    public quit() {
        this.sendWebsocket({type: 'ircQuitRequest'});
    }

    public ctcpRequest(nick: string, ...args: string[]) {
        this.sendWebsocket({type: 'ircCtcpRequest', message: args.concat(" ")})
    }

    public messageReceived(message: any) {
        let type: string = message.type;
        message.type = message.ircType;
        if (Client.mapping.has(type)) {
            Client.mapping.get(type)(message);
        } 
    }

    public on(event: string, functionToCall: any) {
        Client.mapping.set(event, functionToCall);
    }

    private sendWebsocket(data: any) {
        if (this.win != null) {
            this.win.webContents.send('sendWebsocket', data);
        }
    }
}

interface ConnectInfo {host: string; port: number; nick: string; gecos: string; }
