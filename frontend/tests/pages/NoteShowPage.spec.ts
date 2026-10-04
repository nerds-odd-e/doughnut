import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import {
  mockNotebookGetForNoteRealm,
  mockSdkService,
  productionRouterAt,
} from "@tests/helpers"
import {
  mainNoteContentEl,
  renderNoteShowPageWithoutSidebar,
} from "@tests/pages/noteShowPageTestSupport"
import { noteShowLocation } from "@/routes/noteShowLocation"
import type { Router } from "vue-router"
import { beforeEach, describe, expect, it } from "vitest"

describe("note show page", () => {
  const noteRealm = makeMe.aNoteRealm.please()
  let router: Router
  let showNoteSpy: ReturnType<typeof mockSdkService>

  beforeEach(async () => {
    router = await productionRouterAt(noteShowLocation(noteRealm.id))
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
