import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { fireEvent, screen } from "@testing-library/vue"
import { mockSdkService } from "@tests/helpers"
import { mountSoftKeyboardPrimer } from "@tests/helpers/softKeyboardPrimerTestSupport"
import { describe, expect, it, vi } from "vitest"
import { flushPromises } from "@vue/test-utils"
import {
  openDonutMenu,
  searchField,
  setupDonutMenuTests,
} from "./donutMenuTestSupport"

vi.mock("@/managedApi/AiReplyEventSource", async () => {
  const { aiReplyEventSourceMockExports } = await import("./menuDataMocks")
  return aiReplyEventSourceMockExports()
})
setupDonutMenuTests()

describe("Donut menu search focus handoff", () => {
  it("hands focus to the search input in the opening update", async () => {
    mockSdkService(NoteController, "getRecentNotes", [])
    mountSoftKeyboardPrimer()
    await openDonutMenu()
    vi.spyOn(window, "requestAnimationFrame").mockReturnValue(1)
    const dialogFocus: HTMLElement[] = []
    const recordFocus = (event: FocusEvent) => {
      const target = event.target
      if (target instanceof HTMLElement && target.closest("dialog")) {
        dialogFocus.push(target)
      }
    }
    document.addEventListener("focus", recordFocus, true)

    try {
      await fireEvent.click(screen.getByLabelText("Open search"))
      await flushPromises()
    } finally {
      document.removeEventListener("focus", recordFocus, true)
    }
    expect(dialogFocus[0]).toBe(searchField())
    expect(document.activeElement).toBe(searchField())
  })
})
