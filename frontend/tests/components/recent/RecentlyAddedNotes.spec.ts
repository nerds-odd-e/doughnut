import RecentlyAddedNotes from "@/components/recent/RecentlyAddedNotes.vue"
import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import type { NoteTopology } from "@generated/backend"

describe("RecentlyAddedNotes", () => {
  const mockNotes: NoteTopology[] = [
    {
      id: 1,
      titleOrPredicate: "Note 1",
      shortDetails: undefined,
      linkType: undefined,
      objectNoteTopology: undefined,
      parentOrSubjectNoteTopology: undefined,
    },
    {
      id: 2,
      titleOrPredicate: "Note 2",
      shortDetails: undefined,
      linkType: undefined,
      objectNoteTopology: undefined,
      parentOrSubjectNoteTopology: undefined,
    },
  ]

  beforeEach(() => {
    mockSdkService("getRecentNotes", mockNotes)
  })

  it("fetches and displays recent notes", async () => {
    const getRecentNotesSpy = mockSdkService("getRecentNotes", mockNotes)
    const wrapper = helper.component(RecentlyAddedNotes).withRouter().mount()

    await flushPromises()

    // Verify API was called
    expect(getRecentNotesSpy).toBeCalled()

    // Verify notes are displayed in Cards component
    const cards = wrapper.findAll('[role="card"]')
    expect(cards).toHaveLength(2)
    expect(wrapper.text()).toContain("Note 1")
    expect(wrapper.text()).toContain("Note 2")
  })
})
