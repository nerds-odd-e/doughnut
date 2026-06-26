import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteMoreOptionsActions from "@/components/notes/widgets/NoteMoreOptionsActions.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import { screen } from "@testing-library/vue"

const aiMarkdownStub = { markdown: "# AI context\n\nHello **world**." }

function dispatchNoteExportShortcut() {
  document.dispatchEvent(
    new KeyboardEvent("keydown", {
      key: "e",
      code: "KeyE",
      bubbles: true,
      cancelable: true,
    })
  )
}

describe("NoteMoreOptionsActions keyboard shortcut", () => {
  const note = makeMe.aNote.please()

  beforeEach(() => {
    mockSdkService(NoteController, "getAiContextMarkdown", aiMarkdownStub)
  })

  afterEach(() => {
    document.body.innerHTML = ""
  })

  it.each([
    "toolbar",
    "menu",
  ] as const)("opens the export dialog when e is pressed (layout=%s)", async (layout) => {
    helper
      .component(NoteMoreOptionsActions)
      .withRouter()
      .withProps({ note, layout })
      .mount({ attachTo: document.body })

    await flushPromises()
    expect(document.querySelector("dialog")).toBeNull()

    dispatchNoteExportShortcut()
    await flushPromises()

    expect(await screen.findByText("Export Note Data")).toBeInTheDocument()
    expect((document.querySelector("dialog") as HTMLDialogElement)?.open).toBe(
      true
    )
  })
})
