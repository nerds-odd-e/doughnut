import { describe, expect, it } from "vitest"
import { productionRouterAt } from "@tests/helpers"
import {
  notePropertyItemHref,
  notePropertyItemLocation,
  notePropertyItemFromRoute,
  resolveInternalNoteFamilyFromHref,
} from "@/routes/noteShowLocation"
import { isNoteRouteFamily } from "@/routes/noteRouteFamily"

describe("property item destinations", () => {
  it.each([
    ["example of", "run", 1],
    ["a/b?#%", "[[folder/Run|跑]]", 2],
    ["空 白", "", 1],
    ["topic", ".", 1],
    ["topic", "..", 1],
    ["topic", "~run", 1],
    ["topic", " run\n", 1],
    ["topic", "long".repeat(70), 1],
  ] as const)(
    "round trips authored key %s and value %s",
    async (key, value, occurrence) => {
      const item = { value, occurrence }
      const href = notePropertyItemHref(42, key, item)
      const router = await productionRouterAt(
        `${new URL(href, "https://example.test").pathname}?conversation=true`
      )
      expect(router.currentRoute.value).toMatchObject({
        ...notePropertyItemLocation(42, key, item),
        query: { conversation: "true" },
      })
      expect(notePropertyItemFromRoute(router.currentRoute.value)).toEqual(item)
      expect(isNoteRouteFamily(router.currentRoute.value)).toBe(true)
      expect(resolveInternalNoteFamilyFromHref(href)).toEqual({
        noteId: 42,
        propertyKey: key,
        item,
      })
    }
  )
})
