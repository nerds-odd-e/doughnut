import { describe, it, expect, beforeEach } from "vitest"
import { h } from "vue"
import { createMemoryHistory, createRouter, createWebHistory } from "vue-router"
import type { RouteRecordRaw } from "vue-router"
import { dummyRouteRecordsFromMetadata } from "@/routes/dummyRouteRecords"
import { noteShowHref, noteShowLocation } from "@/routes/noteShowLocation"
import routes from "@/routes/routes"

function findRouteRecordByName(
  routeList: RouteRecordRaw[],
  name: string
): RouteRecordRaw | undefined {
  for (const r of routeList) {
    if (r.name === name) {
      return r
    }
    if (r.children) {
      const found = findRouteRecordByName(r.children, name)
      if (found) {
        return found
      }
    }
  }
  return
}

function expectNoteShowProps(
  route: { params: Record<string, unknown> },
  noteId: number
) {
  const meta = findRouteRecordByName(routes, "noteShow")
  expect(meta).toBeDefined()
  expect(typeof meta!.props).toBe("function")
  expect((meta!.props as (r: typeof route) => unknown)(route)).toEqual({
    noteId,
  })
}

/** Absorbs otherwise-unmatched URLs so legacy-path tests do not trigger Vue Router warnings. */
const testCatchAll: RouteRecordRaw = {
  path: "/:pathMatch(.*)*",
  name: "testCatchAll",
  component: { render: () => h("div") },
}

describe("routes", () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    router = createRouter({
      history: createWebHistory(),
      routes: [...routes, testCatchAll],
    })
  })

  describe("noteShow route", () => {
    it("matches /n:noteId and passes noteId prop", async () => {
      await router.push("/n123")

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShow")
      expect(route.params.noteId).toBe("123")
      expectNoteShowProps(route, 123)
    })

    it("redirects legacy /d/n/:noteId to /n:noteId", async () => {
      await router.push("/d/n/888")

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShow")
      expect(route.path).toBe("/n888")
      expect(route.params.noteId).toBe("888")
    })

    it("redirects legacy /n/:noteId to /n:noteId", async () => {
      await router.push("/n/888")

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShow")
      expect(route.path).toBe("/n888")
      expect(route.params.noteId).toBe("888")
    })

    it("compiles noteShowHref from the named location", () => {
      expect(noteShowHref(123)).toBe(router.resolve(noteShowLocation(123)).href)
    })

    it("navigates by name with noteId param", async () => {
      await router.push({
        name: "noteShow",
        params: {
          noteId: "456",
        },
      })

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShow")
      expect(route.params.noteId).toBe("456")
      expectNoteShowProps(route, 456)
    })

    it("does not absorb legacy slash paths under notebooks", async () => {
      await router.push("/d/notebooks/42/notes/a/b")

      expect(router.currentRoute.value.name).not.toBe("noteShow")
      expect(
        router.currentRoute.value.matched.some((r) => r.name === "noteShow")
      ).toBe(false)
    })
  })

  describe("dummy metadata records", () => {
    function dummyRouter() {
      return createRouter({
        history: createMemoryHistory(),
        routes: dummyRouteRecordsFromMetadata,
      })
    }

    it.each([
      { name: "failureReport", params: { failureReportId: "1" } },
      { name: "settingsGeneral" },
      { name: "settingsRecent" },
      { name: "settingsAccessTokens" },
      { name: "settingsRecallStats" },
    ] as const)("compile the same $name path as production", (location) => {
      expect(dummyRouter().resolve(location).path).toBe(
        router.resolve(location).path
      )
    })
  })

  describe("notebookPage route", () => {
    it("does not match legacy /d/notebooks/:id/edit URL", () => {
      const resolved = router.resolve("/d/notebooks/42/edit")
      expect(resolved.matched.some((r) => r.name === "notebookPage")).toBe(
        false
      )
    })
  })
})
