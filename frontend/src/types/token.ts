export interface Token {
  id: number
  userId: number
  token: string
  createdAt: string // ISO 8601 format
  expiresAt: string // ISO 8601 format
  isRevoked: boolean
}

export interface TokenResponse {
  generated_token: string
}

export interface TokenListResponse {
  tokens: Token[]
}
