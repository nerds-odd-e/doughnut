import useStore from "../store/pinia_store";
import createStoredApi from  "./createStoredApi";
import useLoadingApi from "./useLoadingApi";

export default function(loadingOptions={initalLoading: false, hasFormError: false}) {
    const piniaStore = useStore();
    return {
      ...useLoadingApi(loadingOptions),
      piniaStore,
      storedApi(options={}) { return createStoredApi(this, options) }
    }
}