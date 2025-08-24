import { DoughnutApi } from '../../generated/backend/DoughnutApi.js'

export type ApiConfig = {
  apiBaseUrl: string
  authToken?: string
}

export function createDoughnutApi(config: ApiConfig) {
  return new DoughnutApi({
    BASE: config.apiBaseUrl,
    TOKEN: config.authToken,
  })
}
