import SidebarFolderItem from "@/components/notes/SidebarFolderItem.vue"
import type { Folder } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises, mount, type VueWrapper } from "@vue/test-utils"
import { dummyRouteRecordsFromMetadata } from "@/routes/dummyRouteRecords"
import { createRouter, createWebHistory } from "vue-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { stubIntersectionObserver } from "./sidebarTestSupport"

function mountFolderItem(
  router: ReturnType<typeof createRouter>,
  options: {
    folderId: number
    notebookId: number
    activeFolder?: Folder | null
    activePathFolderIds?: Set<number>
  }
) {
  return mount(SidebarFolderItem, {
    props: {
      folder: makeMe.aFolder.folder(options.folderId, "Alpha").please(),
      notebookId: options.notebookId,
      expandedFolderIds: new Set<number>(),
      activePathFolderIds: options.activePathFolderIds ?? new Set<number>(),
      activeFolder: options.activeFolder ?? undefined,
    },
    global: {
      plugins: [router],
    },
  })
}

describe("SidebarFolderItem", () => {
  let router: ReturnType<typeof createRouter>
  let wrapper: VueWrapper | undefined
  let restoreIntersectionObserver: (() => void) | undefined

  beforeEach(async () => {
    router = createRouter({
      history: createWebHistory(),
      routes: dummyRouteRecordsFromMetadata,
    })
    await router.push("/")
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = undefined
    restoreIntersectionObserver?.()
    restoreIntersectionObserver = undefined
    vi.restoreAllMocks()
  })

  async function mountActiveFolderItem(isIntersecting: boolean) {
    const scrollSpy = vi.spyOn(HTMLElement.prototype, "scrollIntoView")
    restoreIntersectionObserver = stubIntersectionObserver(isIntersecting)
    const activeFolder = makeMe.aFolder.folder(42, "Alpha").please()
    wrapper = mountFolderItem(router, {
      folderId: 42,
      notebookId: 7,
      activeFolder,
    })
    await flushPromises()
    return { scrollSpy, activeFolder }
  }

  it("requests expansion for the active folder and does not scroll when intersecting", async () => {
    const { scrollSpy, activeFolder } = await mountActiveFolderItem(true)
    const updates = wrapper!.emitted("update:expandedFolderIds") as
      | [Set<number>][]
      | undefined
    expect(updates?.some(([ids]) => ids.has(activeFolder.id))).toBe(true)
    await wrapper!.setProps({ expandedFolderIds: new Set([activeFolder.id]) })
    expect(wrapper!.attributes("aria-expanded")).toBe("true")
    expect(scrollSpy).not.toHaveBeenCalled()
  })

  it("renders a link to folderPage with encoded ids", async () => {
    wrapper = mountFolderItem(router, { folderId: 42, notebookId: 7 })
    const link = wrapper.get('[data-testid="sidebar-folder-open-page-link"]')
    expect(link.attributes("href")).toBe(
      router.resolve({
        name: "folderPage",
        params: { notebookId: "7", folderId: "42" },
      }).href
    )
    expect(link.text()).toContain("Alpha")
  })

  it("scrolls folder row into view when active folder row is not intersecting", async () => {
    const { scrollSpy } = await mountActiveFolderItem(false)
    expect(scrollSpy).toHaveBeenCalled()
  })
})
