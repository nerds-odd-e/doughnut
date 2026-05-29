import { describe, it, expect, beforeEach } from "vitest"
import { render } from "@testing-library/vue"
import AssimilationProgressSummary from "@/components/recall/AssimilationProgressSummary.vue"
import { useAssimilationCount } from "@/composables/useAssimilationCount"

const { setAssimilatedCountOfTheDay, setDueCount, setTotalUnassimilatedCount } =
  useAssimilationCount()

beforeEach(() => {
  setAssimilatedCountOfTheDay(undefined)
  setDueCount(undefined)
  setTotalUnassimilatedCount(undefined)
})

describe("AssimilationProgressSummary", () => {
  it("renders today/planned/total from the counts", () => {
    setAssimilatedCountOfTheDay(1)
    setDueCount(1)
    setTotalUnassimilatedCount(2)

    render(AssimilationProgressSummary)

    const summary = document.querySelector(
      '[data-test="assimilation-progress-summary"]'
    )
    expect(summary?.textContent?.replace(/\s/g, "")).toBe("1/2/3")
  })

  it("does not render when counts are not loaded", () => {
    render(AssimilationProgressSummary)

    expect(
      document.querySelector('[data-test="assimilation-progress-summary"]')
    ).toBeNull()
  })
})
