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
    it("should match /d/notes/:slug and pass slug prop", async () => {
      await router.push("/d/notes/my-note-slug")

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShow")
      expect(route.params.slug).toBe("my-note-slug")

      const noteShowRoute = routes.find((r) => r.name === "noteShow")
      expect(noteShowRoute).toBeDefined()
      if (noteShowRoute && typeof noteShowRoute.props === "function") {
        expect(noteShowRoute.props(route)).toEqual({
          slug: "my-note-slug",
        })
      }
    })

    it("should match slug that is digits only", async () => {
      await router.push("/d/notes/123")

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShow")
      expect(route.params.slug).toBe("123")

      const noteShowRoute = routes.find((r) => r.name === "noteShow")
      expect(noteShowRoute).toBeDefined()
      if (noteShowRoute && typeof noteShowRoute.props === "function") {
        expect(noteShowRoute.props(route)).toEqual({ slug: "123" })
      }
    })

    it("should not match legacy /n:id URLs", () => {
      const resolved = router.resolve("/n123")
      expect(resolved.matched.some((r) => r.name === "noteShow")).toBe(false)
    })
  })

  describe("noteShowByNotebookSlug route", () => {
    it("should match path with multi-segment note slug and pass props", async () => {
      await router.push("/d/notebooks/42/notes/journal/2025/daily")

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShowByNotebookSlug")
      expect(route.params.notebookId).toBe("42")
      expect(route.params.noteSlugPath).toBe("journal/2025/daily")

      const meta = routes.find((r) => r.name === "noteShowByNotebookSlug")
      expect(meta).toBeDefined()
      if (meta && typeof meta.props === "function") {
        expect(meta.props(route)).toEqual({
          notebookId: 42,
          noteSlugPath: "journal/2025/daily",
        })
      }
    })
    it("should navigate by name with multi-segment noteSlugPath params", async () => {
      await router.push({
        name: "noteShowByNotebookSlug",
        params: {
          notebookId: "42",
          noteSlugPath: "journal/2025/daily",
        },
      })

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShowByNotebookSlug")
      expect(route.params.notebookId).toBe("42")
      expect(route.params.noteSlugPath).toBe("journal/2025/daily")
      const meta = routes.find((r) => r.name === "noteShowByNotebookSlug")
      expect(meta).toBeDefined()
      if (meta && typeof meta.props === "function") {
        expect(meta.props(route)).toEqual({
          notebookId: 42,
          noteSlugPath: "journal/2025/daily",
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
