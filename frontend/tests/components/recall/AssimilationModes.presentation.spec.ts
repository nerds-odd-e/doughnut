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

  it("aligns actions in one column after the labels, with equal-height rows, and keeps compact rows compact", () => {
    const modes = ["UNDERSTANDING", "SPELLING", "COMMISSIONED"]
    const wrapper = mountModes(
      { allowedModes: modes },
      { attachTo: document.body }
    )

    const labelRights = modes.map(
      (mode) =>
        wrapper.element
          .querySelector(`[data-test="mode-label-${mode}"]`)!
          .getBoundingClientRect().right
    )
    const actionLefts = modes.map(
      (mode) => actionSlot(wrapper, mode)!.getBoundingClientRect().left
    )
    expect(new Set(actionLefts).size).toBe(1)
    expect(Math.max(...labelRights)).toBeLessThanOrEqual(actionLefts[0]!)
    for (const mode of modes) {
      const row = wrapper.element.querySelector(
        `[data-test="assimilation-mode-row-${mode}"]`
      )
      expect(row?.classList).toContain("grid-cols-subgrid")
      expect(row?.classList).toContain("min-h-12")
      expect(actionSlot(wrapper, mode)?.classList).toContain("min-h-12")
    }

    wrapper.unmount()
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
