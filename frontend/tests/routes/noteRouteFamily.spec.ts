import { describe, it, expect, beforeEach } from "vitest"
import { createMemoryHistory, createRouter, createWebHistory } from "vue-router"
import type { RouteComponent } from "vue-router"
import NotebookSidebarLayout from "@/layouts/NotebookSidebarLayout.vue"
import {
  isNoteRouteFamily,
  noteRouteFamilyNoteId,
} from "@/routes/noteRouteFamily"
import {
  pathnameLooksLikeInternalNoteFamily,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import { routeMetadata } from "@/routes/routeMetadata"
import { routeRecordsFromMetadata } from "@/routes/routeRecordsFromMetadata"
import routes from "@/routes/routes"

const noteFamilyParentPath = routeMetadata.find(
  (metadata) => metadata.name === "noteShow"
)!.path
const dummyPage: RouteComponent = { template: "<div />" }
const siblingMetadata = {
  path: `${noteFamilyParentPath}/p/:propertyKey`,
  name: "noteFamilySibling",
}

function recordsWithSibling() {
  return routeRecordsFromMetadata(
    [...routeMetadata, siblingMetadata],
    () => dummyPage,
    dummyPage
  )
}

describe("note route family", () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    router = createRouter({
      history: createWebHistory(),
      routes,
    })
  })

  it("nests noteShow as the index child of one shared /n:noteId sidebar parent", () => {
    const parents = routes.filter((r) => r.path === noteFamilyParentPath)
    expect(parents).toHaveLength(1)
    const parent = parents[0]!
    expect(parent.name).toBeUndefined()
    expect(parent.component).toBe(NotebookSidebarLayout)
    expect(parent.meta?.noteRouteFamily).toBe(true)
    expect(parent.children?.map((child) => child.name)).toEqual(["noteShow"])
    expect(parent.children?.map((child) => child.path)).toEqual([""])
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

  it("groups a sibling child under the same shared family parent", async () => {
    const records = recordsWithSibling()
    const parents = records.filter((r) => r.path === noteFamilyParentPath)
    expect(parents).toHaveLength(1)
    expect(parents[0]!.children?.map((child) => child.name)).toEqual([
      "noteShow",
      "noteFamilySibling",
    ])
    expect(parents[0]!.children?.map((child) => child.path)).toEqual([
      "",
      "p/:propertyKey",
    ])

    const siblingRouter = createRouter({
      history: createMemoryHistory(),
      routes: records,
    })
    await siblingRouter.push("/n123/p/Due")
    expect(siblingRouter.currentRoute.value.name).toBe("noteFamilySibling")
    expect(isNoteRouteFamily(siblingRouter.currentRoute.value)).toBe(true)
    expect(noteRouteFamilyNoteId(siblingRouter.currentRoute.value)).toBe("123")
    expect(
      pathnameLooksLikeInternalNoteFamily("/n123/p/Due", siblingRouter)
    ).toBe(true)
  })
})

describe("internal note-route classifier", () => {
  it.each(["/n123", "/n/123", "/d/n/123"])(
    "treats %s as an internal note-family URL",
    (pathname) => {
      expect(pathnameLooksLikeInternalNoteFamily(pathname)).toBe(true)
    }
  )

  it.each([
    "/n123/p/Due",
    "/Folder/Title.md",
    "/d/notebooks/42/notes/a/b",
    "/notebooks",
  ])("does not treat %s as an internal note-family URL", (pathname) => {
    expect(pathnameLooksLikeInternalNoteFamily(pathname)).toBe(false)
  })
})
