import { HistoryState } from "../store/history";
import useStore from "../store/createPiniaStore";
import storedApiCollection from "./storedApiCollection";
import useLoadingApi from "./useLoadingApi";

class GlobalStorage {
  static storageWrapper = {
    historyState: { noteUndoHistories: [] } as HistoryState,
  };
}

export default function useStoredLoadingApi({
  initalLoading = false,
  hasFormError = false,
  skipLoading = false,
} = {}) {
  const piniaStore = useStore();
  return {
    ...useLoadingApi({ initalLoading, hasFormError, skipLoading }),
    piniaStore,
    get storedApi(): ReturnType<typeof storedApiCollection> {
      return storedApiCollection(
        this.managedApi,
        piniaStore,
        this.globalHistory
      );
    },
    get globalHistory(): HistoryState {
      return GlobalStorage.storageWrapper.historyState;
    },
  };
}
