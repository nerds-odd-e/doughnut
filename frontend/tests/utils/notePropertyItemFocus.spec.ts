import { describe, expect, it } from "vitest"
import { propertyItemIndex } from "@/utils/noteProperties"

describe("property item occurrence identity", () => {
  it.each([
    { items: ["walk", "run", "jog"], value: "run", occurrence: 1, index: 1 },
    { items: ["jog", "walk", "run"], value: "run", occurrence: 1, index: 2 },
    { items: ["run", "run", "walk"], value: "run", occurrence: 1, index: 0 },
    { items: ["run", "run", "walk"], value: "run", occurrence: 2, index: 1 },
    { items: ["", " run ", "run"], value: "", occurrence: 1, index: 0 },
    { items: ["", " run ", "run"], value: " run ", occurrence: 1, index: 1 },
    { items: ["", " run ", "run"], value: "run", occurrence: 1, index: 2 },
    { items: ["walk", "jog"], value: "run", occurrence: 1, index: -1 },
    { items: ["run", "walk"], value: "run", occurrence: 2, index: -1 },
  ])(
    "resolves $value occurrence $occurrence in $items to $index",
    ({ items, value, occurrence, index }) => {
      expect(propertyItemIndex(items, { value, occurrence })).toBe(index)
    }
  )
})
