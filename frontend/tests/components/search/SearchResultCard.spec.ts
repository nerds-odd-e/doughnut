import SearchResultCard from "@/components/search/SearchResultCard.vue"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import type { NoteSearchResult } from "@generated/doughnut-backend-api"
import { describe, it, expect } from "vitest"

describe("SearchResultCard", () => {
  it("renders the card with title", async () => {
    const searchResult: NoteSearchResult = makeMe.aNoteSearchResult
      .id(1)
      .title("Test Note")
      .notebookId(10)
      .please()

    helper.component(SearchResultCard).withProps({ searchResult }).render()

    await screen.findByText("Test Note")
  })

  it("does not add border when notebookId is not provided", async () => {
    const searchResult: NoteSearchResult = makeMe.aNoteSearchResult
      .id(1)
      .title("Test Note")
      .notebookId(10)
      .please()

    const wrapper = helper
      .component(SearchResultCard)
      .withProps({ searchResult })
      .mount()

    const card = wrapper.find('[role="card"]')
    expect(card.classes()).not.toContain("different-notebook-border")
  })

  it("does not add border when notebookId matches", async () => {
    const searchResult: NoteSearchResult = makeMe.aNoteSearchResult
      .id(1)
      .title("Test Note")
      .notebookId(10)
      .please()

    const wrapper = helper
      .component(SearchResultCard)
      .withProps({ searchResult, notebookId: 10 })
      .mount()

    const card = wrapper.find('[role="card"]')
    expect(card.classes()).not.toContain("different-notebook-border")
  })

  it("adds border when notebookId does not match", async () => {
    const searchResult: NoteSearchResult = makeMe.aNoteSearchResult
      .id(1)
      .title("Test Note")
      .notebookId(10)
      .please()

    const wrapper = helper
      .component(SearchResultCard)
      .withProps({ searchResult, notebookId: 20 })
      .mount()

    const card = wrapper.find('[role="card"]')
    expect(card.classes()).toContain("different-notebook-border")
  })

  it("displays notebook title when provided", async () => {
    const searchResult = makeMe.aNoteSearchResult
      .id(1)
      .title("Test Note")
      .notebookId(10)
      .notebookTitle("My Notebook")
      .please()

    helper.component(SearchResultCard).withProps({ searchResult }).render()

    await screen.findByText("My Notebook", {
      selector: ".notebook-title-label",
    })
  })

  it("does not display notebook title element when notebookTitle is not provided", async () => {
    const searchResult = makeMe.aNoteSearchResult
      .id(1)
      .title("Test Note")
      .notebookId(10)
      .please()

    const wrapper = helper
      .component(SearchResultCard)
      .withProps({ searchResult })
      .mount()

    expect(wrapper.find(".notebook-title-label").exists()).toBe(false)
  })
})
