import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { settleScheduledAutofocus } from "@tests/helpers/focusTargetTestSupport"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import {
  expectPanelClosed,
  launcher,
  mountDonutMenu,
  openDonutMenu,
  recentHeading,
  searchField,
  setupDonutMenuTests,
} from "./donutMenuTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./menuDataMocks")
  return aiReplyEventSourceMockExports()
})

setupDonutMenuTests()

const pressSearchShortcut = async (modifiers: KeyboardEventInit) => {
  const event = new KeyboardEvent("keydown", {
    key: "f",
    code: "KeyF",
    bubbles: true,
    cancelable: true,
    ...modifiers,
  })
  document.dispatchEvent(event)
  await settleScheduledAutofocus()
  return event
}

describe("Donut menu search shortcut", () => {
  beforeEach(() => {
    mockSdkService(NoteController, "getRecentNotes", [
      makeMe.aNoteSearchResult.title("Recent one").please(),
    ])
  })

  it.each([
    { label: "Ctrl+F", modifiers: { ctrlKey: true } },
    { label: "Cmd+F", modifiers: { metaKey: true } },
  ])(
    "opens into search on $label with the field focused",
    async ({ modifiers }) => {
      mountDonutMenu()
      await flushPromises()

      const event = await pressSearchShortcut(modifiers)

      expect(event.defaultPrevented).toBe(true)
      expect(document.activeElement).toBe(searchField())
      expect(recentHeading()).toBeInTheDocument()
    }
  )

  it("focuses the field on Ctrl+F when the panel is already open", async () => {
    await openDonutMenu()
    launcher().focus()

    await pressSearchShortcut({ ctrlKey: true })

    expect(document.activeElement).toBe(searchField())
  })

  it("ignores Ctrl+Shift+F", async () => {
    mountDonutMenu()
    await flushPromises()

    await pressSearchShortcut({ ctrlKey: true, shiftKey: true })

    expectPanelClosed()
  })

  it("ignores Ctrl+F for a visitor", async () => {
    mountDonutMenu(null)
    await flushPromises()

    await pressSearchShortcut({ ctrlKey: true })

    expectPanelClosed()
  })

  it("leaves Ctrl+F to an open modal dialog", async () => {
    mountDonutMenu()
    await flushPromises()
    const dialog = document.createElement("dialog")
    document.body.append(dialog)
    dialog.showModal()

    const event = await pressSearchShortcut({ ctrlKey: true })

    expect(event.defaultPrevented).toBe(false)
    expectPanelClosed()
  })
})
