import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import { within } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockNotebookGetForNoteRealm, mockSdkService } from "@tests/helpers"
import {
  createNoteShowPageRouter,
  renderNoteShowPage,
} from "@tests/pages/noteShowPageTestSupport"
import { beforeEach, describe, expect, it, vi } from "vitest"

describe("note show page", () => {
  const noteRealm = makeMe.aNoteRealm.please()
  let router: ReturnType<typeof createNoteShowPageRouter>
  let showNoteSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    router = createNoteShowPageRouter()
    showNoteSpy = mockSdkService(NoteController, "showNote", noteRealm)
    mockNotebookGetForNoteRealm(noteRealm, { id: 101, name: "a circle" })
  })

  it("loads note by id from route", async () => {
    await renderNoteShowPage(router, noteRealm.id)

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
