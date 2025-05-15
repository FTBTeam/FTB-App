export interface JWTIdTokenData {
  exp: number
  iat: number
  auth_time: number
  jti: string
  iss: string
  aud: string[] | string
  sub: string
  typ: string
  azp: string
  sid: string
  at_hash: string
  acr: string
  resource_access: Record<string, Roles>
  email_verified?: boolean
  realm_access: RealmAccess
  name?: string
  preferred_username?: string
  given_name?: string
  family_name?: string
  picture?: string
  email?: string
}

export interface Roles {
  roles: string[]
}
