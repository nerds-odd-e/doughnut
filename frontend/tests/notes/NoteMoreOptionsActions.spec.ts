import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteMoreOptionsActions from "@/components/notes/widgets/NoteMoreOptionsActions.vue"
import usePopups from "@/components/commons/Popups/usePopups"
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

function dispatchNoteDeleteShortcut() {
  document.dispatchEvent(
    new KeyboardEvent("keydown", {
      key: "d",
      code: "KeyD",
      bubbles: true,
      cancelable: true,
    })
  )
}

describe("NoteMoreOptionsActions keyboard shortcut", () => {
  const note = makeMe.aNote.please()

  beforeEach(() => {
    usePopups().popups.register({ popupInfo: [] })
    mockSdkService(NoteController, "getAiContextMarkdown", aiMarkdownStub)
    mockSdkService(NoteController, "deleteNote", undefined)
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

  it.each([
    "toolbar",
    "menu",
  ] as const)("starts the delete flow when d is pressed (layout=%s)", async (layout) => {
    helper
      .component(NoteMoreOptionsActions)
      .withRouter()
      .withCleanStorage()
      .withProps({ note, layout })
      .mount({ attachTo: document.body })

    await flushPromises()
    expect(usePopups().popups.peek()).toHaveLength(0)

    dispatchNoteDeleteShortcut()
    await flushPromises()

    const popups = usePopups().popups.peek()
    expect(popups?.length).toBe(1)
    expect(popups?.[0]?.type).toBe("confirm")
    expect(popups?.[0]?.message).toBe('Confirm to delete "Note1.1.1"?')
  })
})
