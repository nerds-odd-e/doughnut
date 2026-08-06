import { beforeEach, describe, expect, it, vi } from "vitest"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import makeMe from "doughnut-test-fixtures/makeMe"
import {
  clearNotebooksPageStorage,
  mockMyNotebooks,
  mountNotebooksPage,
} from "./notebooksPageTestSupport"

describe("read book catalog button", () => {
  beforeEach(() => {
    clearNotebooksPageStorage()
  })

  it("shows read book control when hasAttachedBook is true", async () => {
    const nb = makeMe.aNotebook.please()
    mockMyNotebooks({ notebooks: [{ notebook: nb, hasAttachedBook: true }] })
    const wrapper = await mountNotebooksPage()
    expect(
      wrapper.find('[data-testid="notebook-catalog-read-book"]').exists()
    ).toBe(true)
  })

  it("hides read book control when hasAttachedBook is false", async () => {
    const nb = makeMe.aNotebook.please()
    mockMyNotebooks({ notebooks: [{ notebook: nb, hasAttachedBook: false }] })
    const wrapper = await mountNotebooksPage()
    expect(
      wrapper.find('[data-testid="notebook-catalog-read-book"]').exists()
    ).toBe(false)
  })

  it("navigates to book reading when read book is clicked", async () => {
    const nb = makeMe.aNotebook.please()
    mockMyNotebooks({ notebooks: [{ notebook: nb, hasAttachedBook: true }] })
    const router = createRouter({ history: createWebHistory(), routes })
    const pushSpy = vi.spyOn(router, "push")
    const wrapper = await mountNotebooksPage(router)
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
})
