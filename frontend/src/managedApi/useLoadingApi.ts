import apiCollection from "./apiCollection";
import ManagedApi from "./ManagedApi";

export default function useLoadingApi() {
  return {
    get managedApi() {
      return new ManagedApi();
    },
    get api(): ReturnType<typeof apiCollection> {
      return apiCollection(this.managedApi);
    },
  };
}
