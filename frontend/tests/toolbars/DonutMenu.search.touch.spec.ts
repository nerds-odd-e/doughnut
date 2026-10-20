import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { fireEvent, screen } from "@testing-library/vue"
import { mockSdkService } from "@tests/helpers"
import { settleScheduledAutofocus } from "@tests/helpers/focusTargetTestSupport"
import {
  mountSoftKeyboardPrimer,
  softKeyboardPrimerElement,
} from "@tests/helpers/softKeyboardPrimerTestSupport"
import { describe, expect, it, vi } from "vitest"
import {
  expectPanelClosed,
  openDonutMenu,
  searchField,
  setupDonutMenuTests,
} from "./donutMenuTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./menuDataMocks")
  return aiReplyEventSourceMockExports()
})

setupDonutMenuTests()

describe("Donut menu touch search", () => {
  it.each(["Open search", "Search"])(
    "prepares input synchronously from %s",
    async (label) => {
      expect(window.matchMedia("(pointer: coarse)").matches).toBe(true)
      mockSdkService(NoteController, "getRecentNotes", [])
      mountSoftKeyboardPrimer()
      await openDonutMenu()
      const primerFocused = vi.fn()
      softKeyboardPrimerElement()!.addEventListener("focus", primerFocused)

      const opening = fireEvent.click(screen.getByLabelText(label))
      expect(primerFocused).toHaveBeenCalledOnce()
      await opening
      await settleScheduledAutofocus()

      expectPanelClosed()
      expect(document.activeElement).toBe(searchField())
    }
  )
})
