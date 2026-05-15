import {
  NoteController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import type { User } from "@generated/doughnut-backend-api"
import NoteEditingHistory from "@/store/NoteEditingHistory"
import createNoteStorage from "@/store/createNoteStorage"
import { screen } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { beforeEach, vi, describe, it, expect } from "vitest"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const mockedPush = vi.fn()
vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => ({ path: "/" }),
    useRouter: () => ({
      push: mockedPush,
    }),
  }
})

describe("global bar", () => {
  let noteEditingHistory: NoteEditingHistory
  let user: User

  beforeEach(() => {
    vi.clearAllMocks()
    // Mock services used by SearchResults (used by LinkNoteDialog in GlobalBar)
    mockSdkService(NoteController, "getRecentNotes", [])
    mockSdkService(SearchController, "searchForRelationshipTarget", [])
    mockSdkService(SearchController, "searchForRelationshipTargetWithin", [])
    mockSdkService(SearchController, "semanticSearch", [])
    mockSdkService(SearchController, "semanticSearchWithin", [])
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

  it("opens note search on Ctrl+F when logged in", async () => {
    helper.component(GlobalBar).withCurrentUser(user).render()
    expect(screen.queryByPlaceholderText("Search")).toBeNull()
    window.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "f",
        code: "KeyF",
        ctrlKey: true,
        bubbles: true,
        cancelable: true,
      })
    )
    expect(await screen.findByPlaceholderText("Search")).toBeInTheDocument()
  })

  it("does not open note search on Ctrl+F when logged out", async () => {
    helper.component(GlobalBar).render()
    expect(screen.queryByPlaceholderText("Search")).toBeNull()
    window.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "f",
        code: "KeyF",
        ctrlKey: true,
        bubbles: true,
        cancelable: true,
      })
    )
    expect(screen.queryByPlaceholderText("Search")).toBeNull()
  })
})
