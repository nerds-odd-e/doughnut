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
    it("should match path /n:noteId format (e.g., /n123)", async () => {
      const noteId = 123
      await router.push(`/n${noteId}`)

      expect(router.currentRoute.value.name).toBe("noteShow")
      expect(router.currentRoute.value.params.noteId).toBe(String(noteId))
      expect(router.currentRoute.value.path).toBe(`/n${noteId}`)
    })

    it("should extract noteId from path", async () => {
      const noteId = 456
      await router.push(`/n${noteId}`)

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShow")
      expect(route.params.noteId).toBe(String(noteId))
    })

    it("should pass noteId as number prop to component", async () => {
      const noteId = 789
      await router.push(`/n${noteId}`)

      const route = router.currentRoute.value
      const noteShowRoute = routes.find((r) => r.name === "noteShow")
      expect(noteShowRoute).toBeDefined()

      if (noteShowRoute && typeof noteShowRoute.props === "function") {
        const props = noteShowRoute.props(route)
        expect(props.noteId).toBe(noteId)
      }
    })

    it("should match basename alias /d/notes/:noteId and pass basename prop", async () => {
      await router.push("/d/notes/my-note-slug")

      const route = router.currentRoute.value
      expect(route.name).toBe("noteShow")
      expect(route.params.noteId).toBe("my-note-slug")

      const noteShowRoute = routes.find((r) => r.name === "noteShow")
      expect(noteShowRoute).toBeDefined()
      if (noteShowRoute && typeof noteShowRoute.props === "function") {
        expect(noteShowRoute.props(route)).toEqual({
          basename: "my-note-slug",
        })
      }
    })

    it("should not match noteShow when /n is not followed by digits only", () => {
      const resolved = router.resolve("/nabc")
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
  })
})
