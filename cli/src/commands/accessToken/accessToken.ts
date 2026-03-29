import {
  UserController,
  type TokenConfigDto,
  type UserToken,
} from 'doughnut-api'
import {
  doughnutSdkOptions,
  withBackendClient,
  withBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import {
  appendStoredAccessToken,
  defaultAccessTokenLabel,
  getStoredAccessTokenEntryByLabel,
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

export function removeAccessTokenLocal(label: string): void {
  const config = loadAccessTokenConfig()
  const idx = config.tokens.findIndex((t) => t.label === label)
  if (idx < 0) {
    throw new Error(`No access token stored with label "${label}".`)
  }
  config.tokens.splice(idx, 1)
  if (config.tokens.length === 0) {
    delete config.defaultLabel
  } else if (config.defaultLabel === label) {
    config.defaultLabel = config.tokens[0]!.label
  }
  saveAccessTokenConfig(config)
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

export async function removeAccessTokenCompletely(
  label: string,
  signal?: AbortSignal
): Promise<void> {
  const entry = getStoredAccessTokenEntryByLabel(label)
  if (!entry) {
    throw new Error(`No access token stored with label "${label}".`)
  }
  await withBackendClient(entry.token, () =>
    UserController.revokeToken(doughnutSdkOptions(signal))
  )
  removeAccessTokenLocal(label)
}
