import type { OpenAPIConfig } from '@/generated/backend/core/OpenAPI'
import { request } from '@/generated/backend/core/request'
import { ExportControllerService } from '@/generated/backend/services/ExportControllerService'
import type { ApiRequestOptions } from '@/generated/backend/core/ApiRequestOptions'
import type { CancelablePromise } from '@/generated/backend/core/CancelablePromise'
import { BaseHttpRequest } from "@/generated/backend/core/BaseHttpRequest"
import { ImportControllerService } from '@/generated/backend/services/ImportControllerService'

const apiConfig: OpenAPIConfig = {
  BASE: '',
  VERSION: '1.0',
  WITH_CREDENTIALS: true,
  CREDENTIALS: 'include',
  TOKEN: undefined,
  USERNAME: undefined,
  PASSWORD: undefined,
  HEADERS: undefined,
  ENCODE_PATH: undefined,
}

class CustomHttpRequest extends BaseHttpRequest {
  constructor(config: OpenAPIConfig) {
    super(config)
  }

  public request<T>(options: ApiRequestOptions): CancelablePromise<T> {
    return request(this.config, options)
  }
}

const httpRequest = new CustomHttpRequest(apiConfig)
const exportApi = new ExportControllerService(httpRequest)
const importApi = new ImportControllerService(httpRequest)

export const useBackendApi = () => {
  return {
    exportController: exportApi,
    importController: importApi
  }
} 