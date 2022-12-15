import dayjs from 'dayjs';

export class Logger {
  private name: string;

  constructor(name: string) {
    this.name = name;
  }

  info(...data: any[]) {
    this.log('info', data);
  }

  debug(...data: any[]) {
    this.log('debug', data);
  }

  warn(...data: any[]) {
    this.log('warn', data);
  }

  // TODO: support errors
  error(...data: any[]) {
    this.log('error', data);
  }

  // TODO: support errors
  fatal(...data: any[]) {
    this.log('fatal', data);
  }

  private log(type: 'debug' | 'info' | 'warn' | 'error' | 'fatal', ...data: any[]) {
    const message = data[0] ?? '';
    if (data.length) {
      data.shift();
    }

    const messageData = `[${dayjs().format('HH:mm:ss')}] [${this.name.toUpperCase()}/${type.toUpperCase()}] ${message}`;
    if (!data.length) {
      console.log(messageData);
    } else {
      console.log(messageData, data);
    }
  }
}

const loggers = new Map<string, Logger>();

export function getLogger(name: string): Logger {
  if (!loggers.has(name)) {
    loggers.set(name, new Logger(name));
  }

  return loggers.get(name)!;
}
