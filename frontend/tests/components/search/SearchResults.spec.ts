import SearchResults from "@/components/search/SearchResults.vue"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"

describe("SearchResults.vue", () => {
  it("triggers second API call for same trimmed key when context changes", async () => {
    vi.useFakeTimers()

    const result = [
      { noteTopology: { id: 1, title: "Alpha" } },
    ] as unknown as Array<unknown>

    const firstSpy = vitest.fn().mockResolvedValue(result)
    const withinSpy = vitest.fn().mockResolvedValue(result)
    const semanticSpy = vitest.fn().mockResolvedValue([])
    const semanticWithinSpy = vitest.fn().mockResolvedValue([])
    helper.managedApi.restSearchController.searchForLinkTarget = firstSpy
    helper.managedApi.restSearchController.searchForLinkTargetWithin = withinSpy
    helper.managedApi.restSearchController.semanticSearch = semanticSpy
    helper.managedApi.restSearchController.semanticSearchWithin =
      semanticWithinSpy

    const wrapper = helper
      .component(SearchResults)
      .withProps({ inputSearchKey: "a", isDropdown: true })
      .mount()

    // first debounced call
    await nextTick()
    vi.advanceTimersByTime(600)
    await flushPromises()

    // change search context (noteId) and keep same trimmed key (adds trailing space)
    await wrapper.setProps({ noteId: 1, inputSearchKey: "a " })

    // second debounced schedules another API call for same trimmed key
    // ensure watchers flush
    await nextTick()
    vi.advanceTimersByTime(600)
    await flushPromises()

    expect(firstSpy).toHaveBeenCalledTimes(1)
    expect(semanticSpy).toHaveBeenCalledTimes(1)
    expect(withinSpy).toHaveBeenCalledTimes(1)
    expect(semanticWithinSpy).toHaveBeenCalledTimes(1)

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

    const mockTop = vitest.fn().mockResolvedValueOnce(firstBatch)
    const mockWithin = vitest.fn().mockResolvedValueOnce(secondBatch)
    const mockSemanticTop = vitest.fn().mockResolvedValueOnce([])
    const mockSemanticWithin = vitest.fn().mockResolvedValueOnce([])

    helper.managedApi.restSearchController.searchForLinkTarget = mockTop
    helper.managedApi.restSearchController.searchForLinkTargetWithin =
      mockWithin
    helper.managedApi.restSearchController.semanticSearch = mockSemanticTop
    helper.managedApi.restSearchController.semanticSearchWithin =
      mockSemanticWithin

    const wrapper = helper
      .component(SearchResults)
      .withProps({ inputSearchKey: "x", isDropdown: true })
      .mount()

    // first batch
    await nextTick()
    vi.advanceTimersByTime(600)
    await flushPromises()

    // second batch using context change to within (noteId)
    await wrapper.setProps({ noteId: 1, inputSearchKey: "x " })
    // ensure watchers flush
    await nextTick()
    await flushPromises()
    vi.advanceTimersByTime(600)
    await flushPromises()

    // Ensure both endpoints were used once
    expect(mockTop).toHaveBeenCalledTimes(1)
    expect(mockSemanticTop).toHaveBeenCalledTimes(1)
    expect(mockWithin).toHaveBeenCalledTimes(1)
    expect(mockSemanticWithin).toHaveBeenCalledTimes(1)

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
