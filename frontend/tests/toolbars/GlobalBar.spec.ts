import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import type { User } from "@generated/backend"
import NoteEditingHistory from "@/store/NoteEditingHistory"
import createNoteStorage from "@/store/createNoteStorage"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { beforeEach, vi } from "vitest"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRoute: () => ({ path: "/" }),
  useRouter: () => ({
    push: mockedPush,
  }),
}))

describe("global bar", () => {
  let noteEditingHistory: NoteEditingHistory
  let user: User

  beforeEach(() => {
    vi.clearAllMocks()
    // Mock services used by SearchResults (used by LinkNoteDialog in GlobalBar)
    mockSdkService("getRecentNotes", [])
    mockSdkService("searchForLinkTarget", [])
    mockSdkService("searchForLinkTargetWithin", [])
    mockSdkService("semanticSearch", [])
    mockSdkService("semanticSearchWithin", [])
    user = makeMe.aUser.please()
    noteEditingHistory = new NoteEditingHistory()
    const storageAccessor = useStorageAccessor()
    storageAccessor.value = createNoteStorage(noteEditingHistory)
  })

  it("fetch API to be called ONCE", async () => {
    helper.component(GlobalBar).withCurrentUser(user).render()

    expect(screen.queryByTitle("undo")).toBeNull()
  })

  it("show undo when there is something to undo", async () => {
    const note = makeMe.aNote.please()
    noteEditingHistory.deleteNote(note.id)
    helper.component(GlobalBar).withCurrentUser(user).render()

    expect(await screen.findByTitle("undo delete note")).not.toBeDisabled()
  })
})
