import SidebarFolderItem from "@/components/notes/SidebarFolderItem.vue"
import { sidebarTreeKey } from "@/components/notes/useNoteSidebarTree"
import { mount } from "@vue/test-utils"
import { computed, ref } from "vue"
import { createRouter, createWebHistory } from "vue-router"
import { beforeEach, describe, expect, it, vi } from "vitest"

describe("SidebarFolderItem", () => {
  let router: ReturnType<typeof createRouter>

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

  it("renders a link to folderPage with encoded ids", async () => {
    const folder = {
      id: 42,
      name: "Alpha",
      createdAt: "2020-01-01T00:00:00Z",
      updatedAt: "2020-01-01T00:00:00Z",
    }
    const expandedFolderIds = ref(new Set<number>())
    const userActiveFolder = ref(null)
    const wrapper = mount(SidebarFolderItem, {
      props: {
        folder,
        notebookId: 7,
      },
      global: {
        plugins: [router],
        provide: {
          [sidebarTreeKey]: {
            expandedFolderIds,
            toggleFolder: vi.fn(),
            ancestorFolderIds: computed(() => new Set()),
            activeNoteFolderIds: computed(() => new Set()),
            userActiveFolder,
          },
        },
      },
    })
    const link = wrapper.get('[data-testid="sidebar-folder-open-page-link"]')
    expect(link.attributes("href")).toContain("notebooks/7")
    expect(link.attributes("href")).toContain("folders/42")
    expect(link.text()).toContain("Alpha")
  })
})
