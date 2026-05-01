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

  describe("noteShowLegacyNotebookSlug route", () => {
    it("should match /d/notebooks/:notebookId/notes/:path and pass props", async () => {
      await router.push("/d/notebooks/42/notes/journal/2025/daily")

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShowLegacyNotebookSlug")
      expect(route.params.notebookId).toBe("42")
      expect(route.params.noteSlugPath).toBe("journal/2025/daily")

      const legacyRoute = routes.find(
        (r) => r.name === "noteShowLegacyNotebookSlug"
      )
      expect(legacyRoute).toBeDefined()
      if (legacyRoute && typeof legacyRoute.props === "function") {
        expect(legacyRoute.props(route)).toEqual({
          notebookId: 42,
          noteSlugPath: "journal/2025/daily",
        })
      }
    })

    it("should navigate by name with multi-segment noteSlugPath params", async () => {
      await router.push({
        name: "noteShowLegacyNotebookSlug",
        params: {
          notebookId: "42",
          noteSlugPath: "journal/2025/daily",
        },
      })

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShowLegacyNotebookSlug")
      expect(route.params.notebookId).toBe("42")
      expect(route.params.noteSlugPath).toBe("journal/2025/daily")
      const legacyRoute = routes.find(
        (r) => r.name === "noteShowLegacyNotebookSlug"
      )
      expect(legacyRoute).toBeDefined()
      if (legacyRoute && typeof legacyRoute.props === "function") {
        expect(legacyRoute.props(route)).toEqual({
          notebookId: 42,
          noteSlugPath: "journal/2025/daily",
        })
      }
    })

    it("should not match canonical note-id URLs", () => {
      const resolved = router.resolve("/d/n/123")
      expect(
        resolved.matched.some((r) => r.name === "noteShowLegacyNotebookSlug")
      ).toBe(false)
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
