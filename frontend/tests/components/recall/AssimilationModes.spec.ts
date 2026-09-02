import { describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import AssimilationModes from "@/components/recall/AssimilationModes.vue"
import type { MemoryTracker } from "@generated/donut-backend-api"

function mountModes(props: Record<string, unknown>) {
  return helper.component(AssimilationModes).withProps(props).mount()
}

function assimilateButton(
  wrapper: ReturnType<typeof mountModes>,
  mode: string
) {
  return wrapper.element.querySelector(
    `[data-test="assimilate-${mode}"]`
  ) as HTMLInputElement | null
}

function statusLink(wrapper: ReturnType<typeof mountModes>, mode: string) {
  return wrapper.element.querySelector(
    `[data-test="assimilation-status-${mode}"]`
  ) as HTMLAnchorElement | null
}

describe("AssimilationModes", () => {
  it("shows a direct Assimilate trigger (no dropdown) when a mode has no tracker", async () => {
    const wrapper = mountModes({
      allowedModes: ["UNDERSTANDING", "SPELLING", "COMMISSIONED"],
    })

    expect(assimilateButton(wrapper, "UNDERSTANDING")).not.toBeNull()
    expect(assimilateButton(wrapper, "SPELLING")).not.toBeNull()
    expect(assimilateButton(wrapper, "COMMISSIONED")).not.toBeNull()
    expect(wrapper.element.querySelector(".daisy-dropdown")).toBeNull()
    expect(
      wrapper.element.querySelector('[data-test="assimilate-options-caret"]')
    ).toBeNull()
  })

  it("emits assimilate with the mode-specific request when Assimilate is clicked", async () => {
    const wrapper = mountModes({
      allowedModes: ["SPELLING", "COMMISSIONED"],
      propertyKey: undefined,
    })

    assimilateButton(wrapper, "SPELLING")!.click()
    assimilateButton(wrapper, "COMMISSIONED")!.click()

    expect(wrapper.emitted("assimilate")?.[0]).toEqual([
      {
        propertyKey: undefined,
        assimilateAsCommissioned: undefined,
        assimilateAsSpelling: true,
      },
    ])
    expect(wrapper.emitted("assimilate")?.[1]).toEqual([
      {
        propertyKey: undefined,
        assimilateAsCommissioned: true,
        assimilateAsSpelling: undefined,
      },
    ])
  })

  it("shows a link-styled status navigating to the tracker page when an active tracker exists", () => {
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
    expect(link!.textContent?.trim()).toBe(`In recall · next ${expectedDate}`)
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

  it("does not show stability anywhere on the row", () => {
    const tracker = makeMe.aMemoryTracker
      .id(1)
      .nextRecallAt("2026-09-12T10:00:00.000Z")
      .stability(123.45)
      .commissioned()
      .please()

    const wrapper = mountModes({
      allowedModes: ["COMMISSIONED"],
      trackers: [tracker],
    })

    expect(wrapper.text()).not.toContain("123.45")
  })

  it("treats a removed tracker the same as no tracker", () => {
    const tracker = makeMe.aMemoryTracker
      .id(1)
      .commissioned()
      .removedFromTracking(true)
      .please()

    const wrapper = mountModes({
      allowedModes: ["COMMISSIONED"],
      trackers: [tracker],
    })

    expect(statusLink(wrapper, "COMMISSIONED")).toBeNull()
    expect(assimilateButton(wrapper, "COMMISSIONED")).not.toBeNull()
  })

  it("puts the Skip / Return-to-sequence affordance on the MCQ row when allowed", () => {
    const wrapper = mountModes({
      allowedModes: ["UNDERSTANDING", "COMMISSIONED"],
    })

    const commissionedRow = wrapper.element.querySelector(
      '[data-test="assimilation-mode-row-COMMISSIONED"]'
    )
    const understandingRow = wrapper.element.querySelector(
      '[data-test="assimilation-mode-row-UNDERSTANDING"]'
    )

    expect(commissionedRow?.querySelector('[data-test="skip"]')).not.toBeNull()
    expect(understandingRow?.querySelector('[data-test="skip"]')).toBeNull()
  })

  it("falls back the Skip / Return-to-sequence affordance to the Comprehension row when MCQ isn't allowed", () => {
    const wrapper = mountModes({
      allowedModes: ["UNDERSTANDING"],
    })

    const understandingRow = wrapper.element.querySelector(
      '[data-test="assimilation-mode-row-UNDERSTANDING"]'
    )
    expect(understandingRow?.querySelector('[data-test="skip"]')).not.toBeNull()
  })

  it("shows Return to sequence instead of Skip when already skipped from the sequence", () => {
    const wrapper = mountModes({
      allowedModes: ["COMMISSIONED"],
      skippedFromAssimilationSequence: true,
    })

    expect(
      wrapper.element.querySelector('[data-test="return-to-sequence"]')
    ).not.toBeNull()
    expect(wrapper.element.querySelector('[data-test="skip"]')).toBeNull()
  })

  it("emits skip and returnToSequence", () => {
    const skipWrapper = mountModes({ allowedModes: ["COMMISSIONED"] })
    const skipEl = skipWrapper.element.querySelector(
      '[data-test="skip"]'
    ) as HTMLInputElement
    skipEl.click()
    expect(skipWrapper.emitted("skip")).toEqual([[]])

    const returnWrapper = mountModes({
      allowedModes: ["COMMISSIONED"],
      skippedFromAssimilationSequence: true,
    })
    const returnEl = returnWrapper.element.querySelector(
      '[data-test="return-to-sequence"]'
    ) as HTMLInputElement
    returnEl.click()
    expect(returnWrapper.emitted("returnToSequence")).toEqual([[]])
  })

  it.each([
    ["COMMISSIONED", "MCQ"],
    ["SPELLING", "Spelling"],
    ["UNDERSTANDING", "Comprehension"],
  ])("maps %s to the label %s", (mode, label) => {
    const wrapper = mountModes({ allowedModes: [mode] })
    expect(
      wrapper.element
        .querySelector(`[data-test="mode-label-${mode}"]`)
        ?.textContent?.trim()
    ).toBe(label)
  })

  it("resolves note-level and property-scoped trackers independently", () => {
    const noteLevelTracker = makeMe.aMemoryTracker.id(1).please()
    const propertyTracker: MemoryTracker = {
      ...makeMe.aMemoryTracker.withPropertyKey("summary").please(),
      id: 2,
    }

    const wrapper = mountModes({
      allowedModes: ["UNDERSTANDING"],
      propertyKey: "summary",
      trackers: [noteLevelTracker, propertyTracker],
    })

    const link = statusLink(wrapper, "UNDERSTANDING")
    expect(link!.getAttribute("to")).toBe(
      JSON.stringify({
        name: "memoryTrackerShow",
        params: { memoryTrackerId: 2 },
      })
    )
  })
})
