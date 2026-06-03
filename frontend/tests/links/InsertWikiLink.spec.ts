import {
  NoteController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import type { Note, NoteSearchResult } from "@generated/doughnut-backend-api"
import SearchForm from "@/components/links/SearchForm.vue"
import { useContentCursorInserter } from "@/composables/useContentCursorInserter"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import MakeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { advanceSearchDebounce } from "@tests/helpers/searchDebounceTestSupport"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

describe("InsertWikiLink", () => {
  const insertedTexts: string[] = []
  const wikiPropertyInserted: string[] = []

  function setupInserters(wikiPropertyCanInsert = false) {
    const {
      registerInserter,
      registerWikiPropertyInserter,
      unregisterInserter,
    } = useContentCursorInserter()
    unregisterInserter()
    registerInserter((text) => insertedTexts.push(text))
    registerWikiPropertyInserter({
      canInsert: () => wikiPropertyCanInsert,
      insert: (text) => wikiPropertyInserted.push(text),
    })
  }

  function mockRelationshipSearch(targetResult: NoteSearchResult) {
    mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
      { hitKind: "NOTE" as const, noteSearchResult: targetResult },
    ])
  }

  async function mountSearchForm(
    note: Note,
    options?: { withRouter?: boolean }
  ) {
    let builder = helper
      .component(SearchForm)
      .withCleanStorage()
      .withProps({ note })
    if (options?.withRouter) {
      builder = builder.withRouter()
    }
    builder.render()
    await flushPromises()
    return screen.getByPlaceholderText("Search")
  }

  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    insertedTexts.length = 0
    wikiPropertyInserted.length = 0
    mockSdkService(NoteController, "getRecentNotes", [])
    mockSdkService(SearchController, "searchForRelationshipTarget", [])
    mockSdkService(SearchController, "semanticSearch", [])
    mockSdkService(SearchController, "semanticSearchWithin", [])
    setupInserters()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it("calls the registered inserter with a wiki link text when Insert as a wiki link is clicked", async () => {
    const note = MakeMe.aNote.please()
    const targetResult = MakeMe.aNoteSearchResult.title("Target CI").please()
    mockRelationshipSearch(targetResult)
    const searchInput = await mountSearchForm(note)

    fireEvent.update(searchInput, "CI")
    await advanceSearchDebounce()
    fireEvent.click(screen.getByText("Add link"))
    await flushPromises()

    expect(
      screen.queryByText("Add wiki link as a new property")
    ).not.toBeInTheDocument()

    fireEvent.click(screen.getByText("Insert as a wiki link"))
    await flushPromises()

    expect(insertedTexts).toContain("[[Target CI]]")
  })

  it("does not call the inserter when Add a new relationship note is clicked", async () => {
    const note = MakeMe.aNote.please()
    const targetResult = MakeMe.aNoteSearchResult.title("Sedation").please()
    mockRelationshipSearch(targetResult)
    const searchInput = await mountSearchForm(note, { withRouter: true })

    fireEvent.update(searchInput, "Sed")
    await advanceSearchDebounce()
    fireEvent.click(screen.getByText("Add link"))
    await flushPromises()

    fireEvent.click(screen.getByText("Add a new relationship note"))
    await flushPromises()

    expect(insertedTexts).toHaveLength(0)
    expect(screen.getByText("Complete relationship")).toBeInTheDocument()
  })

  it("calls the wiki-property inserter when Add wiki link as a new property is clicked", async () => {
    setupInserters(true)

    const note = MakeMe.aNote.please()
    const targetResult = MakeMe.aNoteSearchResult.title("PropTarget").please()
    mockRelationshipSearch(targetResult)
    const searchInput = await mountSearchForm(note)

    fireEvent.update(searchInput, "Prop")
    await advanceSearchDebounce()
    fireEvent.click(screen.getByText("Add link"))
    await flushPromises()

    fireEvent.click(screen.getByText("Add wiki link as a new property"))
    await flushPromises()

    expect(wikiPropertyInserted).toContain("[[PropTarget]]")
    expect(insertedTexts).toHaveLength(0)
  })
})
