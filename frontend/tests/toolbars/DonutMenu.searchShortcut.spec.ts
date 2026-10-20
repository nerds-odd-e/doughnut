import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { settleScheduledAutofocus } from "@tests/helpers/focusTargetTestSupport"
import { fireEvent, screen } from "@testing-library/vue"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import {
  expectPanelClosed,
  launcher,
  mountDonutMenu,
  openDonutMenu,
  recentHeading,
  router,
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
  const recentOne = makeMe.aNoteSearchResult.title("Recent one").please()

  beforeEach(() => {
    mockSdkService(NoteController, "getRecentNotes", [recentOne])
  })

  it.each([
    { label: "Ctrl+F", modifiers: { ctrlKey: true } },
    { label: "Cmd+F", modifiers: { metaKey: true } },
  ])(
    "opens the common search dialog on $label with the field focused",
    async ({ modifiers }) => {
      mountDonutMenu()
      await flushPromises()

      const event = await pressSearchShortcut(modifiers)

      expect(event.defaultPrevented).toBe(true)
      const dialog = document.querySelector("dialog")!
      expect(dialog.open).toBe(true)
      expect(dialog).toContainElement(searchField())
      expect(document.activeElement).toBe(searchField())
      expect(recentHeading()).toBeInTheDocument()
    }
  )

  it("opens the search dialog on Ctrl+F when the panel is already open", async () => {
    await openDonutMenu()
    launcher().focus()

    await pressSearchShortcut({ ctrlKey: true })

    expect(document.querySelector("dialog")!.open).toBe(true)
    expectPanelClosed()
    expect(document.activeElement).toBe(searchField())
  })

  it.each(["Escape", "Close"])(
    "closes the search dialog with %s",
    async (control) => {
      mountDonutMenu()
      await pressSearchShortcut({ ctrlKey: true })

      if (control === "Escape") {
        await fireEvent.keyDown(document, { key: "Escape" })
      } else {
        await fireEvent.click(screen.getByLabelText("Close"))
      }
      await flushPromises()

      expect(document.querySelector("dialog")).toBeNull()
    }
  )

  it.each([false, true])(
    "closes on result selection when current note is selected: %s",
    async (alreadyOpen) => {
      const destination = noteShowLocation(recentOne.noteTopology.id)
      if (alreadyOpen) await router.push(destination)
      mountDonutMenu()
      await pressSearchShortcut({ ctrlKey: true })

      await fireEvent.click(screen.getByText("Recent one").closest("a")!)
      await flushPromises()

      expect(router.currentRoute.value.name).toBe(destination.name)
      expect(router.currentRoute.value.params).toEqual(destination.params)
      expect(document.querySelector("dialog")).toBeNull()
    }
  )

  it("ignores Ctrl+Shift+F", async () => {
    mountDonutMenu()
    await flushPromises()

    const event = await pressSearchShortcut({ ctrlKey: true, shiftKey: true })

    expect(event.defaultPrevented).toBe(false)
    expectPanelClosed()
  })

  it("ignores Ctrl+F for a visitor", async () => {
    mountDonutMenu(null)
    await flushPromises()

    const event = await pressSearchShortcut({ ctrlKey: true })

    expect(event.defaultPrevented).toBe(false)
    expect(document.querySelector("dialog")).toBeNull()
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
