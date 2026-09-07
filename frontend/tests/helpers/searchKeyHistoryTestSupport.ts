import { SEARCH_KEY_HISTORY_KEY } from "@/utils/searchKeyHistory"

export function seedSearchKeyHistory(keys: unknown) {
  seedEncodedSearchKeyHistory(encodeURIComponent(JSON.stringify(keys)))
}

export function seedEncodedSearchKeyHistory(encoded: string) {
  document.cookie = `${SEARCH_KEY_HISTORY_KEY}=${encoded}; Path=/; SameSite=Lax`
}

export function resetSearchKeyHistory() {
  localStorage.removeItem(SEARCH_KEY_HISTORY_KEY)
  document.cookie = `${SEARCH_KEY_HISTORY_KEY}=; Path=/; Max-Age=0; SameSite=Lax`
}

export function seedLocalSearchKeyHistory(keys: unknown) {
  localStorage.setItem(SEARCH_KEY_HISTORY_KEY, JSON.stringify(keys))
}
