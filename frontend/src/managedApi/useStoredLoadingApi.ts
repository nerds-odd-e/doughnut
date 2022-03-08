import useStore from "../store/pinia_store";
import storedApiCollection from  "./storedApiCollection";
import useLoadingApi from "./useLoadingApi";

export default function(loadingOptions={initalLoading: false, hasFormError: false, skipLoading: false}) {
    const piniaStore = useStore();
    return {
      ...useLoadingApi(loadingOptions),
      piniaStore,
      get storedApi(): any {
         return storedApiCollection(this.managedApi, piniaStore)
      }
    }
}