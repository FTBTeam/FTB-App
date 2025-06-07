interface BaseDeviceAuthResponse {
  token_type: string;
  scope: string;
  expires_in: number;
  access_token: string;
  refresh_token: string;
}

export interface DeviceAuthResponse extends BaseDeviceAuthResponse {
	ext_expires_in: number;
}

export interface RefreshTokenResponse extends BaseDeviceAuthResponse {
  id_token: string;
} 

export interface XboxLiveAuthResponse {
	IssueInstant: string;
	NotAfter: string;
	Token: string;
	DisplayClaims: XboxDisplayClaims;
}

export interface Xui {
	uhs: string;
}

export interface XboxDisplayClaims {
	xui: Xui[];
}

export interface MinecraftAuthResponse {
	username: string;
	roles: any[];
	metadata: object;
	access_token: string;
	expires_in: number;
	token_type: string;
}

export interface EntitlementsResponse {
	items: EntitlementItem[];
	signature: string;
	keyId: string;
	requestId: string;
	errors: any[];
}

export interface EntitlementItem {
	name: string;
	source: string;
}

export interface MinecraftProfileFromApi {
	id: string;
	name: string;
	skins: MinecraftProfileSkins[];
	capes: MinecraftProfileCapes[];
	profileActions: object;
}

export interface MinecraftProfileSkins {
	id: string;
	state: string;
	url: string;
	textureKey: string;
	variant: string;
	alias: string;
}

export interface MinecraftProfileCapes {
	id: string;
	state: string;
	url: string;
	alias: string;
}

export type DeviceCodeHolder = {
  device_code: string;
  user_code: string;
  verification_uri: string;
  expires_in: number;
  verification_uri_complete?: string;
  interval?: number;
}

export type LoadCodeReturn = DeviceCodeHolder | { error: string };

export type CheckForCodeReturn = { pass: true, pollingResult?: LastPollingResult } | { error: string, pollingResult?: LastPollingResult } | { data: any };

export type OnResultReturn = true | {
  error: string;
}

export type LastPollingResult = {
  code: number;
  codes?: number[];
  message: string;
  data?: any;
}