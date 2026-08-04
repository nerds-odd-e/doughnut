import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebooksPage from "@/pages/NotebooksPage.vue"
import { beforeEach, describe, expect, it } from "vitest"
import makeMe, {
  type NotebookCatalogEntry,
} from "doughnut-test-fixtures/makeMe"
import { NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY } from "@/composables/useNoteSidebarPeerSort"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"

describe("notebook updates", () => {
  beforeEach(() => {
    localStorage.removeItem("doughnut.notebooksPage.sortOrder")
    localStorage.removeItem("doughnut.notebooksPage.layout")
    sessionStorage.removeItem(NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY)
  })

  it("should update notebook in the list when notebook-updated event is emitted", async () => {
    const originalNotebook = {
      ...makeMe.aNotebook.please(),
      name: "Original Title",
    }
    const updatedNotebook = {
      ...originalNotebook,
      name: "Updated Title",
      notebookSettings: {
        ...originalNotebook.notebookSettings,
        skipMemoryTrackingEntirely: true,
      },
    }

    mockSdkService(NotebookController, "myNotebooks", {
      notebooks: [{ notebook: originalNotebook }],
      catalogItems: makeMe.notebookCatalog.notebooks(originalNotebook).please(),
      subscriptions: [],
    })
    mockSdkService(NotebookController, "updateNotebook", updatedNotebook)

    const wrapper = helper
      .component(NotebooksPage)
      .withCurrentUser(makeMe.aUser.please())
      .withRouter()
      .mount()

    await flushPromises()

    const vm = wrapper.vm as unknown as {
      catalogItems: NotebookCatalogEntry[] | undefined
    }

    // Verify initial state
    expect(vm.catalogItems).toHaveLength(1)
    expect(vm.catalogItems?.[0]?.type).toBe("notebook")
    if (vm.catalogItems?.[0]?.type === "notebook") {
      expect(vm.catalogItems[0].notebook.name).toBe("Original Title")
    }

    // Find and trigger the notebook-updated event
    const notebookButtons = wrapper.findComponent({ name: "NotebookButtons" })
    notebookButtons.vm.$emit("notebook-updated", updatedNotebook)
    await flushPromises()

    // Verify the notebook was updated
    if (vm.catalogItems?.[0]?.type === "notebook") {
      expect(vm.catalogItems[0].notebook.name).toBe("Updated Title")
    }
  })

  it("should handle notebook-updated event when notebooks array is populated", async () => {
    const notebook1 = {
      ...makeMe.aNotebook.please(),
      name: "Notebook 1",
    }
    const notebook2 = {
      ...makeMe.aNotebook.please(),
      name: "Notebook 2",
    }

    const updatedNotebook1 = {
      ...notebook1,
      name: "Updated Notebook 1",
    }

    mockSdkService(NotebookController, "myNotebooks", {
      notebooks: [{ notebook: notebook1 }, { notebook: notebook2 }],
      catalogItems: makeMe.notebookCatalog
        .notebooks(notebook1, notebook2)
        .please(),
      subscriptions: [],
    })

    const wrapper = helper
      .component(NotebooksPage)
      .withCurrentUser(makeMe.aUser.please())
      .withRouter()
      .mount()

    await flushPromises()

    const vm = wrapper.vm as unknown as {
      catalogItems: NotebookCatalogEntry[] | undefined
    }

    expect(vm.catalogItems).toHaveLength(2)
    expect(vm.catalogItems?.[0]?.type).toBe("notebook")
    if (vm.catalogItems?.[0]?.type === "notebook") {
      expect(vm.catalogItems[0].notebook.name).toBe("Notebook 1")
    }

    // Emit notebook-updated event
    const notebookButtons = wrapper.findAllComponents({
      name: "NotebookButtons",
    })[0]
    if (notebookButtons) {
      notebookButtons.vm.$emit("notebook-updated", updatedNotebook1)
    }
    await flushPromises()

    expect(vm.catalogItems).toHaveLength(2)
    if (vm.catalogItems?.[0]?.type === "notebook") {
      expect(vm.catalogItems[0].notebook.name).toBe("Updated Notebook 1")
    }
    if (vm.catalogItems?.[1]?.type === "notebook") {
      expect(vm.catalogItems[1].notebook.name).toBe("Notebook 2")
    }
  })

  it("should handle empty notebooks array gracefully", async () => {
    // Start with empty notebooks array
    mockSdkService(NotebookController, "myNotebooks", {
      notebooks: [],
      catalogItems: [],
      subscriptions: [],
    })

    const wrapper = helper
      .component(NotebooksPage)
      .withCurrentUser(makeMe.aUser.please())
      .withRouter()
      .mount()

    await flushPromises()

    const vm = wrapper.vm as unknown as {
      catalogItems: NotebookCatalogEntry[] | undefined
    }

    expect(vm.catalogItems).toEqual([])

    // NotebookButtons shouldn't exist when there are no notebooks
    const notebookButtons = wrapper.findComponent({ name: "NotebookButtons" })
    expect(notebookButtons.exists()).toBe(false)
  })

  it("should handle event from NotebookButtons", async () => {
    const originalNotebook = {
      ...makeMe.aNotebook.please(),
      name: "Before Update",
    }
    const updatedNotebook = {
      ...originalNotebook,
      name: "After Update",
    }

    mockSdkService(NotebookController, "myNotebooks", {
      notebooks: [{ notebook: originalNotebook }],
      catalogItems: makeMe.notebookCatalog.notebooks(originalNotebook).please(),
      subscriptions: [],
    })

    const wrapper = helper
      .component(NotebooksPage)
      .withCurrentUser(makeMe.aUser.please())
      .withRouter()
      .mount()

    await flushPromises()

    const vm = wrapper.vm as unknown as {
      catalogItems: NotebookCatalogEntry[] | undefined
    }

    if (vm.catalogItems?.[0]?.type === "notebook") {
      expect(vm.catalogItems[0].notebook.name).toBe("Before Update")
    }

    // Simulate event from NotebookButtons
    const notebookButtons = wrapper.findComponent({ name: "NotebookButtons" })
    notebookButtons.vm.$emit("notebook-updated", updatedNotebook)
    await flushPromises()

    if (vm.catalogItems?.[0]?.type === "notebook") {
      expect(vm.catalogItems[0].notebook.name).toBe("After Update")
    }
  })
})
