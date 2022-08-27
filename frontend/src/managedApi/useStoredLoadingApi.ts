import { HistoryWriter } from "../store/history";
import ManagedApi from "./ManagedApi";
import storedApiCollection from "./storedApiCollection";

export default function useStoredLoadingApi({
  historyWriter: undoHistory = undefined as HistoryWriter | undefined,
} = {}) {
  return {
    get storedApi(): ReturnType<typeof storedApiCollection> {
      return storedApiCollection(undoHistory, new ManagedApi(undefined));
    },
  };
}
