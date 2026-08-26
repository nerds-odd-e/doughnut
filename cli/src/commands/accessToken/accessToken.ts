import { UserController, type UserToken } from 'donut-api'
import {
  donutSdkOptions,
  withBackendJson,
} from '../../backendApi/donutBackendClient.js'
import { saveStoredAccessToken } from '../../backendApi/accessTokenStorage.js'

export async function setAccessToken(
  token: string,
  signal?: AbortSignal
): Promise<void> {
  const identity = await withBackendJson<UserToken>(token, () =>
    UserController.getTokenInfo(donutSdkOptions(signal))
  )
  saveStoredAccessToken({ label: identity.label, token })
}
