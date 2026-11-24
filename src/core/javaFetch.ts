import {HttpMethod} from '@/core/types/commonTypes';
import {MessageRaw, Nullable, sendMessage} from '@/core/websockets/websocketsApi';
import {WebRequestData, WebRequestDataResponse} from '@/core/types/javaApi';
import {createLogger} from '@/core/logger';
import { constants } from '@/core/constants';

type FetchResponseBody = {
  contentType: string;
  bytes: number[];
}

class FetchResponse {
  constructor(
    readonly status: string,
    readonly statusCode: number,
    readonly headers: Record<string, string[]>,
    readonly statusMessage?: string,
    readonly statusLine?: string,
    readonly body?: FetchResponseBody,
  ) {}
  
  public static of(data: WebRequestDataResponse) {
    return new FetchResponse(
      data.status,
      data.statusCode,
      data.headers,
      data.statusMessage,
      data.statusLine,
      data.body
    )
  }
  
  public text() {
    return new TextDecoder().decode(new Uint8Array(this.body?.bytes ?? []));
  }
  
  public json<T>() {
    if (!this.body?.contentType || !this.body?.contentType.includes("json")) {
      throw new Error(`Unable to extract json data from content type of ${this.body?.contentType}... Expected application/json\n\nFull response: ${this.text()}`)
    }
    
    return JSON.parse(new TextDecoder().decode(new Uint8Array(this.body?.bytes ?? []))) as T
  }
  
  public raw(): Uint8Array {
    return new Uint8Array(this.body?.bytes ?? [])
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
  
  public static modpacksApi(endpoint: string) {
    return JavaFetch.create(constants.modpacksApi + "/" + endpoint)
  }

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