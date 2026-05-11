import { describe, it, expect, beforeEach } from "vitest"
import { h } from "vue"
import { createRouter, createWebHistory } from "vue-router"
import type { RouteRecordRaw } from "vue-router"
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
    it("should match /d/n/:noteId and pass noteId prop", async () => {
      await router.push("/d/n/123")

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShow")
      expect(route.params.noteId).toBe("123")

      const meta = findRouteRecordByName(routes, "noteShow")
      expect(meta).toBeDefined()
      if (meta && typeof meta.props === "function") {
        expect(meta.props(route)).toEqual({
          noteId: 123,
        })
      }
    })

    it("should navigate by name with noteId param", async () => {
      await router.push({
        name: "noteShow",
        params: {
          noteId: "456",
        },
      })

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShow")
      expect(route.params.noteId).toBe("456")
      const meta = findRouteRecordByName(routes, "noteShow")
      expect(meta).toBeDefined()
      if (meta && typeof meta.props === "function") {
        expect(meta.props(route)).toEqual({
          noteId: 456,
        })
      }
    })

    it("does not absorb legacy slash paths under notebooks", async () => {
      await router.push("/d/notebooks/42/notes/a/b")

      expect(router.currentRoute.value.name).not.toBe("noteShow")
      expect(
        router.currentRoute.value.matched.some((r) => r.name === "noteShow")
      ).toBe(false)
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
