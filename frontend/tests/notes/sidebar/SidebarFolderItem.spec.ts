import SidebarFolderItem from "@/components/notes/SidebarFolderItem.vue"
import type { Folder } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import { flushPromises, mount } from "@vue/test-utils"
import { createRouter, createWebHistory } from "vue-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

function mountFolderItem(
  router: ReturnType<typeof createRouter>,
  options: {
    folderId: number
    notebookId: number
    activeFolder?: Folder | null
    activePathFolderIds?: Set<number>
  }
) {
  const folder = {
    id: options.folderId,
    name: "Alpha",
    createdAt: "2020-01-01T00:00:00Z",
    updatedAt: "2020-01-01T00:00:00Z",
  }
  return mount(SidebarFolderItem, {
    props: {
      folder,
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
  const originalIntersectionObserver = globalThis.IntersectionObserver

  beforeEach(async () => {
    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: "/", component: { template: "<div/>" } },
        {
          path: "/notebooks/:notebookId(\\d+)/folders/:folderId(\\d+)",
          name: "folderPage",
          component: { template: "<div/>" },
        },
      ],
    })
    await router.push("/")
  })

  afterEach(() => {
    globalThis.IntersectionObserver = originalIntersectionObserver
    vi.restoreAllMocks()
  })

  it("requests expansion when it is the active folder on folder page", async () => {
    const activeFolder = makeMe.aFolder.folder(42, "Alpha").please()
    const wrapper = mountFolderItem(router, {
      folderId: 42,
      notebookId: 7,
      activeFolder,
    })
    await flushPromises()
    const updates = wrapper.emitted("update:expandedFolderIds") as
      | [Set<number>][]
      | undefined
    expect(updates?.some(([ids]) => ids.has(42))).toBe(true)
    await wrapper.setProps({ expandedFolderIds: new Set([42]) })
    expect(wrapper.attributes("aria-expanded")).toBe("true")
  })

  it("renders a link to folderPage with encoded ids", async () => {
    const wrapper = mountFolderItem(router, { folderId: 42, notebookId: 7 })
    const link = wrapper.get('[data-testid="sidebar-folder-open-page-link"]')
    expect(link.attributes("href")).toContain("notebooks/7")
    expect(link.attributes("href")).toContain("folders/42")
    expect(link.text()).toContain("Alpha")
  })

  it("scrolls folder row into view when active folder row is not intersecting", async () => {
    const scrollSpy = vi.spyOn(HTMLElement.prototype, "scrollIntoView")
    globalThis.IntersectionObserver = class {
      constructor(private readonly cb: IntersectionObserverCallback) {}
      observe() {
        setTimeout(() => {
          this.cb(
            [{ isIntersecting: false }] as IntersectionObserverEntry[],
            this as unknown as IntersectionObserver
          )
        }, 0)
      }
      disconnect() {
        /* no-op mock */
      }
      unobserve() {
        /* no-op mock */
      }
    } as unknown as typeof IntersectionObserver

    const activeFolder = makeMe.aFolder.folder(42, "Alpha").please()
    mountFolderItem(router, { folderId: 42, notebookId: 7, activeFolder })
    await flushPromises()
    await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
    await flushPromises()
    await vi.waitUntil(() => scrollSpy.mock.calls.length > 0)
    expect(scrollSpy).toHaveBeenCalled()
  })

  it("does not scroll folder row when active folder row is already intersecting", async () => {
    const scrollSpy = vi.spyOn(HTMLElement.prototype, "scrollIntoView")
    globalThis.IntersectionObserver = class {
      constructor(private readonly cb: IntersectionObserverCallback) {}
      observe() {
        setTimeout(() => {
          this.cb(
            [{ isIntersecting: true }] as IntersectionObserverEntry[],
            this as unknown as IntersectionObserver
          )
        }, 0)
      }
      disconnect() {
        /* no-op mock */
      }
      unobserve() {
        /* no-op mock */
      }
    } as unknown as typeof IntersectionObserver

    const activeFolder = makeMe.aFolder.folder(42, "Alpha").please()
    mountFolderItem(router, { folderId: 42, notebookId: 7, activeFolder })
    await flushPromises()
    await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
    await flushPromises()
    await new Promise<void>((resolve) => setTimeout(resolve, 0))
    expect(scrollSpy).not.toHaveBeenCalled()
  })
})
