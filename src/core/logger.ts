import dayjs from 'dayjs';

enum LogLevel {
  SILLY = "silly",
  DEBUG = "debug",
  INFO = "info",
  WARN = "warn",
  ERROR = "error",
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
    if (!this.shouldLog(LogLevel.WARN)) {
      return
    }
    
    console.warn(`[${this.now()}][${this.typeToShort(LogLevel.WARN)}][${this.name}]`, ...args)
  }

  error(...args: any[]) {
    if (!this.shouldLog(LogLevel.ERROR)) {
      return
    }
    
    console.error(`[${this.now()}][${this.typeToShort(LogLevel.ERROR)}][${this.name}]`, ...args)
  }

  debug(...args: any[]) {
    if (!this.shouldLog(LogLevel.DEBUG)) {
      return
    }
    
    console.info(`[${this.now()}][${this.typeToShort(LogLevel.DEBUG)}][${this.name}]`, ...args)
  }
  
  info(...args: any[]) {
    if (!this.shouldLog(LogLevel.INFO)) {
      return
    }
    
    console.info(`[${this.now()}][${this.typeToShort(LogLevel.INFO)}][${this.name}]`, ...args)
  }
  
  silly(...args: any[]) {
    if (!this.shouldLog(LogLevel.SILLY)) {
      return
    }
    
    console.log(`[${this.now()}][${this.typeToShort(LogLevel.SILLY)}][${this.name}]`, ...args)
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