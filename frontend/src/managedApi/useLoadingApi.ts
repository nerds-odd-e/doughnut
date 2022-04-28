import { Ref, ref } from "vue";
import apiCollection from "./apiCollection";
import ManagedApi from "./ManagedApi";

export default function useLoadingApi({
  initalLoading = false,
  hasFormError = false,
  skipLoading = false,
} = {}) {
  const loading: Ref<boolean> = ref(initalLoading);
  const formErrors = hasFormError ? ref({}) : undefined;
  const loadingData = { loading, formErrors };
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
