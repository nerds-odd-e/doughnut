import {
  UserController,
  type TokenConfigDto,
  type UserToken,
} from 'doughnut-api'
import {
  doughnutSdkOptions,
  withBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import {
  appendStoredAccessToken,
  defaultAccessTokenLabel,
  loadAccessTokenConfig,
  saveAccessTokenConfig,
} from '../../backendApi/accessTokenStorage.js'

type AccessTokenLabel = TokenConfigDto['label']

export function getDefaultTokenLabel(): AccessTokenLabel | undefined {
  return defaultAccessTokenLabel(loadAccessTokenConfig())
}

export function setDefaultTokenLabel(label: AccessTokenLabel): void {
  const config = loadAccessTokenConfig()
  if (!config.tokens.some((t) => t.label === label)) {
    throw new Error(`No access token stored with label "${label}".`)
  }
  config.defaultLabel = label
  saveAccessTokenConfig(config)
}

export function getStoredAccessTokenLabels(): string[] {
  return loadAccessTokenConfig().tokens.map((t) => t.label)
}

export async function addAccessToken(
  token: string,
  signal?: AbortSignal
): Promise<void> {
  const identity = await withBackendJson<UserToken>(token, () =>
    UserController.getTokenInfo(doughnutSdkOptions(signal))
  )
  appendStoredAccessToken({ label: identity.label, token })
}
