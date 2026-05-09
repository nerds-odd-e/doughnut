import { describe, expect, it } from "vitest"
import { renderTitleFromPattern } from "@/utils/titlePatternRender"

describe("renderTitleFromPattern", () => {
  it("replaces {{date}} with yyyy-MM-dd in UTC from options.now", () => {
    const d = new Date("2024-06-15T12:00:00.000Z")
    expect(renderTitleFromPattern("Note {{date}}", { now: d })).toBe(
      "Note 2024-06-15"
    )
  })

  it("replaces every {{date}} occurrence", () => {
    const d = new Date("2020-01-02T00:00:00.000Z")
    expect(renderTitleFromPattern("{{date}}-{{date}}", { now: d })).toBe(
      "2020-01-02-2020-01-02"
    )
  })

  it("leaves unknown placeholders unchanged", () => {
    const d = new Date("2020-01-02T00:00:00.000Z")
    expect(renderTitleFromPattern("{{foo}}", { now: d })).toBe("{{foo}}")
  })
})
