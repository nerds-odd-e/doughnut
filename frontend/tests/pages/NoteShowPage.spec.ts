import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteShowPageWithNotebookSidebarLayout from "@tests/fixtures/NoteShowPageWithNotebookSidebarLayout.vue"
import { within } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockNotebookGetForNoteRealm,
  mockSdkService,
} from "@tests/helpers"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { createNoteShowPageRouter } from "@tests/pages/noteShowPageTestSupport"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"

describe("note show page", () => {
  const noteRealm = makeMe.aNoteRealm.please()
  let router: ReturnType<typeof createNoteShowPageRouter>

  beforeEach(() => {
    router = createNoteShowPageRouter()
    mockSdkService(NoteController, "showNote", noteRealm)
    mockNotebookGetForNoteRealm(noteRealm, {
      id: 101,
      name: "a circle",
    })
  })

  it("loads note by id from route", async () => {
    const showNoteSpy = mockSdkService(NoteController, "showNote", noteRealm)

    helper
      .component(NoteShowPageWithNotebookSidebarLayout)
      .withCleanStorage()
      .withProps({ noteId: noteRealm.id })
      .withRouter(router)
      .currentRoute(noteShowLocation(noteRealm.id))
      .render()

    await flushPromises()
    await vi.waitFor(() => {
      const main = document.getElementById("main-note-content")
      expect(main).not.toBeNull()
      expect(
        within(main as HTMLElement).getByText(noteRealm.note.noteTopology.title)
      ).toBeInTheDocument()
    })

    expect(showNoteSpy).toHaveBeenCalledWith({
      path: { note: noteRealm.id },
    })
  })
})
