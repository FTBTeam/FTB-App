export interface BaseReply extends BaseRequest {
  requestId?: number;
}

export interface BaseRequest {
  type: string;
}
