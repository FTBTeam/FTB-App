import {HttpMethod} from '@/core/types/commonTypes';
import {MessageRaw, Nullable, sendMessage} from '@/core/websockets/websocketsApi';
import {WebRequestData} from '@/core/types/javaApi';
import {createLogger} from '@/core/logger';
import { constants } from '@/core/constants';

interface FetchResponseRaw {
  status: string;
  statusMessage: string;
  statusCode: number;
  statusLine: string;
  headers: Record<string, string[]>;
  body: {
    contentType: string;
    bytes: Uint8Array;
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
    bytes: Uint8Array;
  }
  
  constructor(status: string, statusMessage: string, statusCode: number, statusLine: string, headers: Record<string, string[]>, body: { contentType: string; bytes: Uint8Array }) {
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
    return new TextDecoder().decode(new Uint8Array(this.body.bytes));
  }
  
  public json<T>() {
    if (!this.body.contentType.includes("json")) {
      throw new Error(`Unable to extract json data from content type of ${this.body.contentType}... Expected application/json\n\nFull response: ${this.text()}`)
    }
    
    return JSON.parse(new TextDecoder().decode(new Uint8Array(this.body.bytes))) as T
  }
  
  public raw(): Uint8Array {
    return this.body.bytes as Uint8Array
  }
}

export class JavaFetch {
  private readonly logger = createLogger("JavaFetch.ts")
  
  private _url;
  private _headers: Record<string, string[]> = {};
  private _body: string | Buffer | null = null;
  private _method: HttpMethod = "GET";
  private _contentType = "application/json"
  private _timeout = 60_000; 
  
  private constructor(url: string) {
    this._url = url;
  }
  
  static create(url: string = ""): JavaFetch {
    return new JavaFetch(url);
  }
  
  //#region helper methods
  public static modpacksCh(endpoint: string) {
    return JavaFetch.create(constants.modpacksApi + "/" + endpoint)
  }
  
  public static modpacksChPrivate(endpoint: string) {
    // TODO: Implement private api key
    return JavaFetch.create(constants.modpacksApi + "/" + endpoint)
  }
  
  //#endregion
  
  public method(method: HttpMethod) {
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
  
  async execute(): Promise<FetchResponse | null> {
    this.logger.debug(`Executing request to ${this._method}::${this._url}`)

    const payload: Nullable<MessageRaw<WebRequestData>, "body"> = {
      url: this._url,
      method: this._method,
      headers: this._headers,
      body: null
    };
    
    if (this._body !== null) {
      const byteArray = Array.from(new TextEncoder().encode(this._body as string));

      payload.body = {
        contentType: this._contentType,
        // Create a byte array of the content body (u8)
        bytes: byteArray
      }
    }
    
    try {
      const request = await sendMessage("webRequest", payload, this._timeout);
      return FetchResponse.of(request);
    } catch(error) {
      this.logger.error(`Request to ${this._method}::${this._url} failed`, error)
      return null;
    }
  }
}