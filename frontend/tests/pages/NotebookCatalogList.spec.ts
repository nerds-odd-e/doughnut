import NotebooksPageView from "@/pages/NotebooksPageView.vue"
import { beforeEach, describe, expect, it } from "vitest"
import { RouterLink } from "vue-router"
import makeMe from "doughnut-test-fixtures/makeMe"
import { NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY } from "@/composables/useNoteSidebarPeerSort"
import helper from "@tests/helpers"
import { fireEvent } from "@testing-library/vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"

async function pickNotebookCatalogPeerSort(
  wrapper: VueWrapper,
  field: "title" | "created" | "updated",
  direction: "asc" | "desc"
) {
  await fireEvent.click(
    wrapper.get('[data-testid="notebook-catalog-sort"] summary').element
  )
  await flushPromises()
  await fireEvent.click(
    document.querySelector(`[data-catalog-sort="${field}-${direction}"]`)!
  )
  await flushPromises()
}

describe("catalog list", () => {
  beforeEach(() => {
    localStorage.removeItem("doughnut.notebooksPage.sortOrder")
    localStorage.removeItem("doughnut.notebooksPage.layout")
    sessionStorage.removeItem(NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY)
  })

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

  it("collapses the sort dropdown when clicking outside", async () => {
    const catalogItems = makeMe.notebookCatalog.notebook("Alpha").please()

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

    const dropdown = wrapper.get('[data-testid="notebook-catalog-sort"]')
      .element as HTMLDetailsElement
    dropdown.open = true
    document.body.dispatchEvent(new MouseEvent("click", { bubbles: true }))
    await flushPromises()

    expect(dropdown.open).toBe(false)
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
