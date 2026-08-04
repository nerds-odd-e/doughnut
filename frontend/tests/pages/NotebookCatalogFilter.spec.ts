import NotebooksPageView from "@/pages/NotebooksPageView.vue"
import { beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import { NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY } from "@/composables/useNoteSidebarPeerSort"
import helper from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"

describe("filter", () => {
  beforeEach(() => {
    localStorage.removeItem("doughnut.notebooksPage.sortOrder")
    localStorage.removeItem("doughnut.notebooksPage.layout")
    sessionStorage.removeItem(NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY)
  })

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
