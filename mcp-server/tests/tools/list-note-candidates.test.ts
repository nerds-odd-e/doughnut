import { describe, test, expect, vi, beforeEach } from 'vitest'
import makeMe from 'donut-test-fixtures/makeMe'
import type { RelationshipLiteralSearchHit } from '@generated/donut-backend-api'
import { createMockContext, findTool } from '../helpers/index.js'
import { SearchController } from '@generated/donut-backend-api/sdk.gen'

vi.mock('@generated/donut-backend-api/sdk.gen', () => ({
  SearchController: {
    searchForRelationshipTarget: vi.fn(),
  },
}))

const searchReturns = (hits: RelationshipLiteralSearchHit[]) =>
  vi.mocked(SearchController.searchForRelationshipTarget).mockResolvedValue({
    data: hits,
    error: undefined,
  } as Awaited<ReturnType<typeof SearchController.searchForRelationshipTarget>>)

const noteHit = (
  noteSearchResult: RelationshipLiteralSearchHit['noteSearchResult']
): RelationshipLiteralSearchHit => ({ hitKind: 'NOTE', noteSearchResult })

const listCandidates = (query: string) =>
  findTool('list_note_candidates').handle(createMockContext(), { query })

describe('list_note_candidates tool', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('searches all notebooks and subscriptions for the query', async () => {
    searchReturns([])

    await listCandidates('Functional')

    expect(SearchController.searchForRelationshipTarget).toHaveBeenCalledWith({
      body: { searchKey: 'Functional', allMyNotebooksAndSubscriptions: true },
    })
  })

  test('lists every note hit in order, telling equal titles apart', async () => {
    searchReturns([
      noteHit(
        makeMe.aNoteSearchResult
          .id(1)
          .title('Functional')
          .notebookName('CS concepts')
          .folderName('Programming Concepts')
          .distance(0)
          .please()
      ),
      noteHit(
        makeMe.aNoteSearchResult
          .id(2)
          .title('Functional')
          .notebookName('Maths')
          .distance(0.1)
          .please()
      ),
      noteHit(
        makeMe.aNoteSearchResult
          .id(3)
          .title('Functional analysis')
          .notebookName('Maths')
          .folderName('Analysis')
          .distance(0.4)
          .please()
      ),
    ])

    const result = await listCandidates('Functional')

    expect(JSON.parse(result.content[0].text)).toEqual([
      {
        noteId: 1,
        title: 'Functional',
        notebookName: 'CS concepts',
        folderName: 'Programming Concepts',
        distance: 0,
      },
      { noteId: 2, title: 'Functional', notebookName: 'Maths', distance: 0.1 },
      {
        noteId: 3,
        title: 'Functional analysis',
        notebookName: 'Maths',
        folderName: 'Analysis',
        distance: 0.4,
      },
    ])
  })

  test('leaves folder and notebook hits out of the candidates', async () => {
    searchReturns([
      { hitKind: 'FOLDER', folderId: 7, folderName: 'Functional' },
      { hitKind: 'NOTEBOOK', notebookId: 8, notebookName: 'Functional' },
      noteHit(makeMe.aNoteSearchResult.id(5).title('Functional').please()),
    ])

    const result = await listCandidates('Functional')

    expect(
      JSON.parse(result.content[0].text).map(
        (c: { noteId: number }) => c.noteId
      )
    ).toEqual([5])
  })

  test.each([
    ['no hits', []],
    [
      'only container hits',
      [{ hitKind: 'FOLDER', folderId: 7, folderName: 'Fiona' }],
    ],
  ] as [string, RelationshipLiteralSearchHit[]][])(
    'reports no matching notes for %s',
    async (_case, hits) => {
      searchReturns(hits)

      const result = await listCandidates('Fiona')

      expect(result.content[0].text).toBe('No matching notes.')
    }
  )
})
