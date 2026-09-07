import DonutApp from "@/DonutApp.vue"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import { SEARCH_KEY_HISTORY_KEY } from "@/utils/searchKeyHistory"
import {
  CurrentUserInfoController,
  TestabilityRestController,
} from "@generated/donut-backend-api/sdk.gen"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import {
  resetSearchKeyHistory,
  seedEncodedSearchKeyHistory,
  seedLocalSearchKeyHistory,
  seedSearchKeyHistory,
} from "@tests/helpers/searchKeyHistoryTestSupport"
import {
  historyItems,
  openSearchKeyHistoryDropdown,
  renderSearchForm,
  setupSearchFormSdkMocks,
} from "./wiki-link-or-relationship/searchDialogTestSupport"
import { cleanup, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

describe("DonutApp search history migration", () => {
  beforeEach(() => {
    resetSearchKeyHistory()
    mockSdkService(TestabilityRestController, "getFeatureToggle", false)
    setupSearchFormSdkMocks()
  })

  afterEach(() => {
    cleanup()
    teardownGlobalClientForTesting()
    vi.restoreAllMocks()
    resetSearchKeyHistory()
    document.cookie = "unrelated=; Path=/; Max-Age=0"
    localStorage.removeItem("unrelated")
  })

  async function arrive() {
    let cookieAtCurrentUserRequest: string | undefined
    mockSdkService(
      CurrentUserInfoController,
      "currentUserInfo",
      {}
    ).mockImplementation(() => {
      cookieAtCurrentUserRequest = document.cookie
      return Promise.resolve(wrapSdkResponse({}))
    })
    helper.component(DonutApp).withRouter().render()
    await flushPromises()
    expect(screen.getByLabelText("Login via Github")).toBeVisible()
    expect(cookieAtCurrentUserRequest).toBeDefined()
    return cookieAtCurrentUserRequest!
  }

  async function reopenHistory() {
    cleanup()
    teardownGlobalClientForTesting()
    await renderSearchForm({ note: null })
    await openSearchKeyHistoryDropdown()
    return historyItems()
  }

  it("preserves legacy history before requesting the user without opening search", async () => {
    seedSearchKeyHistory(["beta", "alpha"])
    document.cookie = "unrelated=keep; Path=/"
    localStorage.setItem("unrelated", "keep")
    expect(await arrive()).not.toContain(`${SEARCH_KEY_HISTORY_KEY}=`)
    expect(localStorage.getItem(SEARCH_KEY_HISTORY_KEY)).toBe(
      JSON.stringify(["beta", "alpha"])
    )
    expect(await reopenHistory()).toEqual(["beta", "alpha"])
    expect(document.cookie).toContain("unrelated=keep")
    expect(localStorage.getItem("unrelated")).toBe("keep")
  })

  it.each([{ keys: ["newer", "beta"] }, { keys: [] }])(
    "preserves current local history $keys on repeated arrival",
    async ({ keys }) => {
      seedSearchKeyHistory(["beta", "alpha"])
      await arrive()
      cleanup()
      teardownGlobalClientForTesting()
      seedLocalSearchKeyHistory(keys)
      seedSearchKeyHistory(["stale"])
      expect(await arrive()).not.toContain(`${SEARCH_KEY_HISTORY_KEY}=`)
      expect(localStorage.getItem(SEARCH_KEY_HISTORY_KEY)).toBe(
        JSON.stringify(keys)
      )
      expect(await reopenHistory()).toEqual(keys)
    }
  )

  it.each(["SecurityError", "QuotaExceededError"])(
    "retains legacy history when persistence fails with %s",
    async (name) => {
      seedSearchKeyHistory(["legacy"])
      const cookie = document.cookie
      vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
        throw new DOMException("Storage denied", name)
      })
      expect(await arrive()).toBe(cookie)
      expect(localStorage.getItem(SEARCH_KEY_HISTORY_KEY)).toBeNull()
      expect(await reopenHistory()).toEqual(["legacy"])
    }
  )

  it("keeps login available when the storage getter is denied without creating a cookie", async () => {
    vi.spyOn(window, "localStorage", "get").mockImplementation(() => {
      throw new DOMException("Storage denied", "SecurityError")
    })
    expect(await arrive()).not.toContain(`${SEARCH_KEY_HISTORY_KEY}=`)
  })

  it("retains legacy history when the storage getter is denied", async () => {
    seedSearchKeyHistory(["legacy"])
    const cookie = document.cookie
    vi.spyOn(window, "localStorage", "get").mockImplementation(() => {
      throw new DOMException("Storage denied", "SecurityError")
    })
    expect(await arrive()).toBe(cookie)
  })

  it("preserves legacy history when storage reading is denied", async () => {
    seedSearchKeyHistory(["legacy"])
    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new DOMException("Storage denied", "SecurityError")
    })
    expect(await arrive()).not.toContain(`${SEARCH_KEY_HISTORY_KEY}=`)
    vi.restoreAllMocks()
    setupSearchFormSdkMocks()
    expect(await reopenHistory()).toEqual(["legacy"])
  })

  it.each(["%%%bad%%%", "not json", "%7B%7D"])(
    "keeps login available with malformed legacy history %s",
    async (encoded) => {
      seedEncodedSearchKeyHistory(encoded)
      await arrive()
      expect(localStorage.getItem(SEARCH_KEY_HISTORY_KEY)).toBeNull()
    }
  )
})
