import NotebooksPage from "@/pages/NotebooksPage.vue"
import NotebooksPageView from "@/pages/NotebooksPageView.vue"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { createRouter, createWebHistory, RouterLink } from "vue-router"
import routes from "@/routes/routes"
import makeMe, {
  type NotebookCatalogEntry,
} from "doughnut-test-fixtures/makeMe"
import { NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY } from "@/composables/useNoteSidebarPeerSort"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"

async function pickNotebookCatalogPeerSort(
  wrapper: VueWrapper,
  field: "title" | "created" | "updated",
  direction: "asc" | "desc"
) {
  ;(
    wrapper.get('[data-testid="notebook-catalog-sort"]')
      .element as HTMLDetailsElement
  ).open = true
  await flushPromises()
  await wrapper
    .get(`[data-catalog-sort="${field}-${direction}"]`)
    .trigger("click")
  await flushPromises()
}

describe("Notebooks Page", () => {
  beforeEach(() => {
    localStorage.removeItem("doughnut.notebooksPage.sortOrder")
    localStorage.removeItem("doughnut.notebooksPage.layout")
    sessionStorage.removeItem(NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY)
  })

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
        name: "Original Title",
      }
      const updatedNotebook = {
        ...originalNotebook,
        name: "Updated Title",
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
        name: "Before Update",
      }
      const updatedNotebook = {
        ...originalNotebook,
        name: "After Update",
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

    it("patches grouped notebook in catalogItems when notebook-updated fires", async () => {
      const member = {
        ...makeMe.aNotebook.please(),
        name: "Member Title",
      }
      const catalogItems = [
        makeMe.notebookCatalogGroup
          .id(1)
          .name("G")
          .createdAt("2020-01-01T00:00:00.000Z")
          .membersFromNotebooks([member])
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

      const updated = { ...member, name: "Renamed Member" }
      const buttons = wrapper.findComponent({ name: "NotebookButtons" })
      buttons.vm.$emit("notebook-updated", updated)
      await flushPromises()

      const grp = vm.catalogItems?.[0]
      expect(grp?.type).toBe("notebookGroup")
      if (grp?.type === "notebookGroup") {
        expect(grp.notebooks[0]?.notebook.name).toBe("Renamed Member")
      }
    })

    it("preserves hasAttachedBook when notebook-updated payload omits it", async () => {
      const notebookEntity = { ...makeMe.aNotebook.please(), name: "T" }
      const updatedNotebook = { ...notebookEntity, name: "Updated" }

      mockSdkService("myNotebooks", {
        notebooks: [{ notebook: notebookEntity, hasAttachedBook: true }],
        catalogItems: makeMe.notebookCatalog
          .notebooks({ ...notebookEntity, hasAttachedBook: true })
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

      const notebookButtons = wrapper.findComponent({ name: "NotebookButtons" })
      notebookButtons.vm.$emit("notebook-updated", updatedNotebook)
      await flushPromises()

      if (vm.catalogItems?.[0]?.type === "notebook") {
        expect(vm.catalogItems[0].hasAttachedBook).toBe(true)
        expect(vm.catalogItems[0].notebook.name).toBe("Updated")
      }
    })
  })

  describe("read book catalog button", () => {
    it("shows read book control when hasAttachedBook is true", async () => {
      const nb = makeMe.aNotebook.please()
      mockSdkService("myNotebooks", {
        notebooks: [{ notebook: nb, hasAttachedBook: true }],
        catalogItems: makeMe.notebookCatalog
          .notebooks({ ...nb, hasAttachedBook: true })
          .please(),
        subscriptions: [],
      })
      const wrapper = helper
        .component(NotebooksPage)
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()
      await flushPromises()
      expect(
        wrapper.find('[data-testid="notebook-catalog-read-book"]').exists()
      ).toBe(true)
    })

    it("navigates to book reading when read book is clicked", async () => {
      const nb = makeMe.aNotebook.please()
      mockSdkService("myNotebooks", {
        notebooks: [{ notebook: nb, hasAttachedBook: true }],
        catalogItems: makeMe.notebookCatalog
          .notebooks({ ...nb, hasAttachedBook: true })
          .please(),
        subscriptions: [],
      })
      const router = createRouter({ history: createWebHistory(), routes })
      const pushSpy = vi.spyOn(router, "push")
      const wrapper = helper
        .component(NotebooksPage)
        .withCurrentUser(makeMe.aUser.please())
        .withRouter(router)
        .mount()
      await flushPromises()
      await wrapper
        .find('[data-testid="notebook-catalog-read-book"]')
        .trigger("click")
      expect(pushSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          name: "bookReading",
          params: expect.objectContaining({
            notebookId: nb.id,
          }),
        })
      )
    })

    it("hides read book control when hasAttachedBook is false", async () => {
      const nb = makeMe.aNotebook.please()
      mockSdkService("myNotebooks", {
        notebooks: [{ notebook: nb, hasAttachedBook: false }],
        catalogItems: makeMe.notebookCatalog
          .notebooks({ ...nb, hasAttachedBook: false })
          .please(),
        subscriptions: [],
      })
      const wrapper = helper
        .component(NotebooksPage)
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()
      await flushPromises()
      expect(
        wrapper.find('[data-testid="notebook-catalog-read-book"]').exists()
      ).toBe(false)
    })
  })

  describe("catalog overflow menu", () => {
    it("offers move to group without edit notebook settings", async () => {
      const nb = { ...makeMe.aNotebook.please(), name: "Owned Catalog" }
      mockSdkService("myNotebooks", {
        notebooks: [nb],
        catalogItems: makeMe.notebookCatalog.notebooks(nb).please(),
        subscriptions: [],
      })
      const wrapper = helper
        .component(NotebooksPage)
        .withCurrentUser(makeMe.aUser.please())
        .withRouter()
        .mount()
      await flushPromises()
      await wrapper
        .find('[data-cy="notebook-catalog-overflow"]')
        .trigger("click")
      await flushPromises()
      expect(
        wrapper.find('button[title="Edit notebook settings"]').exists()
      ).toBe(false)
      expect(wrapper.find('button[title="Move to group"]').exists()).toBe(true)
    })
  })

  describe("catalog list", () => {
    it("sorts catalog by title A–Z by default (list layout)", async () => {
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
        "Bottom Loose",
        "Middle Group",
        "Inside One",
        "Top Loose",
      ])
    })

    it("sorts catalog by title Z–A when selected", async () => {
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
      await pickNotebookCatalogPeerSort(wrapper, "title", "desc")

      const headingTexts = wrapper.findAll("h3, h5").map((w) => w.text())
      expect(headingTexts).toEqual([
        "Top Loose",
        "Middle Group",
        "Inside One",
        "Bottom Loose",
      ])
    })

    it("returns to title A–Z after title Z–A", async () => {
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
      await pickNotebookCatalogPeerSort(wrapper, "title", "desc")
      await pickNotebookCatalogPeerSort(wrapper, "title", "asc")

      const headingTexts = wrapper.findAll("h3, h5").map((w) => w.text())
      expect(headingTexts).toEqual([
        "Bottom Loose",
        "Middle Group",
        "Inside One",
        "Top Loose",
      ])
    })

    it("sorts group members by title A–Z by default", async () => {
      const catalogItems = makeMe.notebookCatalog
        .group("My Group", "Zebra", "Alpha")
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
      expect(headingTexts).toEqual(["My Group", "Alpha", "Zebra"])
    })

    it("shows member hint for groups with many notebooks", async () => {
      const catalogItems = [
        makeMe.notebookCatalogGroup
          .name("Big Group")
          .id(1)
          .createdAt("2020-01-01T00:00:00.000Z")
          .names("Member Alpha", "Member Beta", "Member Gamma", "Member Delta")
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

      expect(wrapper.text()).toContain("Showing 3 of 4 notebooks")

      const groupCard = wrapper.find('[data-cy="notebook-group-card"]')
      expect(groupCard.exists()).toBe(true)
      expect(groupCard.classes()).toContain("notebook-catalog-group")
      expect(groupCard.findAll(".notebook-list-row").length).toBe(3)
      expect(groupCard.text()).toContain("Member Alpha")
      expect(groupCard.text()).toContain("Member Beta")
      expect(groupCard.text()).toContain("Member Delta")
      expect(groupCard.text()).not.toContain("Member Gamma")
      expect(groupCard.attributes("aria-label")).toContain("Member Alpha")
      expect(groupCard.attributes("aria-label")).toContain("Member Delta")
    })

    it("group header links to notebook group route", async () => {
      const catalogItems = [
        makeMe.notebookCatalogGroup
          .id(42)
          .name("Nav Group")
          .createdAt("2020-01-01T00:00:00.000Z")
          .names("Member One")
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

      const groupCard = wrapper.get('[data-cy="notebook-group-card"]')
      const headerRouterLink = groupCard.findAllComponents(RouterLink)[0]
      expect(headerRouterLink.props("to")).toEqual({
        name: "notebookGroup",
        params: { groupId: 42 },
      })
    })
  })

  describe("filter", () => {
    it("focuses the filter input when the catalog has notebooks", async () => {
      const focusedIds: string[] = []
      const originalFocus = HTMLInputElement.prototype.focus
      const focusSpy = vi
        .spyOn(HTMLInputElement.prototype, "focus")
        .mockImplementation(function (
          this: HTMLInputElement,
          ...args: Parameters<HTMLInputElement["focus"]>
        ) {
          focusedIds.push(this.id)
          return originalFocus.apply(this, args)
        })

      try {
        const catalogItems = makeMe.notebookCatalog
          .notebook("Alpha notebook")
          .please()

        helper
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

        expect(focusedIds).toContain("notebook-filter-input")
      } finally {
        focusSpy.mockRestore()
      }
    })

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
      expect(wrapper.text()).not.toContain("UI Patterns")
      expect(wrapper.text()).toContain("API Notes")
      expect(wrapper.text()).not.toContain("Other notebook")
    })

    it("caps filtered matching members at three with matching count subtitle", async () => {
      const catalogItems = makeMe.notebookCatalog
        .group("Batch", "X One", "X Two", "X Three", "X Four")
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
      await wrapper.find("#notebook-filter-input").setValue("x ")

      const groupCard = wrapper.get('[data-cy="notebook-group-card"]')
      expect(groupCard.findAll(".notebook-list-row").length).toBe(3)
      expect(groupCard.text()).toContain("Showing 3 of 4 matching notebooks")
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
        name: "Bazaar Shared",
      }
      const catalogItems = [
        makeMe.notebookCatalogNotebook.name("Owned").please(),
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
      const overflowTriggers = wrapper.findAll(
        '[data-cy="notebook-catalog-overflow"]'
      )
      expect(overflowTriggers.length).toBeGreaterThanOrEqual(2)
      await overflowTriggers[1]!.trigger("click")
      await flushPromises()
      expect(wrapper.find('button[title="Edit subscription"]').exists()).toBe(
        true
      )
    })

    it("shows subscription actions for a subscribed member inside a group", async () => {
      const ownedMember = {
        ...makeMe.aNotebook.please(),
        name: "Owned In Group",
      }
      const subMember = {
        ...makeMe.aNotebook.please(),
        name: "Subscribed In Group",
      }
      const catalogItems = [
        makeMe.notebookCatalogGroup
          .id(1)
          .name("Mixed Group")
          .createdAt("2020-01-01T00:00:00.000Z")
          .membersFromNotebooks([ownedMember, subMember])
          .please(),
      ]
      const subscriptions = [
        {
          id: 99,
          dailyTargetOfNewNotes: 5,
          notebook: subMember,
          user: makeMe.aUser.please(),
        },
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
      const subMemberCard = wrapper
        .findAll('[data-cy="notebook-card"]')
        .find((c) => c.text().includes("Subscribed In Group"))
      expect(subMemberCard?.exists()).toBe(true)
      await subMemberCard!
        .find('[data-cy="notebook-catalog-overflow"]')
        .trigger("click")
      await flushPromises()
      expect(
        subMemberCard!.find('button[title="Edit subscription"]').exists()
      ).toBe(true)
    })
  })
})
