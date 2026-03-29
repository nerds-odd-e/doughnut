import { UserController, type UserToken } from 'doughnut-api'
import {
  doughnutSdkOptions,
  withBackendClient,
  withBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import {
  appendStoredAccessToken,
  getStoredAccessTokenEntryByLabel,
  removeAccessTokenLocal,
} from '../../backendApi/accessTokenStorage.js'

export {
  getDefaultTokenLabel,
  getStoredAccessTokenLabels,
  removeAccessTokenLocal,
  setDefaultTokenLabel,
} from '../../backendApi/accessTokenStorage.js'

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
