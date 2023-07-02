import { inject } from "vue";
import apiCollection from "./apiCollection";
import ManagedApi from "./ManagedApi";

export function withLoadingApi(managedApi: ManagedApi) {
  return {
    get managedApi(): ManagedApi {
      return managedApi;
    },
    get api(): ReturnType<typeof apiCollection> {
      return apiCollection(this.managedApi);
    },
    get silentApi(): ReturnType<typeof apiCollection> {
      return apiCollection(this.managedApi.silent);
    },
  };
}

export default function useLoadingApi1() {
  const ma = inject("managedApi");
  if (!(ma instanceof ManagedApi)) {
    throw new Error(`ManagedApi not found, got ${ma}`);
  }
  return withLoadingApi(ma);
}
