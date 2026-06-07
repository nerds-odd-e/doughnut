export function googleClientIdFromEnv(): string {
  return process.env.GOOGLE_CLIENT_ID ?? ''
}

export function googleClientSecretFromEnv(): string {
  return process.env.GOOGLE_CLIENT_SECRET ?? ''
}
