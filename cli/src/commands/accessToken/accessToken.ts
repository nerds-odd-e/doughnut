import { UserController, type UserToken } from 'doughnut-api'
import {
  doughnutSdkOptions,
  withBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import { appendStoredAccessToken } from '../../backendApi/accessTokenStorage.js'

export async function addAccessToken(
  token: string,
  signal?: AbortSignal
): Promise<void> {
  const identity = await withBackendJson<UserToken>(token, () =>
    UserController.getTokenInfo(doughnutSdkOptions(signal))
  )
  appendStoredAccessToken({ label: identity.label, token })
}
