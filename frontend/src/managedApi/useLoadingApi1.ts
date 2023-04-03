import { inject } from "vue";
import apiCollection from "./apiCollection";
import ManagedApi from "./ManagedApi";

export default function useLoadingApi1() {
  return {
    get managedApi(): ManagedApi {
      const ma = inject("managedApi");
      if (!(ma instanceof ManagedApi)) {
        throw new Error(`ManagedApi not found, got ${ma}`);
      }
      return ma;
    },
    get api(): ReturnType<typeof apiCollection> {
      return apiCollection(this.managedApi);
    },
  };
}
