import { describe, it, expect, beforeEach } from "vitest"
import {
  resetAssimilationViewForTests,
  useAssimilationView,
} from "@/composables/useAssimilationView"

describe("useAssimilationView", () => {
  beforeEach(() => {
    resetAssimilationViewForTests()
  })

  it("requestOnFor turns settings on for the given note", () => {
    const { requestOnFor, showAssimilationSettings, pendingOnForNoteId } =
      useAssimilationView()

    requestOnFor(5)

    expect(showAssimilationSettings.value).toBe(true)
    expect(pendingOnForNoteId.value).toBe(5)
  })

  it("resetForNote shows settings only when pending matches the note", () => {
    const { requestOnFor, resetForNote, showAssimilationSettings } =
      useAssimilationView()

    requestOnFor(5)
    resetForNote(5)
    expect(showAssimilationSettings.value).toBe(true)

    resetForNote(7)
    expect(showAssimilationSettings.value).toBe(false)
  })

  it("resetForNote hides settings when pending is null", () => {
    const { resetForNote, showAssimilationSettings } = useAssimilationView()

    resetForNote(5)
    expect(showAssimilationSettings.value).toBe(false)
  })

  it("toggle turns settings on for a note when off", () => {
    const { toggle, showAssimilationSettings, pendingOnForNoteId } =
      useAssimilationView()

    toggle(3)

    expect(showAssimilationSettings.value).toBe(true)
    expect(pendingOnForNoteId.value).toBe(3)
  })

  it("toggle turns settings off when already on for the same note", () => {
    const {
      requestOnFor,
      toggle,
      showAssimilationSettings,
      pendingOnForNoteId,
    } = useAssimilationView()

    requestOnFor(3)
    toggle(3)

    expect(showAssimilationSettings.value).toBe(false)
    expect(pendingOnForNoteId.value).toBe(null)
  })

  it("isOnForNote is true only when settings are on for that note", () => {
    const { requestOnFor, isOnForNote } = useAssimilationView()

    requestOnFor(5)

    expect(isOnForNote(5)).toBe(true)
    expect(isOnForNote(7)).toBe(false)
  })
})
