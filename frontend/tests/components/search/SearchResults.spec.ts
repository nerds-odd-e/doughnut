import SearchResults from "@/components/search/SearchResults.vue"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"

describe("SearchResults.vue", () => {
  it("uses cache for same trimmed key (no second API call)", async () => {
    vi.useFakeTimers()

    const result = [
      { noteTopology: { id: 1, title: "Alpha" } },
    ] as unknown as Array<unknown>

    helper.managedApi.restSearchController.searchForLinkTarget = vitest
      .fn()
      .mockResolvedValue(result)

    const wrapper = helper
      .component(SearchResults)
      .withProps({ inputSearchKey: "a", isDropdown: true })
      .mount()

    // first debounced call
    vi.advanceTimersByTime(600)
    await flushPromises()

    // change to same trimmed key (adds trailing space)
    await wrapper.setProps({ inputSearchKey: "a " })

    // second debounced call should NOT happen if caching/trim logic works
    vi.advanceTimersByTime(600)
    await flushPromises()

    expect(
      helper.managedApi.restSearchController.searchForLinkTarget
    ).toHaveBeenCalledTimes(1)

    vi.useRealTimers()
  })
})
