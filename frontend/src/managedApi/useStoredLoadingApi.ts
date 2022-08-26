import { HistoryWriter } from "../store/history";
import storedApiCollection from "./storedApiCollection";
import useLoadingApi from "./useLoadingApi";

export default function useStoredLoadingApi({
  historyWriter: undoHistory = undefined as HistoryWriter | undefined,
  initalLoading = false,
  hasFormError = false,
  skipLoading = false,
} = {}) {
  return {
    ...useLoadingApi({ initalLoading, hasFormError, skipLoading }),
    get storedApi(): ReturnType<typeof storedApiCollection> {
      return storedApiCollection(undoHistory, this.managedApi);
    },
  };
}
