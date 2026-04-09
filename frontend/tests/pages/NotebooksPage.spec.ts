import NotebooksPage from "@/pages/NotebooksPage.vue"
import NotebooksPageView from "@/pages/NotebooksPageView.vue"
import { describe, it, expect } from "vitest"
import makeMe, {
  type NotebookCatalogEntry,
} from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"

describe("Notebooks Page", () => {
  it("fetch API to be called ONCE", async () => {
    const notebook = makeMe.aNotebook.please()

    const myNotebooksSpy = mockSdkService("myNotebooks", {
      notebooks: [notebook],
      catalogItems: makeMe.notebookCatalog.notebooks(notebook).please(),
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
        catalogItems: makeMe.notebookCatalog
          .notebooks(originalNotebook)
          .please(),
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
        catalogItems: NotebookCatalogEntry[] | undefined
      }

      // Verify initial state
      expect(vm.catalogItems).toHaveLength(1)
      expect(vm.catalogItems?.[0]?.type).toBe("notebook")
      if (vm.catalogItems?.[0]?.type === "notebook") {
        expect(vm.catalogItems[0].notebook.title).toBe("Original Title")
      }

      // Find and trigger the notebook-updated event
      const notebookButtons = wrapper.findComponent({ name: "NotebookButtons" })
      notebookButtons.vm.$emit("notebook-updated", updatedNotebook)
      await flushPromises()

      // Verify the notebook was updated
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
        expect(vm.catalogItems[0].notebook.title).toBe("Notebook 1")
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
        expect(vm.catalogItems[0].notebook.title).toBe("Updated Notebook 1")
      }
      if (vm.catalogItems?.[1]?.type === "notebook") {
        expect(vm.catalogItems[1].notebook.title).toBe("Notebook 2")
      }
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
        title: "Before Update",
      }
      const updatedNotebook = {
        ...originalNotebook,
        title: "After Update",
      }

      mockSdkService("myNotebooks", {
        notebooks: [originalNotebook],
        catalogItems: makeMe.notebookCatalog
          .notebooks(originalNotebook)
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

      if (vm.catalogItems?.[0]?.type === "notebook") {
        expect(vm.catalogItems[0].notebook.title).toBe("Before Update")
      }

      // Simulate event from NotebookButtons
      const notebookButtons = wrapper.findComponent({ name: "NotebookButtons" })
      notebookButtons.vm.$emit("notebook-updated", updatedNotebook)
      await flushPromises()

      if (vm.catalogItems?.[0]?.type === "notebook") {
        expect(vm.catalogItems[0].notebook.title).toBe("After Update")
      }
    })

    it("patches grouped notebook in catalogItems when notebook-updated fires", async () => {
      const member = {
        ...makeMe.aNotebook.please(),
        title: "Member Title",
      }
      const catalogItems = [
        makeMe.notebookCatalogGroup
          .id(1)
          .name("G")
          .createdAt("2020-01-01T00:00:00.000Z")
          .members([member])
          .please(),
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
      const catalogItems = makeMe.notebookCatalog
        .notebook("Top Loose")
        .group("Middle Group", "Inside One")
        .notebook("Bottom Loose")
        .please()

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
      const catalogItems = [
        makeMe.notebookCatalogGroup
          .name("Big Group")
          .id(1)
          .createdAt("2020-01-01T00:00:00.000Z")
          .titles("Member Alpha", "Member Beta", "Member Gamma", "Member Delta")
          .please(),
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

  describe("filter", () => {
    it("filters top-level notebooks by title", async () => {
      const catalogItems = makeMe.notebookCatalog
        .notebook("Alpha notebook")
        .notebook("Beta notebook")
        .please()

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
      await wrapper.find("#notebook-filter-input").setValue("beta")

      expect(wrapper.text()).not.toContain("Alpha notebook")
      expect(wrapper.text()).toContain("Beta notebook")
    })

    it("shows a full group when filtering by group name", async () => {
      const catalogItems = makeMe.notebookCatalog
        .group("Writers", "Hemingway", "Woolf")
        .notebook("Outside notebook")
        .please()

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
      await wrapper.find("#notebook-filter-input").setValue("writers")

      expect(wrapper.text()).toContain("Writers")
      expect(wrapper.text()).toContain("Hemingway")
      expect(wrapper.text()).toContain("Woolf")
      expect(wrapper.text()).not.toContain("Outside notebook")
    })

    it("shows the group when one member title matches", async () => {
      const catalogItems = makeMe.notebookCatalog
        .group("Design Group", "UI Patterns", "API Notes")
        .notebook("Other notebook")
        .please()

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
      await wrapper.find("#notebook-filter-input").setValue("api")

      expect(wrapper.text()).toContain("Design Group")
      expect(wrapper.text()).toContain("UI Patterns")
      expect(wrapper.text()).toContain("API Notes")
      expect(wrapper.text()).not.toContain("Other notebook")
    })

    it("restores full list after clearing filter", async () => {
      const catalogItems = makeMe.notebookCatalog
        .notebook("Alpha notebook")
        .notebook("Beta notebook")
        .please()

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
      await wrapper.find("#notebook-filter-input").setValue("beta")
      await wrapper.find('button[aria-label="Clear filter"]').trigger("click")

      expect(wrapper.text()).toContain("Alpha notebook")
      expect(wrapper.text()).toContain("Beta notebook")
    })

    it("shows no-match state and supports clear action", async () => {
      const catalogItems = makeMe.notebookCatalog
        .notebook("Alpha notebook")
        .group("Work", "Sprint Notes")
        .please()

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
      await wrapper.find("#notebook-filter-input").setValue("zzz")

      expect(wrapper.text()).toContain("No notebooks match")
      expect(wrapper.find(".notebook-catalog-section").exists()).toBe(false)

      await wrapper.find('button[aria-label="Clear filter"]').trigger("click")

      expect(wrapper.text()).toContain("Alpha notebook")
      expect(wrapper.text()).toContain("Work")
    })
  })

  describe("subscribed notebooks in merged catalog", () => {
    it("shows subscription actions for a top-level subscribedNotebook row", async () => {
      const subNotebook = {
        ...makeMe.aNotebook.please(),
        title: "Bazaar Shared",
      }
      const catalogItems = [
        makeMe.notebookCatalogNotebook.title("Owned").please(),
        makeMe.notebookCatalogSubscribedNotebook
          .forNotebook(subNotebook)
          .subscriptionId(42)
          .please(),
      ]
      const subscriptions = [
        { id: 42, notebook: subNotebook, user: makeMe.aUser.please() },
      ]

      const wrapper = helper
        .component(NotebooksPageView)
        .withProps({
          catalogItems,
          subscriptions,
          user: makeMe.aUser.please(),
        })
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()

      await flushPromises()

      expect(wrapper.find('button[title="Unsubscribe"]').exists()).toBe(true)
      expect(wrapper.find('button[title="Edit subscription"]').exists()).toBe(
        true
      )
    })

    it("shows subscription actions for a subscribed member inside a group", async () => {
      const ownedMember = {
        ...makeMe.aNotebook.please(),
        title: "Owned In Group",
      }
      const subMember = {
        ...makeMe.aNotebook.please(),
        title: "Subscribed In Group",
      }
      const catalogItems = [
        makeMe.notebookCatalogGroup
          .id(1)
          .name("Mixed Group")
          .createdAt("2020-01-01T00:00:00.000Z")
          .members([ownedMember, subMember])
          .please(),
      ]
      const subscriptions = [
        { id: 99, notebook: subMember, user: makeMe.aUser.please() },
      ]

      const wrapper = helper
        .component(NotebooksPageView)
        .withProps({
          catalogItems,
          subscriptions,
          user: makeMe.aUser.please(),
        })
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()

      await flushPromises()

      const unsubButtons = wrapper.findAll('button[title="Unsubscribe"]')
      expect(unsubButtons.length).toBe(1)
    })
  })
})
