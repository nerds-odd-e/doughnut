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
} from "@tests/wiki-link-or-relationship/searchDialogTestSupport"
import { afterEach, beforeEach, vi } from "vitest"

export const insertedTexts: string[] = []
export const insertedWikiLinkAsProperty: string[] = []

export function setupInserters(canInsertWikiLinkAsProperty = false) {
  const {
    registerInserter,
    registerInsertWikiLinkAsPropertyInserter,
    unregisterInserter,
  } = useContentCursorInserter()
  unregisterInserter()
  registerInserter((text) => insertedTexts.push(text))
  registerInsertWikiLinkAsPropertyInserter({
    canInsert: () => canInsertWikiLinkAsProperty,
    insert: (text) => insertedWikiLinkAsProperty.push(text),
  })
}

function mockRelationshipSearch(targetResult: NoteSearchResult) {
  mockSdkService(SearchController, "searchForRelationshipTargetWithin", [
    { hitKind: "NOTE" as const, noteSearchResult: targetResult },
  ])
}

export async function openWikiLinkOrRelationshipChoice(
  note: Note,
  options: {
    searchKey: string
    targetResult: NoteSearchResult
    withRouter?: boolean
    canInsertWikiLinkAsProperty?: boolean
  }
) {
  if (options.canInsertWikiLinkAsProperty) {
    setupInserters(true)
  }
  mockRelationshipSearch(options.targetResult)
  const searchInput = await renderSearchForm(
    { note },
    { router: options.withRouter }
  )
  await typeInSearch(searchInput, options.searchKey)
  fireEvent.click(screen.getByText("Use this note"))
  await flushPromises()
}

export function setupInsertWikiLinkTests() {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    insertedTexts.length = 0
    insertedWikiLinkAsProperty.length = 0
    setupSearchFormSdkMocks()
    setupInserters()
  })

  afterEach(() => {
    vi.useRealTimers()
  })
}
