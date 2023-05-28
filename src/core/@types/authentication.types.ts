export type AuthenticationCredentialsPayload = {
  iv: string;
  password: string;
  key: string;
} | string;