import apiCollection from "./apiCollection";
import ManagedApi from "./ManagedApi";

export default function useLoadingApi({ skipLoading = false } = {}) {
  return {
    get managedApi() {
      return new ManagedApi({ skipLoading });
    },
    get api(): ReturnType<typeof apiCollection> {
      return apiCollection(this.managedApi);
    },
  };
}
