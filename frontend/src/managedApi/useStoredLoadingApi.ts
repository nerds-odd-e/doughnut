import { HistoryWriter } from "../store/history";
import storedApiCollection from "./storedApiCollection";

export default function useStoredLoadingApi(historyWriter: HistoryWriter) {
  return {
    get storedApi(): ReturnType<typeof storedApiCollection> {
      return storedApiCollection(historyWriter);
    },
  };
}
