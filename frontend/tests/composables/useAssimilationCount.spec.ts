import { describe, it, expect } from "vitest"
import {
  abbreviateCount,
  assimilationBadgeTitle,
  formatAssimilationBadge,
} from "@/composables/useAssimilationCount"

describe("abbreviateCount", () => {
  it("shows zero as-is", () => {
    expect(abbreviateCount(0)).toBe("0")
  })

  it("shows the last unabbreviated 3-digit value as-is", () => {
    expect(abbreviateCount(999)).toBe("999")
  })

  it("abbreviates 1000 to 1k", () => {
    expect(abbreviateCount(1000)).toBe("1k")
  })

  it("abbreviates a large value with one decimal place", () => {
    expect(abbreviateCount(12400)).toBe("12.4k")
  })
})

describe("formatAssimilationBadge", () => {
  it("combines due and total counts as due/total, each independently abbreviated", () => {
    expect(formatAssimilationBadge(5, 12400)).toBe("5/12.4k")
  })
})

describe("assimilationBadgeTitle", () => {
  it("spells out the unabbreviated due and total counts", () => {
    expect(assimilationBadgeTitle(5, 128)).toBe(
      "5 due today, 128 total unassimilated"
    )
  })
})
