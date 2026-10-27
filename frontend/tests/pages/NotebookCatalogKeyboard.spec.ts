import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"
import NotebooksPageView from "@/pages/NotebooksPageView.vue"
import helper from "@tests/helpers"
import { dispatchArrowKey } from "@tests/helpers/searchDialogKeyboardTestSupport"
import { fireEvent } from "@testing-library/vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, beforeEach, describe, expect, it } from "vitest"
import type { RouterLink } from "vue-router"
import { clearNotebooksPageStorage } from "./notebooksPageTestSupport"

let wrapper: VueWrapper

async function mountCatalog(catalogItems: NotebookCatalogEntry[]) {
  wrapper = helper
    .component(NotebooksPageView)
    .withProps({
      catalogItems,
      subscriptions: [],
      user: makeMe.aUser.please(),
    })
    .withCurrentUser(makeMe.aUser.please())
    .withRouter()
    .mount({ attachTo: document.body })
  await flushPromises()
  return wrapper.get<HTMLInputElement>("#notebook-filter-input")
}

function expectFocusedEntry(title: string) {
  const heading = wrapper
    .findAll("h3, h5")
    .find((heading) => heading.text() === title)!
  const link = heading.element.closest("a")!
  expect(document.activeElement).toBe(link)
  return wrapper.findComponent<typeof RouterLink>(
    `a[to='${link.getAttribute("to")}']`
  )
}

describe.each(["list", "grid"])("catalog keyboard in %s layout", (layout) => {
  beforeEach(() => {
    clearNotebooksPageStorage()
    localStorage.setItem("donut.notebooksPage.layout", layout)
  })
  afterEach(() => wrapper?.unmount())

  it("walks the displayed links, including group previews and subscriptions, without wrapping", async () => {
    const ownedNotebook = makeMe.aNotebook.title("A Owned").please()
    const subscribed = makeMe.aNotebook.title("Z Subscribed").please()
    const group = makeMe.notebookCatalogGroup
      .name("B Group")
      .names("One", "Two", "Three", "Four", "Five")
      .please()
    const input = await mountCatalog(
      makeMe.notebookCatalog
        .notebooks(ownedNotebook)
        .entry(group)
        .subscribedNotebook(subscribed)
        .please()
    )
    dispatchArrowKey("ArrowUp")
    expect(document.activeElement).toBe(input.element)
    dispatchArrowKey("ArrowDown")
    const owned = expectFocusedEntry("A Owned")
    expect(owned.props("to")).toEqual({
      name: "notebookPage",
      params: { notebookId: ownedNotebook.id },
    })
    dispatchArrowKey("ArrowDown")
    expect(expectFocusedEntry("B Group").props("to")).toEqual({
      name: "notebookGroup",
      params: { groupId: group.id },
    })
    dispatchArrowKey("ArrowDown")
    expect(expectFocusedEntry("Five").props("to")).toEqual({
      name: "notebookPage",
      params: {
        notebookId: group.notebooks.find(
          (member) => member.notebook.name === "Five"
        )!.notebook.id,
      },
    })
    dispatchArrowKey("ArrowDown")
    expectFocusedEntry("Four")
    dispatchArrowKey("ArrowDown")
    expectFocusedEntry("One")
    dispatchArrowKey("ArrowDown")
    expect(expectFocusedEntry("Z Subscribed").props("to")).toEqual({
      name: "notebookPage",
      params: { notebookId: subscribed.id },
    })
    dispatchArrowKey("ArrowDown")
    expectFocusedEntry("Z Subscribed")
  })

  it("walks back to the filter with its text intact", async () => {
    const input = await mountCatalog(
      makeMe.notebookCatalog
        .notebook("Match A")
        .notebook("Match B")
        .notebook("Match C")
        .please()
    )
    await input.setValue("match")
    dispatchArrowKey("ArrowDown")
    dispatchArrowKey("ArrowDown")
    dispatchArrowKey("ArrowDown")
    expectFocusedEntry("Match C")
    dispatchArrowKey("ArrowUp")
    expectFocusedEntry("Match B")
    dispatchArrowKey("ArrowUp")
    expectFocusedEntry("Match A")
    dispatchArrowKey("ArrowUp")
    expect(document.activeElement).toBe(input.element)
    expect(input.element.value).toBe("match")
  })

  it("uses the current filter and sort order", async () => {
    const beta = makeMe.aNotebook.title("Beta").please()
    const input = await mountCatalog(
      makeMe.notebookCatalog.notebook("Alpha").notebooks(beta).please()
    )
    await input.setValue("beta")
    dispatchArrowKey("ArrowDown")
    expect(expectFocusedEntry("Beta").props("to")).toEqual({
      name: "notebookPage",
      params: { notebookId: beta.id },
    })
    input.element.focus()
    await input.setValue("")
    await fireEvent.click(
      wrapper.get('[data-testid="notebook-catalog-sort"] summary').element
    )
    await flushPromises()
    await fireEvent.click(
      document.querySelector('[data-catalog-sort="title-desc"]')!
    )
    await flushPromises()
    input.element.focus()
    dispatchArrowKey("ArrowDown")
    expectFocusedEntry("Beta")
  })

  it("keeps focus in the filter when nothing matches", async () => {
    const input = await mountCatalog(
      makeMe.notebookCatalog.notebook("Alpha").please()
    )
    await input.setValue("missing")
    dispatchArrowKey("ArrowDown")
    expect(document.activeElement).toBe(input.element)
  })

  it("leaves the filter cursor and secondary controls to the browser", async () => {
    const input = await mountCatalog(
      makeMe.notebookCatalog.notebook("Alpha").notebook("Beta").please()
    )
    for (const key of ["ArrowLeft", "ArrowRight", "Tab"]) {
      const event = new KeyboardEvent("keydown", {
        key,
        bubbles: true,
        cancelable: true,
      })
      input.element.dispatchEvent(event)
      expect(event.defaultPrevented).toBe(false)
    }
    await input.setValue("a")
    const controls = [
      wrapper.get<HTMLElement>('[aria-label="Clear filter"]').element,
      wrapper.get<HTMLElement>('[aria-label="Notebook actions"]').element,
      wrapper.get<HTMLElement>('[aria-label="Grid view"]').element,
      wrapper.get<HTMLElement>('[aria-label="Sort notebooks"]').element,
    ]
    for (const control of controls) {
      control.focus()
      dispatchArrowKey("ArrowDown")
      expect(document.activeElement).toBe(control)
    }
  })
})
