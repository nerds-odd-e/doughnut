import { describe, it, expect, beforeEach } from "vitest"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"

describe("routes", () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    router = createRouter({
      history: createWebHistory(),
      routes,
    })
  })

  describe("noteShow route", () => {
    it("should match /d/n/:noteId and pass noteId prop", async () => {
      await router.push("/d/n/123")

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShow")
      expect(route.params.noteId).toBe("123")

      const meta = routes.find((r) => r.name === "noteShow")
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
      const meta = routes.find((r) => r.name === "noteShow")
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
