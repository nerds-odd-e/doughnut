import { describe, it, expect, beforeEach } from "vitest"
import {
  SEARCH_KEY_HISTORY_COOKIE_NAME,
  appendSearchKeyToHistory,
  clearSearchKeyHistoryCookie,
  readSearchKeyHistory,
} from "@/utils/searchKeyHistoryCookie"

describe("searchKeyHistoryCookie", () => {
  beforeEach(() => {
    clearSearchKeyHistoryCookie()
  })

  it("returns empty list when cookie is absent", () => {
    expect(readSearchKeyHistory()).toEqual([])
  })

  it("returns empty list for corrupt cookie value", () => {
    document.cookie = `${SEARCH_KEY_HISTORY_COOKIE_NAME}=%%%bad%%%`
    expect(readSearchKeyHistory()).toEqual([])
  })

  it("returns empty list when JSON is not an array", () => {
    document.cookie = `${SEARCH_KEY_HISTORY_COOKIE_NAME}=${encodeURIComponent(JSON.stringify({ a: 1 }))}`
    expect(readSearchKeyHistory()).toEqual([])
  })

  it("skips empty and whitespace-only keys", () => {
    appendSearchKeyToHistory("")
    appendSearchKeyToHistory("   ")
    expect(readSearchKeyHistory()).toEqual([])
  })

  it("appends newest first and dedupes MRU", () => {
    appendSearchKeyToHistory("alpha")
    appendSearchKeyToHistory("beta")
    appendSearchKeyToHistory("alpha")
    expect(readSearchKeyHistory()).toEqual(["alpha", "beta"])
  })

  it("trims stored keys", () => {
    appendSearchKeyToHistory("  spaced  ")
    expect(readSearchKeyHistory()).toEqual(["spaced"])
  })

  it("keeps at most 100 distinct keys", () => {
    for (let i = 0; i < 101; i++) {
      appendSearchKeyToHistory(`k${i}`)
    }
    const keys = readSearchKeyHistory()
    expect(keys).toHaveLength(100)
    expect(keys[0]).toBe("k100")
    expect(keys[99]).toBe("k1")
    expect(keys).not.toContain("k0")
  })

  it("truncates very long keys to 512 characters", () => {
    const long = "x".repeat(600)
    appendSearchKeyToHistory(long)
    const keys = readSearchKeyHistory()
    expect(keys).toHaveLength(1)
    expect(keys[0]).toHaveLength(512)
    expect(keys[0]).toBe("x".repeat(512))
  })
})
