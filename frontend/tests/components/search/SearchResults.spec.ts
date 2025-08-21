import SearchResults from "@/components/search/SearchResults.vue"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"

describe("SearchResults.vue", () => {
  it("makes second API call for same trimmed key and keeps rendering", async () => {
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

    // second debounced schedules another API call for same trimmed key
    // ensure watchers flush
    await nextTick()
    vi.advanceTimersByTime(600)
    await flushPromises()

    expect(
      helper.managedApi.restSearchController.searchForLinkTarget
    ).toHaveBeenCalledTimes(2)

    vi.useRealTimers()
  })

  it("merges unique results and sorts by ascending distance", async () => {
    vi.useFakeTimers()

    const r = (id: number, d?: number) =>
      ({
        noteTopology: { id, title: `N${id}` },
        distance: d,
      }) as unknown as object

    const firstBatch = [r(2, 0.4), r(1, 0.2)] as Array<unknown>
    const secondBatch = [r(1, 0.1), r(3, 0.8)] as Array<unknown>

    const mock = vitest
      .fn()
      .mockResolvedValueOnce(firstBatch)
      .mockResolvedValueOnce(secondBatch)

    helper.managedApi.restSearchController.searchForLinkTarget = mock

    const wrapper = helper
      .component(SearchResults)
      .withProps({ inputSearchKey: "x", isDropdown: true })
      .mount()

    // first batch
    vi.advanceTimersByTime(600)
    await flushPromises()

    // second batch for same trimmed key; ensure scheduler runs
    await wrapper.setProps({ inputSearchKey: "x " })
    // ensure watchers flush
    await nextTick()
    await flushPromises()
    vi.advanceTimersByTime(600)
    await flushPromises()

    // Ensure second call happened
    expect(mock).toHaveBeenCalledTimes(2)

    // After second call, results should be merged and ordered by distance
    // We expect ids in order of ascending distance after merge: id=1 (0.1), id=2 (0.4), id=3 (0.8)
    // Find all router links and parse ids from href-like JSON in stub
    const links = wrapper.findAll(".router-link")
    const ids = links.map((a) => {
      const to = a.attributes("to") ?? "{}"
      try {
        const parsed = JSON.parse(to)
        return parsed.params.noteId as number
      } catch {
        return undefined
      }
    })

    expect(ids.filter((x) => x !== undefined)).toEqual([1, 2, 3])

    vi.useRealTimers()
  })
})
