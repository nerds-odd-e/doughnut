import { describe, it, expect, beforeEach } from "vitest"
import { useAssimilationView } from "@/composables/useAssimilationView"

describe("useAssimilationView", () => {
  beforeEach(() => {
    useAssimilationView().dismiss()
  })

  it("openForNote turns settings on for the given note", () => {
    const { openForNote, showAssimilationSettings, targetNoteId } =
      useAssimilationView()

    openForNote(5)

    expect(showAssimilationSettings.value).toBe(true)
    expect(targetNoteId.value).toBe(5)
  })

  it("resetForNote shows settings only when pending matches the note", () => {
    const { openForNote, resetForNote, showAssimilationSettings } =
      useAssimilationView()

    openForNote(5)
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
    const { toggle, showAssimilationSettings, targetNoteId } =
      useAssimilationView()

    toggle(3)

    expect(showAssimilationSettings.value).toBe(true)
    expect(targetNoteId.value).toBe(3)
  })

  it("toggle turns settings off when already on for the same note", () => {
    const { openForNote, toggle, showAssimilationSettings, targetNoteId } =
      useAssimilationView()

    openForNote(3)
    toggle(3)

    expect(showAssimilationSettings.value).toBe(false)
    expect(targetNoteId.value).toBe(null)
  })

  it("isOpenForNote is true only when settings are on for that note", () => {
    const { openForNote, isOpenForNote } = useAssimilationView()

    openForNote(5)

    expect(isOpenForNote(5)).toBe(true)
    expect(isOpenForNote(7)).toBe(false)
  })
})
