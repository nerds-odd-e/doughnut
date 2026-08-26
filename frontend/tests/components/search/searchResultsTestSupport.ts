import type {
  NoteSearchResult,
  RelationshipLiteralSearchHit,
} from "@generated/doughnut-backend-api"
import { SearchController } from "@generated/doughnut-backend-api/sdk.gen"
import SearchResults from "@/components/search/SearchResults.vue"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"

export { advanceSearchDebounce as waitForDebounce } from "@tests/helpers/searchDebounceTestSupport"

export const recentNotes: NoteSearchResult[] = [
  makeMe.aNoteSearchResult.id(1).title("Recent Note 1").distance(null).please(),
  makeMe.aNoteSearchResult.id(2).title("Recent Note 2").distance(null).please(),
]

export const searchResult = (
  id: number,
  title: string,
  distance?: number
): NoteSearchResult =>
  makeMe.aNoteSearchResult.id(id).title(title).distance(distance).please()

export function asLiteralHits(
  notes: NoteSearchResult[]
): RelationshipLiteralSearchHit[] {
  return notes.map((r) => ({ hitKind: "NOTE", noteSearchResult: r }))
}

export function setupSearchMocks(
  literalResults: NoteSearchResult[] = [],
  semanticResults: NoteSearchResult[] = []
) {
  const literal = asLiteralHits(literalResults)
  mockSdkService(SearchController, "searchForRelationshipTarget", literal)
  mockSdkService(SearchController, "semanticSearch", semanticResults)
  mockSdkService(SearchController, "searchForRelationshipTargetWithin", literal)
  mockSdkService(SearchController, "semanticSearchWithin", semanticResults)
}

export function setupDelayedSearchMocks() {
  const delayed = new Promise<Array<unknown>>((resolve) =>
    setTimeout(() => resolve([]), 1)
  )

  const searchSpy = mockSdkService(
    SearchController,
    "searchForRelationshipTarget",
    []
  )
  const semanticSpy = mockSdkService(SearchController, "semanticSearch", [])
  searchSpy.mockReturnValue(
    delayed.then((data) => wrapSdkResponse(data)) as never
  )
  semanticSpy.mockReturnValue(
    delayed.then((data) => wrapSdkResponse(data)) as never
  )
  return { searchSpy, semanticSpy }
}

/** Note id from stubbed router-link `to` (see RenderingHelper). */
export function noteIdFromStubbedLinkTo(toAttr: string): number | undefined {
  try {
    const to = JSON.parse(toAttr) as {
      params?: { noteId?: string | number }
    }
    const nid = to.params?.noteId
    if (nid != null) return Number(nid)
    return
  } catch {
    return
  }
}

export function notebookIdFromStubbedLinkTo(
  toAttr: string
): number | undefined {
  try {
    const to = JSON.parse(toAttr) as {
      name?: string
      params?: { notebookId?: string | number }
    }
    if (to.name === "notebookPage" && to.params?.notebookId != null) {
      return Number(to.params.notebookId)
    }
    return
  } catch {
    return
  }
}

export function mountSearchResults(props: {
  inputSearchKey: string
  isDropdown?: boolean
  noteId?: number
  notebookId?: number
  semanticSearchEnabled?: boolean
}) {
  return helper.component(SearchResults).withProps(props).mount()
}
