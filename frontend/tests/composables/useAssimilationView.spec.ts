import { describe, it, expect, beforeEach } from "vitest"
import { useAssimilationView } from "@/composables/useAssimilationView"

describe("useAssimilationView", () => {
  beforeEach(() => {
    useAssimilationView().dismiss()
  })

  it("openForNote opens settings for that note only", () => {
    const { openForNote, isOpenForNote } = useAssimilationView()

    openForNote(5)

    expect(isOpenForNote(5)).toBe(true)
    expect(isOpenForNote(7)).toBe(false)
  })

  it("resetForNote shows settings only for the target note", () => {
    const { openForNote, resetForNote, isOpenForNote } = useAssimilationView()

    openForNote(5)
    resetForNote(5)
    expect(isOpenForNote(5)).toBe(true)

    resetForNote(7)
    expect(isOpenForNote(5)).toBe(false)
  })

  it("resetForNote hides settings when there is no target note", () => {
    const { resetForNote, isOpenForNote } = useAssimilationView()

    resetForNote(5)
    expect(isOpenForNote(5)).toBe(false)
  })

  it("toggle turns settings on for a note when off", () => {
    const { toggle, isOpenForNote } = useAssimilationView()

    toggle(3)

    expect(isOpenForNote(3)).toBe(true)
  })

  it("toggle turns settings off when already on for the same note, forgetting the note", () => {
    const { openForNote, toggle, resetForNote, isOpenForNote } =
      useAssimilationView()

    openForNote(3)
    toggle(3)
    expect(isOpenForNote(3)).toBe(false)

    resetForNote(3)
    expect(isOpenForNote(3)).toBe(false)
  })
})
