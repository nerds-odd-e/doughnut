import { inject } from "vue"
import ManagedApi from "./ManagedApi"

export function withLoadingApi(managedApi: ManagedApi) {
  return {
    get managedApi(): ManagedApi {
      return managedApi
    },
  }
}

export default function useLoadingApi() {
  const ma = inject("managedApi")
  if (!(ma instanceof ManagedApi)) {
    throw new Error(`ManagedApi not found, got ${ma}`)
  }
  return withLoadingApi(ma)
}
