import SearchResultListItem from "@/components/search/SearchResultListItem.vue"
import { screen } from "@testing-library/vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import type {
  NoteSearchResult,
  RelationshipLiteralSearchHit,
} from "@generated/doughnut-backend-api"
import { describe, it, expect } from "vitest"

function noteHit(r: NoteSearchResult): RelationshipLiteralSearchHit {
  return { hitKind: "NOTE", noteSearchResult: r }
}

describe("SearchResultListItem", () => {
  it("renders the row with title", async () => {
    const searchResult: NoteSearchResult = makeMe.aNoteSearchResult
      .id(1)
      .title("Test Note")
      .notebookId(10)
      .please()

    helper
      .component(SearchResultListItem)
      .withProps({ searchHit: noteHit(searchResult) })
      .render()

    await screen.findByText("Test Note")
  })

  it("does not add border when notebookId is not provided", async () => {
    const searchResult: NoteSearchResult = makeMe.aNoteSearchResult
      .id(1)
      .title("Test Note")
      .notebookId(10)
      .please()

    const wrapper = helper
      .component(SearchResultListItem)
      .withProps({ searchHit: noteHit(searchResult) })
      .mount()

    const row = wrapper.find('[role="listitem"]')
    expect(row.classes()).not.toContain("different-notebook-border")
  })

  it("does not add border when notebookId matches", async () => {
    const searchResult: NoteSearchResult = makeMe.aNoteSearchResult
      .id(1)
      .title("Test Note")
      .notebookId(10)
      .please()

    const wrapper = helper
      .component(SearchResultListItem)
      .withProps({ searchHit: noteHit(searchResult), notebookId: 10 })
      .mount()

    const row = wrapper.find('[role="listitem"]')
    expect(row.classes()).not.toContain("different-notebook-border")
  })

  it("adds border when notebookId does not match", async () => {
    const searchResult: NoteSearchResult = makeMe.aNoteSearchResult
      .id(1)
      .title("Test Note")
      .notebookId(10)
      .please()

    const wrapper = helper
      .component(SearchResultListItem)
      .withProps({ searchHit: noteHit(searchResult), notebookId: 20 })
      .mount()

    const row = wrapper.find('[role="listitem"]')
    expect(row.classes()).toContain("different-notebook-border")
  })

  it("displays notebook name when provided", async () => {
    const searchResult = makeMe.aNoteSearchResult
      .id(1)
      .title("Test Note")
      .notebookId(10)
      .notebookName("My Notebook")
      .please()

    helper
      .component(SearchResultListItem)
      .withProps({ searchHit: noteHit(searchResult) })
      .render()

    await screen.findByText("My Notebook", {
      selector: ".notebook-name-label",
    })
  })

  it("does not display notebook name element when notebookName is not provided", async () => {
    const searchResult = makeMe.aNoteSearchResult
      .id(1)
      .title("Test Note")
      .notebookId(10)
      .please()

    const wrapper = helper
      .component(SearchResultListItem)
      .withProps({ searchHit: noteHit(searchResult) })
      .mount()

    expect(wrapper.find(".notebook-name-label").exists()).toBe(false)
  })

  it("renders folder title without note link", async () => {
    const hit: RelationshipLiteralSearchHit = {
      hitKind: "FOLDER",
      folderId: 9,
      folderName: "Archive",
      notebookId: 1,
      notebookName: "NB",
      distance: 0.9,
    }
    const wrapper = helper
      .component(SearchResultListItem)
      .withProps({ searchHit: hit })
      .mount()

    expect(wrapper.text()).toContain("Archive")
    expect(wrapper.find(".router-link").exists()).toBe(false)
  })

  it("renders folderButton slot for folder hit", () => {
    const hit: RelationshipLiteralSearchHit = {
      hitKind: "FOLDER",
      folderId: 9,
      folderName: "Archive",
      notebookId: 1,
      notebookName: "NB",
      distance: 0.9,
    }
    const wrapper = helper
      .component(SearchResultListItem)
      .withProps({ searchHit: hit })
      .mount({
        slots: {
          folderButton: '<button type="button">Move Under</button>',
        },
      })

    expect(wrapper.text()).toContain("Move Under")
  })

  it("note hit button slot can show Add link without Move Under", () => {
    const searchResult: NoteSearchResult = makeMe.aNoteSearchResult
      .id(1)
      .title("Test Note")
      .notebookId(10)
      .please()

    const wrapper = helper
      .component(SearchResultListItem)
      .withProps({ searchHit: noteHit(searchResult) })
      .mount({
        slots: {
          button: '<button type="button">Add link</button>',
        },
      })

    expect(wrapper.text()).toContain("Add link")
    expect(wrapper.text()).not.toContain("Move Under")
  })
})
