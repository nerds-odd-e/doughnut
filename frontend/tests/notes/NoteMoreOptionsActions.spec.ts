import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import NoteMoreOptionsActions from "@/components/notes/widgets/NoteMoreOptionsActions.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { wrapWithNoteShortcutScope } from "@tests/helpers/noteShortcutScopeTestHelpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
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
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any> | undefined

  beforeEach(() => {
    usePopups().popups.register({ popupInfo: [] })
    mockSdkService(NoteController, "getAiContextMarkdown", aiMarkdownStub)
    mockSdkService(NoteController, "deleteNote", undefined)
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
    document.body.innerHTML = ""
  })

  function mountActions(layout: "toolbar" | "menu") {
    wrapper = helper
      .component(NoteMoreOptionsActions)
      .withRouter()
      .withProps({ note, layout })
      .mount({ attachTo: document.body })
    return wrapper
  }

  function mountActionsWithStorage(layout: "toolbar" | "menu") {
    wrapper = helper
      .component(NoteMoreOptionsActions)
      .withRouter()
      .withCleanStorage()
      .withProps({ note, layout })
      .mount({ attachTo: document.body })
    return wrapper
  }

  it.each(["toolbar", "menu"] as const)(
    "opens the export dialog when e is pressed (layout=%s)",
    async (layout) => {
      mountActions(layout)

      await flushPromises()
      expect(document.querySelector("dialog")).toBeNull()

      dispatchNoteExportShortcut()
      await flushPromises()

      expect(await screen.findByText("Export Note Data")).toBeInTheDocument()
      expect(
        (document.querySelector("dialog") as HTMLDialogElement)?.open
      ).toBe(true)
    }
  )

  it.each(["toolbar", "menu"] as const)(
    "starts the delete flow when d is pressed (layout=%s)",
    async (layout) => {
      mountActionsWithStorage(layout)

      await flushPromises()
      expect(usePopups().popups.peek()).toHaveLength(0)

      dispatchNoteDeleteShortcut()
      await flushPromises()

      const popups = usePopups().popups.peek()
      expect(popups?.length).toBe(1)
      expect(popups?.[0]?.type).toBe("confirm")
      expect(popups?.[0]?.message).toBe('Confirm to delete "Note1.1.1"?')
    }
  )

  it.each(["toolbar", "menu"] as const)(
    "ignores d while the export dialog is open (layout=%s)",
    async (layout) => {
      mountActionsWithStorage(layout)

      await flushPromises()
      dispatchNoteExportShortcut()
      await flushPromises()
      expect(await screen.findByText("Export Note Data")).toBeInTheDocument()

      dispatchNoteDeleteShortcut()
      await flushPromises()

      expect(usePopups().popups.peek()).toHaveLength(0)
      expect(
        (document.querySelector("dialog") as HTMLDialogElement)?.open
      ).toBe(true)
    }
  )

  it.each(["toolbar", "menu"] as const)(
    "ignores e and d when shortcut scope is inactive (layout=%s)",
    async (layout) => {
      const Harness = wrapWithNoteShortcutScope(
        NoteMoreOptionsActions,
        { note, layout },
        false
      )
      wrapper = helper
        .component(Harness)
        .withRouter()
        .withCleanStorage()
        .mount({ attachTo: document.body })

      await flushPromises()
      dispatchNoteExportShortcut()
      dispatchNoteDeleteShortcut()
      await flushPromises()

      expect(document.querySelector("dialog")).toBeNull()
      expect(usePopups().popups.peek()).toHaveLength(0)
    }
  )

  it.each(["toolbar", "menu"] as const)(
    "advertises e and d shortcut hints in button titles (layout=%s)",
    async (layout) => {
      mountActions(layout)
      await flushPromises()

      expect(
        document.querySelector('button[title="Export... (e)"]')
      ).not.toBeNull()
      expect(
        document.querySelector('button[title="Delete note (d)"]')
      ).not.toBeNull()
    }
  )
})
