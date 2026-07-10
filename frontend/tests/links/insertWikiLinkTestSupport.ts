import { SearchController } from "@generated/doughnut-backend-api/sdk.gen"
import type { Note, NoteSearchResult } from "@generated/doughnut-backend-api"
import { useContentCursorInserter } from "@/composables/useContentCursorInserter"
import { fireEvent, screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { mockSdkService } from "@tests/helpers"
import {
  renderSearchForm,
  setupSearchFormSdkMocks,
  typeInSearch,
} from "@tests/links/searchDialogTestSupport"
import { afterEach, beforeEach, vi } from "vitest"

export const insertedTexts: string[] = []
export const wikiPropertyInserted: string[] = []

export function setupInserters(wikiPropertyCanInsert = false) {
  const { registerInserter, registerWikiPropertyInserter, unregisterInserter } =
    useContentCursorInserter()
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

export async function openLinkInsertionChoice(
  note: Note,
  options: {
    searchKey: string
    targetResult: NoteSearchResult
    withRouter?: boolean
    wikiPropertyCanInsert?: boolean
  }
) {
  if (options.wikiPropertyCanInsert) {
    setupInserters(true)
  }
  mockRelationshipSearch(options.targetResult)
  const searchInput = await renderSearchForm(
    { note },
    { router: options.withRouter }
  )
  await typeInSearch(searchInput, options.searchKey)
  fireEvent.click(screen.getByText("Add link"))
  await flushPromises()
}

export function setupInsertWikiLinkTests() {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    insertedTexts.length = 0
    wikiPropertyInserted.length = 0
    setupSearchFormSdkMocks()
    setupInserters()
  })

  afterEach(() => {
    vi.useRealTimers()
  })
}
