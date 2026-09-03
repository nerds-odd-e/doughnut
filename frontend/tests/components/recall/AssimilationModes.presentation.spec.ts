import { describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import {
  actionSlot,
  assimilateButton,
  mountModes,
  statusLink,
} from "./assimilationModesTestSupport"

describe("AssimilationModes presentation", () => {
  it("shows a concise tracker link with quiet next-recall metadata", () => {
    const nextRecallAt = "2026-09-12T10:00:00.000Z"
    const expectedDate = new Date(nextRecallAt).toLocaleDateString(undefined, {
      day: "numeric",
      month: "short",
    })
    const tracker = makeMe.aMemoryTracker
      .id(42)
      .nextRecallAt(nextRecallAt)
      .recallCount(7)
      .commissioned()
      .please()

    const wrapper = mountModes({
      allowedModes: ["COMMISSIONED"],
      trackers: [tracker],
    })

    const link = statusLink(wrapper, "COMMISSIONED")
    expect(link).not.toBeNull()
    expect(link!.textContent?.trim()).toBe("View tracker")
    expect(wrapper.text()).toContain(`Next ${expectedDate}`)
    expect(wrapper.text()).not.toContain("In recall")
    expect(link!.textContent).not.toContain("7")
    expect(link!.getAttribute("title")).toBe("Recalled 7 times")
    expect(link!.getAttribute("to")).toBe(
      JSON.stringify({
        name: "memoryTrackerShow",
        params: { memoryTrackerId: 42 },
      })
    )
    expect(assimilateButton(wrapper, "COMMISSIONED")).toBeNull()
  })

  it("uses equal-height grid rows and keeps compact rows compact", () => {
    const wrapper = mountModes({
      allowedModes: ["UNDERSTANDING", "SPELLING", "COMMISSIONED"],
    })

    expect(wrapper.classes()).toContain("grid-cols-[max-content_minmax(0,1fr)]")
    for (const mode of ["UNDERSTANDING", "SPELLING", "COMMISSIONED"]) {
      const row = wrapper.element.querySelector(
        `[data-test="assimilation-mode-row-${mode}"]`
      )
      expect(row?.classList).toContain("grid-cols-subgrid")
      expect(row?.classList).toContain("min-h-12")
      expect(actionSlot(wrapper, mode)?.classList).toContain("min-h-12")
    }

    const compactWrapper = mountModes({
      allowedModes: ["UNDERSTANDING"],
      size: "sm",
    })
    const compactRow = compactWrapper.element.querySelector(
      '[data-test="assimilation-mode-row-UNDERSTANDING"]'
    )
    expect(compactRow?.classList).toContain("min-h-8")
    expect(actionSlot(compactWrapper, "UNDERSTANDING")?.classList).toContain(
      "min-h-8"
    )
  })

  it("uses non-submitting controls with mode-specific accessible names", () => {
    const wrapper = mountModes({
      allowedModes: ["UNDERSTANDING", "SPELLING", "COMMISSIONED"],
    })

    for (const [mode, label] of [
      ["UNDERSTANDING", "Understanding"],
      ["SPELLING", "Spelling"],
      ["COMMISSIONED", "Commissioned"],
    ] as const) {
      const button = assimilateButton(wrapper, mode)
      expect(button?.tagName).toBe("BUTTON")
      expect(button?.getAttribute("type")).toBe("button")
      expect(button?.getAttribute("aria-label")).toBe(`Assimilate as ${label}`)
    }

    const skip = wrapper.element.querySelector('[data-test="skip"]')
    expect(skip?.tagName).toBe("BUTTON")
    expect(skip?.getAttribute("type")).toBe("button")
    expect(skip?.classList).toContain("daisy-btn-ghost")
    expect(skip?.classList).not.toContain("daisy-btn-secondary")
  })
})
