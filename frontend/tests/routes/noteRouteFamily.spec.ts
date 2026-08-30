import { describe, it, expect, beforeEach } from "vitest"
import { createRouter, createWebHistory } from "vue-router"
import NotebookSidebarLayout from "@/layouts/NotebookSidebarLayout.vue"
import NoteShowPage from "@/pages/NoteShowPage.vue"
import {
  isNoteRouteFamily,
  noteRouteFamilyNoteId,
} from "@/routes/noteRouteFamily"
import {
  notePropertyHref,
  notePropertyLocation,
  pathnameLooksLikeInternalNoteFamily,
  resolveInternalNoteFamilyFromHref,
  noteShowHref,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import { routeMetadata } from "@/routes/routeMetadata"
import routes from "@/routes/routes"

const noteFamilyParentPath = routeMetadata.find(
  (metadata) => metadata.name === "noteShow"
)!.path

describe("note route family", () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    router = createRouter({
      history: createWebHistory(),
      routes,
    })
  })

  it("nests noteShow and noteProperty as sibling children of one shared /n:noteId sidebar parent", () => {
    const parents = routes.filter((r) => r.path === noteFamilyParentPath)
    expect(parents).toHaveLength(1)
    const parent = parents[0]!
    expect(parent.name).toBeUndefined()
    expect(parent.component).toBe(NotebookSidebarLayout)
    expect(parent.meta?.noteRouteFamily).toBe(true)
    expect(parent.children?.map((child) => child.name)).toEqual([
      "noteShow",
      "noteProperty",
    ])
    expect(parent.children?.map((child) => child.path)).toEqual([
      "",
      "p/:propertyKey",
    ])
    expect(parent.children?.map((child) => child.component)).toEqual([
      NoteShowPage,
      NoteShowPage,
    ])
  })

  it("treats noteShow as family and notebook pages as not", async () => {
    await router.push(noteShowLocation(123))
    expect(isNoteRouteFamily(router.currentRoute.value)).toBe(true)
    expect(noteRouteFamilyNoteId(router.currentRoute.value)).toBe("123")

    await router.push({
      name: "notebookPage",
      params: { notebookId: "1" },
    })
    expect(isNoteRouteFamily(router.currentRoute.value)).toBe(false)
    expect(noteRouteFamilyNoteId(router.currentRoute.value)).toBeUndefined()
  })

  it("treats noteProperty as the same note-route family", async () => {
    await router.push(notePropertyLocation(123, "Due"))
    expect(router.currentRoute.value.name).toBe("noteProperty")
    expect(isNoteRouteFamily(router.currentRoute.value)).toBe(true)
    expect(noteRouteFamilyNoteId(router.currentRoute.value)).toBe("123")
  })
})

describe("internal note-route classifier", () => {
  it.each([
    "/n123",
    "/n/123",
    "/d/n/123",
    "/n123/p/Due",
    "/n/123/p/Due",
    "/d/n/123/p/Due",
  ])("treats %s as an internal note-family URL", (pathname) => {
    expect(pathnameLooksLikeInternalNoteFamily(pathname)).toBe(true)
  })

  it.each(["/Folder/Title.md", "/d/notebooks/42/notes/a/b", "/notebooks"])(
    "does not treat %s as an internal note-family URL",
    (pathname) => {
      expect(pathnameLooksLikeInternalNoteFamily(pathname)).toBe(false)
    }
  )

  it("parses noteShow hrefs through the route table", () => {
    expect(resolveInternalNoteFamilyFromHref(noteShowHref(123))).toEqual({
      noteId: 123,
      propertyKey: undefined,
    })
    expect(
      resolveInternalNoteFamilyFromHref("https://app.example/d/n/123")
    ).toEqual({
      noteId: 123,
      propertyKey: undefined,
    })
  })

  it("parses noteProperty hrefs through the route table, decoding the key once", () => {
    expect(
      resolveInternalNoteFamilyFromHref(notePropertyHref(99, "Due"))
    ).toEqual({
      noteId: 99,
      propertyKey: "Due",
    })
    expect(
      resolveInternalNoteFamilyFromHref(notePropertyHref(42, "a part of"))
    ).toEqual({
      noteId: 42,
      propertyKey: "a part of",
    })
    expect(
      resolveInternalNoteFamilyFromHref(
        "https://app.example/n/99/p/Due?conversation=true"
      )
    ).toEqual({
      noteId: 99,
      propertyKey: "Due",
    })
  })
})
