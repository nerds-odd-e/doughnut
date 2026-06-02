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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { nextTick } from "vue"

const SEARCH_DEBOUNCE_MS = 1000

describe("InsertWikiLink", () => {
  const insertedTexts: string[] = []
  const wikiPropertyInserted: string[] = []

  async function waitForSearchDebounce() {
    await nextTick()
    vi.advanceTimersByTime(SEARCH_DEBOUNCE_MS + 100)
    await flushPromises()
  }

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

  function mountSearchForm(note: Note, options?: { withRouter?: boolean }) {
    let builder = helper
      .component(SearchForm)
      .withCleanStorage()
      .withProps({ note })
    if (options?.withRouter) {
      builder = builder.withRouter()
    }
    return builder.render()
  }

  async function searchAndOpenLinkChoice(searchKey: string) {
    const searchInput = await screen.findByPlaceholderText("Search")
    fireEvent.update(searchInput, searchKey)
    await waitForSearchDebounce()
    fireEvent.click(screen.getByText("Add link"))
    await flushPromises()
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
    mountSearchForm(note)

    await searchAndOpenLinkChoice("CI")

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
    mountSearchForm(note, { withRouter: true })

    await searchAndOpenLinkChoice("Sed")

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
    mountSearchForm(note)

    await searchAndOpenLinkChoice("Prop")

    fireEvent.click(screen.getByText("Add wiki link as a new property"))
    await flushPromises()

    expect(wikiPropertyInserted).toContain("[[PropTarget]]")
    expect(insertedTexts).toHaveLength(0)
  })
})
