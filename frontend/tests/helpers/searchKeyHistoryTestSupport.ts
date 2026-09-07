import { SEARCH_KEY_HISTORY_COOKIE_NAME } from "@/utils/searchKeyHistory"

export function seedSearchKeyHistory(keys: unknown) {
  seedEncodedSearchKeyHistory(encodeURIComponent(JSON.stringify(keys)))
}

export function seedEncodedSearchKeyHistory(encoded: string) {
  document.cookie = `${SEARCH_KEY_HISTORY_COOKIE_NAME}=${encoded}; Path=/; SameSite=Lax`
}

export function resetSearchKeyHistory() {
  document.cookie = `${SEARCH_KEY_HISTORY_COOKIE_NAME}=; Path=/; Max-Age=0; SameSite=Lax`
}
