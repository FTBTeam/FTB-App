import { BaseReply, BaseRequest } from '@/typings/subprocess/shared';

export interface ShareInstance extends BaseRequest {
  uuid: string;
}

export interface ShareInstanceReply extends BaseReply, ShareInstance {
  status: string;
  message: string;
  uuid: string;
  code: string;
}
