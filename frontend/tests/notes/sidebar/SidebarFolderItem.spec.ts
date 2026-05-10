import SidebarFolderItem from "@/components/notes/SidebarFolderItem.vue"
import { sidebarTreeKey } from "@/components/notes/useNoteSidebarTree"
import { flushPromises, mount } from "@vue/test-utils"
import { computed, ref } from "vue"
import { createRouter, createWebHistory } from "vue-router"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

function mountFolderItem(
  router: ReturnType<typeof createRouter>,
  options: {
    folderId: number
    notebookId: number
    activeFolder?: ReturnType<typeof ref<{ id: number; name: string } | null>>
  }
) {
  const folder = {
    id: options.folderId,
    name: "Alpha",
    createdAt: "2020-01-01T00:00:00Z",
    updatedAt: "2020-01-01T00:00:00Z",
  }
  const expandedFolderIds = ref(new Set<number>())
  const activeFolder = options.activeFolder ?? ref(null)
  return mount(SidebarFolderItem, {
    props: {
      folder,
      notebookId: options.notebookId,
    },
    global: {
      plugins: [router],
      provide: {
        [sidebarTreeKey]: {
          expandedFolderIds,
          activePathFolderIds: computed(() => new Set()),
          activeFolder,
        },
      },
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
          path: "/d/notebooks/:notebookId(\\d+)/folders/:folderId(\\d+)",
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

    const activeFolder = ref<{ id: number; name: string } | null>(null)
    mountFolderItem(router, { folderId: 42, notebookId: 7, activeFolder })
    activeFolder.value = { id: 42, name: "Alpha" }
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

    const activeFolder = ref<{ id: number; name: string } | null>(null)
    mountFolderItem(router, { folderId: 42, notebookId: 7, activeFolder })
    activeFolder.value = { id: 42, name: "Alpha" }
    await flushPromises()
    await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
    await flushPromises()
    await new Promise<void>((resolve) => setTimeout(resolve, 0))
    expect(scrollSpy).not.toHaveBeenCalled()
  })
})
