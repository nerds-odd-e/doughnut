import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import {
  mockNotebookGetForNoteRealm,
  mockSdkService,
  productionRouterAt,
} from "@tests/helpers"
import {
  renderNoteShowPageWithoutSidebar,
  mainNoteContentEl,
} from "./noteShowPageTestSupport"
import { notePropertyItemHref } from "@/routes/noteShowLocation"
import { expect, it } from "vitest"

it("loads an inbound item href into the real note page with its exact value selected", async () => {
  const realm = makeMe.aNoteRealm
    .content("---\nexample of: [walk, run, jog]\n---\nReadable body.")
    .please()
  mockSdkService(NoteController, "showNote", realm)
  mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
  mockNotebookGetForNoteRealm(realm)
  const router = await productionRouterAt(
    notePropertyItemHref(realm.id, "example of", {
      value: "run",
      occurrence: 1,
    })
  )
  await renderNoteShowPageWithoutSidebar(router, realm.id)
  const selected = mainNoteContentEl()!.querySelector(
    '[data-test="assimilation-mode-row-UNDERSTANDING"][data-property-value-focused="true"]'
  )!
  expect(selected.getAttribute("data-property-value")).toBe("run")
  expect((selected as HTMLElement).checkVisibility()).toBe(true)
  expect(mainNoteContentEl()!.textContent).toContain("Readable body.")
})
