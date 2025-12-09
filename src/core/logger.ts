import dayjs from 'dayjs';
import inspect from 'object-inspect';
import log from 'electron-log/renderer';
import platform from '@platform';

enum LogLevel {
  SILLY = "silly",
  DEBUG = "debug",
  INFO = "info",
  WARN = "warn",
  ERROR = "error",
  DEV = "dev",
}

class Logger {
  name: string;
  level: LogLevel = LogLevel.SILLY;

  constructor(options: { level?: LogLevel, source: string }) {
    this.name = options.source
    if (options.level) {
      this.level = options.level
    }
  }

  getSubLogger(options: { source: string, level?: LogLevel }) {
    return new Logger({
      ...options,
      source: `${this.name}::${options.source}`,
    })
  }

  log(...args: any[]) {
    // Just call to info
    this.info(...args)
  }

  warn(...args: any[]) {
    this.internalLog(LogLevel.WARN, ...args)
  }

  error(...args: any[]) {
    this.internalLog(LogLevel.ERROR, ...args)
  }

  debug(...args: any[]) {
    this.internalLog(LogLevel.DEBUG, ...args)
  }
  
  info(...args: any[]) {
    this.internalLog(LogLevel.INFO, ...args)
  }
  
  silly(...args: any[]) {
    this.internalLog(LogLevel.SILLY, ...args)
  }
  
  dd(...args: any[]) {
    this.internalLog(LogLevel.DEV, ...args)
  }
  
  private internalLog(level: LogLevel, ...args: any[]) {
    if (!this.shouldLog(level)) {
      return
    }
    
    let logMethod = this.levelToMethod(level) 

    // Remap the args to use stringify-object
    args = args.map((arg) => {
      if (typeof arg === "object") {
        return `obj(${inspect(arg, {
          depth: 10
        })})`
      }
      
      return arg
    })
    
    logMethod(`[${this.now()}][${this.typeToShort(level)}][${this.name}]`, ...args)
  }
  
  private now() {
    return dayjs().format('HH:mm:ss');
  }
  
  private typeToShort(type: LogLevel) {
    switch (type) {
      case LogLevel.SILLY:
        return "S"
      case LogLevel.DEBUG:
        return "D"
      case LogLevel.INFO:
        return "I"
      case LogLevel.WARN:
        return "W"
      case LogLevel.ERROR:
        return "E"
      case LogLevel.DEV:
        return "DD"
    }
  }
  
  private levelToMethod(level: LogLevel) {
    const isOw = platform.isOverwolf
    if (isOw) {
      return console.log
    }
    
    switch (level) {
      case LogLevel.SILLY:
        return log.debug
      case LogLevel.DEBUG:
        return log.debug
      case LogLevel.INFO:
        return log.log
      case LogLevel.WARN:
        return log.warn
      case LogLevel.ERROR:
        return log.error
      case LogLevel.DEV:
        return log.log
    }
  }
  
  private shouldLog(type: LogLevel) {
    switch (type) {
      case LogLevel.SILLY:
        return this.level === LogLevel.DEBUG || this.level === LogLevel.INFO || this.level === LogLevel.SILLY
      case LogLevel.DEBUG:
        return this.level === LogLevel.DEBUG || this.level === LogLevel.INFO || this.level === LogLevel.SILLY
      case LogLevel.INFO:
        return this.level === LogLevel.DEBUG || this.level === LogLevel.INFO || this.level === LogLevel.SILLY
      case LogLevel.WARN:
        return true
      case LogLevel.ERROR:
        return true
      case LogLevel.DEV:
        return !import.meta.env.PROD
    }
  }
}

const logger = new Logger({
  source: "ftb-app",
})

export function createLogger(source: string, level?: LogLevel) {
  return logger.getSubLogger({
    source,
    level,
  })
}

export {
  logger,
}