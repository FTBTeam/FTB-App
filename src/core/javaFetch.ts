import {getLogger, wsTimeoutWrapperTyped} from '@/utils';
import * as buffer from 'buffer';
import store from '@/modules/store';

type RequestMethod = "GET" | "POST" | "OPTION" | "PATCH" | "PUT" | "DELETE";

interface FetchResponseRaw {
  status: string;
  statusMessage: string;
  statusCode: number;
  statusLine: string;
  headers: Record<string, string[]>;
  body: {
    contentType: string;
    bytes: Buffer;
  }
}

class FetchResponse implements FetchResponseRaw {
  status: string;
  statusMessage: string;
  statusCode: number;
  statusLine: string;
  headers: Record<string, string[]>;
  body: {
    contentType: string;
    bytes: Buffer;
  }
  
  constructor(status: string, statusMessage: string, statusCode: number, statusLine: string, headers: Record<string, string[]>, body: { contentType: string; bytes: Buffer }) {
    this.status = status;
    this.statusMessage = statusMessage;
    this.statusCode = statusCode;
    this.statusLine = statusLine;
    this.headers = headers;
    this.body = body;
  }
  
  public static of(data: FetchResponseRaw) {
    return new FetchResponse(
      data.status,
      data.statusMessage,
      data.statusCode,
      data.statusLine,
      data.headers,
      data.body
    )
  }
  
  public text() {
    return Buffer.from(this.body.bytes as Uint8Array).toString("utf-8")
  }
  
  public json<T>() {
    if (!this.body.contentType.includes("json")) {
      throw new Error(`Unable to extract json data from content type of ${this.body.contentType}`)
    }
    
    return JSON.parse(Buffer.from(this.body.bytes as Uint8Array).toString("utf-8")) as T
  }
  
  public raw(): Buffer {
    return this.body.bytes;
  }
}

export class JavaFetch {
  private readonly logger = getLogger("java-fetch");
  
  private _url;
  private _headers: Record<string, string[]> = {};
  private _body: string | Buffer | null = null;
  private _method: RequestMethod = "GET";
  private _contentType = "application/json"
  private _timeout = 60_000; 
  
  constructor(url: string) {
    this._url = url;
  }
  
  static create(url: string = ""): JavaFetch {
    return new JavaFetch(url);
  }
  
  //#region helper methods
  public static modpacksCh(endpoint: string) {
    return JavaFetch.create(`${process.env.VUE_APP_MODPACK_API}/${store.state.auth?.token?.attributes.modpackschkey ?? "public"}/${endpoint}`)
  }
  //#endregion
  
  public method(method: RequestMethod) {
    this._method = method;
    return this;
  }
  
  public header(key: string, value: string): JavaFetch {
    if (!this._headers[key]) {
      this._headers[key] = [value]
    } else {
      this._headers[key].push(value)
    }
    
    return this;
  }
  
  public contentType(type: string) {
    this._contentType = type;
    return this;
  }
  
  public url(url: string) {
    this._url = url;
    return this;
  }
  
  public timeout(timeoutInSeconds: number) {
    this._timeout = timeoutInSeconds * 100;
    return this;
  }
  
  public body(body: string | object): JavaFetch {
    if (typeof body === "object" || Array.isArray(body)) {
      body = JSON.stringify(body)
    }
    
    this._body = body;
    return this;
  }
  
  async execute<T>(): Promise<FetchResponse | null> {
    const payload: Record<string, any> = {
      type: "webRequest",
      url: this._url,
      method: this._method,
      headers: this._headers,
    };
    
    if (this._body !== null) {
      payload.body = {
        contentType: this._contentType,
        bytes: this._body instanceof Buffer ? this._body : Buffer.from(this._body as string),
      }
    }

    this.logger.info(`Making ${this._method} request to ${this._url}`)
    try {
      const request = await wsTimeoutWrapperTyped<typeof payload, FetchResponseRaw>(payload, this._timeout);
      return FetchResponse.of(request);
    } catch(error) {
      this.logger.info(`Request to ${this._method}::${this._url} failed due to ${error}`)
      return null;
    }
  }
}