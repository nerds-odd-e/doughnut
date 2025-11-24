import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import type { User } from "@generated/backend"
import NoteEditingHistory from "@/store/NoteEditingHistory"
import createNoteStorage from "@/store/createNoteStorage"
import type { StorageAccessor } from "@/store/createNoteStorage"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { NoteController, SearchController } from "@generated/backend/sdk.gen"
import { beforeEach, vi } from "vitest"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRoute: () => ({ path: "/" }),
  useRouter: () => ({
    push: mockedPush,
  }),
}))

describe("global bar", () => {
  let noteEditingHistory: NoteEditingHistory
  let histories: StorageAccessor
  let user: User

  beforeEach(() => {
    vi.clearAllMocks()
    // Mock services used by SearchResults (used by LinkNoteDialog in GlobalBar)
    vi.spyOn(NoteController, "getRecentNotes").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(SearchController, "searchForLinkTarget").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(SearchController, "searchForLinkTargetWithin").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(SearchController, "semanticSearch").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    vi.spyOn(SearchController, "semanticSearchWithin").mockResolvedValue({
      data: [],
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    user = makeMe.aUser.please()
    noteEditingHistory = new NoteEditingHistory()
    histories = createNoteStorage(noteEditingHistory)
  })

  it("fetch API to be called ONCE", async () => {
    helper
      .component(GlobalBar)
      .withProps({
        storageAccessor: histories,
        user,
        apiStatus: { states: [] },
      })
      .render()

    expect(screen.queryByTitle("undo")).toBeNull()
  })

  it("show undo when there is something to undo", async () => {
    const note = makeMe.aNote.please()
    noteEditingHistory.deleteNote(note.id)
    helper
      .component(GlobalBar)
      .withProps({
        storageAccessor: histories,
        user,
        apiStatus: { states: [] },
      })
      .render()

    expect(await screen.findByTitle("undo delete note")).not.toBeDisabled()
  })
})
