import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebooksPage from "@/pages/NotebooksPage.vue"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import makeMe from "doughnut-test-fixtures/makeMe"
import { NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY } from "@/composables/useNoteSidebarPeerSort"
import helper, { mockSdkService } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"

describe("read book catalog button", () => {
  beforeEach(() => {
    localStorage.removeItem("doughnut.notebooksPage.sortOrder")
    localStorage.removeItem("doughnut.notebooksPage.layout")
    sessionStorage.removeItem(NOTE_SIDEBAR_PEER_SORT_STORAGE_KEY)
  })

  it("shows read book control when hasAttachedBook is true", async () => {
    const nb = makeMe.aNotebook.please()
    mockSdkService(NotebookController, "myNotebooks", {
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
    mockSdkService(NotebookController, "myNotebooks", {
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
    mockSdkService(NotebookController, "myNotebooks", {
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
