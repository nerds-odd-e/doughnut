import { describe, expect, it } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import PropertyAssimilationModes from "@/components/recall/PropertyAssimilationModes.vue"
import helper from "@tests/helpers"

const propertyKey = "summary"

const mountModes = (props: Record<string, unknown> = {}) =>
  helper
    .component(PropertyAssimilationModes)
    .withProps({ propertyKey, showLearningControls: true, ...props })
    .mount()

type Wrapper = ReturnType<typeof mountModes>

const find = (wrapper: Wrapper, selector: string) =>
  wrapper.element.querySelector(selector) as HTMLElement | null

const assimilateButton = (wrapper: Wrapper) =>
  find(wrapper, '[data-test="assimilate-UNDERSTANDING"]')

const statusLink = (wrapper: Wrapper) =>
  find(wrapper, '[data-test="assimilation-status-UNDERSTANDING"]')

const propertyTracker = () =>
  makeMe.aMemoryTracker.id(42).withPropertyKey(propertyKey)

describe("PropertyAssimilationModes", () => {
  it("labels a whole property Understanding and joins its non-submitting Assimilate and Skip controls", () => {
    const wrapper = mountModes()

    expect(
      find(wrapper, '[data-test="mode-label-UNDERSTANDING"]')?.textContent
    ).toBe("Understanding")
    const join = find(wrapper, ".daisy-join")
    const assimilate = join?.querySelector(
      '[data-test="assimilate-UNDERSTANDING"]'
    )
    const skip = join?.querySelector('[data-test="skip"]')
    expect(assimilate?.getAttribute("type")).toBe("button")
    expect(assimilate?.getAttribute("aria-label")).toBe(
      "Assimilate as Understanding"
    )
    expect(skip?.getAttribute("type")).toBe("button")
    expect(skip?.classList).toContain("daisy-btn-ghost")
  })

  it("offers each list value its own Assimilate without a Skip", () => {
    const wrapper = mountModes({ propertyValues: ["run", "walk"] })

    expect(
      [
        ...wrapper.element.querySelectorAll(
          '[data-test="assimilate-UNDERSTANDING"]'
        ),
      ].map((button) => button.getAttribute("aria-label"))
    ).toEqual(["Assimilate run", "Assimilate walk"])
    expect(find(wrapper, '[data-test="skip"]')).toBeNull()
  })

  it("shows Return to sequence instead of Skip when already skipped from the sequence", () => {
    const wrapper = mountModes({ skippedFromAssimilationSequence: true })

    expect(find(wrapper, '[data-test="return-to-sequence"]')).not.toBeNull()
    expect(find(wrapper, '[data-test="skip"]')).toBeNull()
  })

  it("emits skip and returnToSequence", () => {
    const skipWrapper = mountModes()
    find(skipWrapper, '[data-test="skip"]')!.click()
    expect(skipWrapper.emitted("skip")).toEqual([[]])

    const returnWrapper = mountModes({ skippedFromAssimilationSequence: true })
    find(returnWrapper, '[data-test="return-to-sequence"]')!.click()
    expect(returnWrapper.emitted("returnToSequence")).toEqual([[]])
  })

  it("shows a concise tracker link with quiet next-recall metadata in place of Assimilate", () => {
    const nextRecallAt = "2026-09-12T10:00:00.000Z"
    const expectedDate = new Date(nextRecallAt).toLocaleDateString(undefined, {
      day: "numeric",
      month: "short",
    })

    const wrapper = mountModes({
      trackers: [
        propertyTracker().nextRecallAt(nextRecallAt).recallCount(7).please(),
      ],
    })

    const link = statusLink(wrapper)
    expect(link?.textContent?.trim()).toBe("View tracker")
    expect(link?.getAttribute("title")).toBe("Recalled 7 times")
    expect(link?.getAttribute("to")).toBe(
      JSON.stringify({
        name: "memoryTrackerShow",
        params: { memoryTrackerId: 42 },
      })
    )
    expect(wrapper.text()).toContain(`Next ${expectedDate}`)
    expect(assimilateButton(wrapper)).toBeNull()
  })

  it("does not show stability anywhere on the row", () => {
    const wrapper = mountModes({
      trackers: [propertyTracker().stability(123.45).please()],
    })

    expect(wrapper.text()).not.toContain("123.45")
  })

  it("treats a removed tracker the same as no tracker", () => {
    const wrapper = mountModes({
      trackers: [propertyTracker().removedFromTracking(true).please()],
    })

    expect(statusLink(wrapper)).toBeNull()
    expect(assimilateButton(wrapper)).not.toBeNull()
  })

  it("resolves note-level and property-scoped trackers independently", () => {
    const wrapper = mountModes({
      trackers: [
        makeMe.aMemoryTracker.id(1).please(),
        propertyTracker().please(),
      ],
    })

    expect(statusLink(wrapper)?.getAttribute("to")).toBe(
      JSON.stringify({
        name: "memoryTrackerShow",
        params: { memoryTrackerId: 42 },
      })
    )
  })
})
