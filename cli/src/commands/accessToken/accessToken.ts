import { UserController, type UserToken } from 'doughnut-api'
import {
  doughnutSdkOptions,
  withBackendJson,
} from '../../backendApi/doughnutBackendClient.js'
import { saveStoredAccessToken } from '../../backendApi/accessTokenStorage.js'

export async function setAccessToken(
  token: string,
  signal?: AbortSignal
): Promise<void> {
  const identity = await withBackendJson<UserToken>(token, () =>
    UserController.getTokenInfo(doughnutSdkOptions(signal))
  )
  saveStoredAccessToken({ label: identity.label, token })
}
