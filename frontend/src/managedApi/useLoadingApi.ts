import { Ref, ref } from "vue";
import apiCollection from "./apiCollection";
import ManagedApi from "./ManagedApi";

export default function useLoadingApi(options = { initalLoading: false, hasFormError: false, skipLoading: false }) {
  const loading: Ref<boolean> = ref(options.initalLoading)
  const formErrors = options.hasFormError ? ref({}) : undefined
  const loadingData = { loading, formErrors }
  return {
    get managedApi() { return new ManagedApi(loadingData, { skipLoading: options.skipLoading }) },
    get api(): any { return apiCollection(this.managedApi) },
    ...loadingData
  };
}
