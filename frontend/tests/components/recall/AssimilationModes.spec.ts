import { describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import type { MemoryTracker } from "@generated/donut-backend-api"
import {
  assimilateButton,
  mountModes,
  statusLink,
} from "./assimilationModesTestSupport"

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

  it("puts the Skip / Return-to-sequence affordance on the Understanding row only", () => {
    const wrapper = mountModes({
      allowedModes: ["UNDERSTANDING", "COMMISSIONED"],
    })

    const commissionedRow = wrapper.element.querySelector(
      '[data-test="assimilation-mode-row-COMMISSIONED"]'
    )
    const understandingRow = wrapper.element.querySelector(
      '[data-test="assimilation-mode-row-UNDERSTANDING"]'
    )

    expect(commissionedRow?.querySelector('[data-test="skip"]')).toBeNull()
    expect(understandingRow?.querySelector('[data-test="skip"]')).not.toBeNull()
  })

  it("groups Assimilate and Skip in a shared daisy-join container when the skip affordance is shown", () => {
    const wrapper = mountModes({
      allowedModes: ["UNDERSTANDING", "COMMISSIONED"],
    })

    const understandingRow = wrapper.element.querySelector(
      '[data-test="assimilation-mode-row-UNDERSTANDING"]'
    )
    const join = understandingRow?.querySelector(".daisy-join")
    expect(join).not.toBeNull()
    expect(
      join?.querySelector('[data-test="assimilate-UNDERSTANDING"]')
    ).not.toBeNull()
    expect(join?.querySelector('[data-test="skip"]')).not.toBeNull()

    const commissionedRow = wrapper.element.querySelector(
      '[data-test="assimilation-mode-row-COMMISSIONED"]'
    )
    expect(commissionedRow?.querySelector(".daisy-join")).toBeNull()
  })

  it("shows Return to sequence instead of Skip when already skipped from the sequence", () => {
    const wrapper = mountModes({
      allowedModes: ["UNDERSTANDING"],
      skippedFromAssimilationSequence: true,
    })

    expect(
      wrapper.element.querySelector('[data-test="return-to-sequence"]')
    ).not.toBeNull()
    expect(wrapper.element.querySelector('[data-test="skip"]')).toBeNull()
  })

  it("emits skip and returnToSequence", () => {
    const skipWrapper = mountModes({ allowedModes: ["UNDERSTANDING"] })
    const skipEl = skipWrapper.element.querySelector(
      '[data-test="skip"]'
    ) as HTMLInputElement
    skipEl.click()
    expect(skipWrapper.emitted("skip")).toEqual([[]])

    const returnWrapper = mountModes({
      allowedModes: ["UNDERSTANDING"],
      skippedFromAssimilationSequence: true,
    })
    const returnEl = returnWrapper.element.querySelector(
      '[data-test="return-to-sequence"]'
    ) as HTMLInputElement
    returnEl.click()
    expect(returnWrapper.emitted("returnToSequence")).toEqual([[]])
  })

  it.each([
    ["COMMISSIONED", "Commissioned"],
    ["SPELLING", "Spelling"],
    ["UNDERSTANDING", "Understanding"],
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
