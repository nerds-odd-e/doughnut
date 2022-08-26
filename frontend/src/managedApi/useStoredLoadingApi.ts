import { HistoryWriter } from "../store/history";
import useStore from "../store/createPiniaStore";
import storedApiCollection from "./storedApiCollection";
import useLoadingApi from "./useLoadingApi";

export default function useStoredLoadingApi({
  undoHistory = undefined as HistoryWriter | undefined,
  initalLoading = false,
  hasFormError = false,
  skipLoading = false,
} = {}) {
  const piniaStore = useStore();
  return {
    ...useLoadingApi({ initalLoading, hasFormError, skipLoading }),
    piniaStore,
    get storedApi(): ReturnType<typeof storedApiCollection> {
      return storedApiCollection(undoHistory, this.managedApi, piniaStore);
    },
  };
}
