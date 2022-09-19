import { ref } from "vue";
import apiCollection from "./apiCollection";
import ManagedApi from "./ManagedApi";

export default function useLoadingApi({
  hasFormError = false,
  skipLoading = false,
} = {}) {
  const formErrors = hasFormError ? ref({}) : undefined;
  const loadingData = { formErrors };
  return {
    get managedApi() {
      return new ManagedApi(loadingData, { skipLoading });
    },
    get api(): ReturnType<typeof apiCollection> {
      return apiCollection(this.managedApi);
    },
    ...loadingData,
  };
}
