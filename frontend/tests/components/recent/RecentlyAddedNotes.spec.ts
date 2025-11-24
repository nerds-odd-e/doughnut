import RecentlyAddedNotes from "@/components/recent/RecentlyAddedNotes.vue"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"
import makeMe from "@tests/fixtures/makeMe"
import { NoteController } from "@generated/backend/sdk.gen"

describe("RecentlyAddedNotes", () => {
  const mockNotes = [
    makeMe.aNoteRealm
      .topicConstructor("Note 1")
      .createdAt("2024-01-01T00:00:00Z")
      .updatedAt("2024-01-02T00:00:00Z")
      .please(),
    makeMe.aNoteRealm
      .topicConstructor("Note 2")
      .createdAt("2024-01-03T00:00:00Z")
      .updatedAt("2024-01-04T00:00:00Z")
      .please(),
  ]

  beforeEach(() => {
    vi.spyOn(NoteController, "getRecentNotes").mockResolvedValue({
      data: mockNotes,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
  })

  it("fetches and displays recent notes", async () => {
    const wrapper = helper.component(RecentlyAddedNotes).withRouter().mount()

    await flushPromises()

    // Verify API was called
    expect(NoteController.getRecentNotes).toBeCalled()

    // Verify notes are displayed
    const rows = wrapper.findAll("tbody tr")
    expect(rows).toHaveLength(2)
  })
})
