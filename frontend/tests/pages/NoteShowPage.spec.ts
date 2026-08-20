import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"
import { mockNotebookGetForNoteRealm, mockSdkService } from "@tests/helpers"
import {
  createNoteShowPageRouter,
  mainNoteContentEl,
  renderNoteShowPageWithoutSidebar,
} from "@tests/pages/noteShowPageTestSupport"
import { beforeEach, describe, expect, it } from "vitest"

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
    await renderNoteShowPageWithoutSidebar(router, noteRealm.id)

    const main = mainNoteContentEl()
    expect(main).not.toBeNull()
    expect(main!.textContent).toContain(noteRealm.note.noteTopology.title)

    expect(showNoteSpy).toHaveBeenCalledWith({
      path: { note: noteRealm.id },
    })
  })
})
