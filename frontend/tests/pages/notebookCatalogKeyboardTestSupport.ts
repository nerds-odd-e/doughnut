import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"
import NotebooksPageView from "@/pages/NotebooksPageView.vue"
import helper from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, beforeEach, expect } from "vitest"
import type { RouterLink } from "vue-router"
import { clearNotebooksPageStorage } from "./notebooksPageTestSupport"

export function setupCatalogKeyboardTest(layout: string) {
  let wrapper: VueWrapper

  beforeEach(() => {
    clearNotebooksPageStorage()
    localStorage.setItem("donut.notebooksPage.layout", layout)
  })
  afterEach(() => wrapper?.unmount())

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

  return {
    mountCatalog,
    expectFocusedEntry,
    get wrapper() {
      return wrapper
    },
  }
}
