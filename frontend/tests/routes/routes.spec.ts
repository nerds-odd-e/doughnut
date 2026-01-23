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
  })
})
