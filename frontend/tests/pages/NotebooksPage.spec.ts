import NotebooksPage from "@/pages/NotebooksPage.vue"
import NotebooksPageView from "@/pages/NotebooksPageView.vue"
import { describe, it, expect } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import type { Notebook } from "@generated/doughnut-backend-api"
import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"

function catalogFromNotebooks(notebooks: Notebook[]): NotebookCatalogEntry[] {
  return notebooks.map((notebook) => ({ type: "notebook", notebook }))
}

describe("Notebooks Page", () => {
  it("fetch API to be called ONCE", async () => {
    const notebook = makeMe.aNotebook.please()

    const myNotebooksSpy = mockSdkService("myNotebooks", {
      notebooks: [notebook],
      catalogItems: catalogFromNotebooks([notebook]),
      subscriptions: [],
    })
    helper.component(NotebooksPage).withRouter().render()
    expect(myNotebooksSpy).toBeCalledTimes(1)
  })

  describe("notebook updates", () => {
    it("should update notebook in the list when notebook-updated event is emitted", async () => {
      const originalNotebook = {
        ...makeMe.aNotebook.please(),
        title: "Original Title",
      }
      const updatedNotebook = {
        ...originalNotebook,
        title: "Updated Title",
        notebookSettings: {
          ...originalNotebook.notebookSettings,
          numberOfQuestionsInAssessment: 10,
        },
      }

      mockSdkService("myNotebooks", {
        notebooks: [originalNotebook],
        catalogItems: catalogFromNotebooks([originalNotebook]),
        subscriptions: [],
      })
      mockSdkService("updateNotebook", updatedNotebook)

      const wrapper = helper
        .component(NotebooksPage)
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()

      await flushPromises()

      const vm = wrapper.vm as unknown as {
        notebooks: (typeof originalNotebook)[] | undefined
        catalogItems: NotebookCatalogEntry[] | undefined
      }

      // Verify initial state
      expect(vm.notebooks).toHaveLength(1)
      expect(vm.notebooks?.[0]?.title).toBe("Original Title")
      expect(vm.catalogItems?.[0]?.type).toBe("notebook")
      if (vm.catalogItems?.[0]?.type === "notebook") {
        expect(vm.catalogItems[0].notebook.title).toBe("Original Title")
      }

      // Find and trigger the notebook-updated event
      const notebookButtons = wrapper.findComponent({ name: "NotebookButtons" })
      notebookButtons.vm.$emit("notebook-updated", updatedNotebook)
      await flushPromises()

      // Verify the notebook was updated
      expect(vm.notebooks?.[0]?.title).toBe("Updated Title")
      if (vm.catalogItems?.[0]?.type === "notebook") {
        expect(vm.catalogItems[0].notebook.title).toBe("Updated Title")
      }
    })

    it("should handle notebook-updated event when notebooks array is populated", async () => {
      const notebook1 = {
        ...makeMe.aNotebook.please(),
        title: "Notebook 1",
      }
      const notebook2 = {
        ...makeMe.aNotebook.please(),
        title: "Notebook 2",
      }

      const updatedNotebook1 = {
        ...notebook1,
        title: "Updated Notebook 1",
      }

      mockSdkService("myNotebooks", {
        notebooks: [notebook1, notebook2],
        catalogItems: catalogFromNotebooks([notebook1, notebook2]),
        subscriptions: [],
      })

      const wrapper = helper
        .component(NotebooksPage)
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()

      await flushPromises()

      // Get component instance to check internal state
      const vm = wrapper.vm as unknown as {
        notebooks: (typeof notebook1)[] | undefined
      }

      // Verify initial state
      expect(vm.notebooks).toHaveLength(2)
      expect(vm.notebooks?.[0]?.title).toBe("Notebook 1")

      // Emit notebook-updated event
      const notebookButtons = wrapper.findAllComponents({
        name: "NotebookButtons",
      })[0]
      if (notebookButtons) {
        notebookButtons.vm.$emit("notebook-updated", updatedNotebook1)
      }
      await flushPromises()

      // Verify the notebook was updated
      expect(vm.notebooks).toHaveLength(2)
      expect(vm.notebooks?.[0]?.title).toBe("Updated Notebook 1")
      expect(vm.notebooks?.[1]?.title).toBe("Notebook 2")
    })

    it("should handle empty notebooks array gracefully", async () => {
      // Start with empty notebooks array
      mockSdkService("myNotebooks", {
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
        notebooks: Notebook[] | undefined
      }

      // Verify empty state
      expect(vm.notebooks).toHaveLength(0)

      // NotebookButtons shouldn't exist when there are no notebooks
      const notebookButtons = wrapper.findComponent({ name: "NotebookButtons" })
      expect(notebookButtons.exists()).toBe(false)
    })

    it("should handle event from NotebookButtons", async () => {
      const originalNotebook = {
        ...makeMe.aNotebook.please(),
        title: "Before Update",
      }
      const updatedNotebook = {
        ...originalNotebook,
        title: "After Update",
      }

      mockSdkService("myNotebooks", {
        notebooks: [originalNotebook],
        catalogItems: catalogFromNotebooks([originalNotebook]),
        subscriptions: [],
      })

      const wrapper = helper
        .component(NotebooksPage)
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()

      await flushPromises()

      const vm = wrapper.vm as unknown as {
        notebooks: (typeof originalNotebook)[] | undefined
      }

      // Verify initial state
      expect(vm.notebooks?.[0]?.title).toBe("Before Update")

      // Simulate event from NotebookButtons
      const notebookButtons = wrapper.findComponent({ name: "NotebookButtons" })
      notebookButtons.vm.$emit("notebook-updated", updatedNotebook)
      await flushPromises()

      // Verify the notebook was updated
      expect(vm.notebooks?.[0]?.title).toBe("After Update")
    })

    it("patches grouped notebook in catalogItems when notebook-updated fires", async () => {
      const member = {
        ...makeMe.aNotebook.please(),
        title: "Member Title",
      }
      const catalogItems: NotebookCatalogEntry[] = [
        {
          type: "notebookGroup",
          id: 1,
          name: "G",
          createdAt: "2020-01-01T00:00:00.000Z",
          notebooks: [member],
        },
      ]

      mockSdkService("myNotebooks", {
        notebooks: [member],
        catalogItems,
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

      const updated = { ...member, title: "Renamed Member" }
      const buttons = wrapper.findComponent({ name: "NotebookButtons" })
      buttons.vm.$emit("notebook-updated", updated)
      await flushPromises()

      const grp = vm.catalogItems?.[0]
      expect(grp?.type).toBe("notebookGroup")
      if (grp?.type === "notebookGroup") {
        expect(grp.notebooks[0]?.title).toBe("Renamed Member")
      }
    })
  })

  describe("catalog list", () => {
    it("renders catalog items in document order (list layout)", async () => {
      const nTop = { ...makeMe.aNotebook.please(), title: "Top Loose" }
      const m1 = { ...makeMe.aNotebook.please(), title: "Inside One" }
      const catalogItems: NotebookCatalogEntry[] = [
        { type: "notebook", notebook: nTop },
        {
          type: "notebookGroup",
          id: 42,
          name: "Middle Group",
          createdAt: "2020-01-01T00:00:00.000Z",
          notebooks: [m1],
        },
        {
          type: "notebook",
          notebook: { ...makeMe.aNotebook.please(), title: "Bottom Loose" },
        },
      ]

      const wrapper = helper
        .component(NotebooksPageView)
        .withProps({
          catalogItems,
          subscriptions: [],
          user: makeMe.aUser.please(),
        })
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()

      await flushPromises()

      const headingTexts = wrapper.findAll("h3, h5").map((w) => w.text())
      expect(headingTexts).toEqual([
        "Top Loose",
        "Middle Group",
        "Inside One",
        "Bottom Loose",
      ])
    })

    it("shows member hint for groups with many notebooks", async () => {
      const m1 = { ...makeMe.aNotebook.please(), title: "Member Alpha" }
      const m2 = { ...makeMe.aNotebook.please(), title: "Member Beta" }
      const m3 = { ...makeMe.aNotebook.please(), title: "Member Gamma" }
      const m4 = { ...makeMe.aNotebook.please(), title: "Member Delta" }
      const catalogItems: NotebookCatalogEntry[] = [
        {
          type: "notebookGroup",
          id: 1,
          name: "Big Group",
          createdAt: "2020-01-01T00:00:00.000Z",
          notebooks: [m1, m2, m3, m4],
        },
      ]

      const wrapper = helper
        .component(NotebooksPageView)
        .withProps({
          catalogItems,
          subscriptions: [],
          user: makeMe.aUser.please(),
        })
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()

      await flushPromises()

      expect(wrapper.text()).toContain("Member Alpha, Member Beta, and 2 more")

      const groupCard = wrapper.find('[data-cy="notebook-group-card"]')
      expect(groupCard.exists()).toBe(true)
      expect(groupCard.classes()).toContain("notebook-catalog-group")
      expect(groupCard.attributes("aria-label")).toContain("Member Alpha")
      expect(groupCard.attributes("aria-label")).toContain("Member Delta")
    })
  })
})
