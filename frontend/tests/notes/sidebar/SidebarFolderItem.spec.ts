import SidebarFolderItem from "@/components/notes/SidebarFolderItem.vue"
import type { Folder } from "@generated/donut-backend-api"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises, mount, type VueWrapper } from "@vue/test-utils"
import { dummyRouteRecordsFromMetadata } from "@/routes/dummyRouteRecords"
import { createRouter, createWebHistory } from "vue-router"
import { afterEach, beforeEach, describe, expect, it } from "vitest"

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
  })

  it("requests expansion for the active folder", async () => {
    const activeFolder = makeMe.aFolder.folder(42, "Alpha").please()
    wrapper = mountFolderItem(router, {
      folderId: 42,
      notebookId: 7,
      activeFolder,
    })
    await flushPromises()
    const updates = wrapper.emitted("update:expandedFolderIds") as
      | [Set<number>][]
      | undefined
    expect(updates?.some(([ids]) => ids.has(activeFolder.id))).toBe(true)
    await wrapper.setProps({ expandedFolderIds: new Set([activeFolder.id]) })
    expect(wrapper.attributes("aria-expanded")).toBe("true")
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
})
