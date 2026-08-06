import { describe, test, expect, vi, beforeEach } from 'vitest'
import makeMe from 'doughnut-test-fixtures/makeMe'
import { createMockContext, findTool } from '../helpers/index.js'
import { SearchController } from '@generated/doughnut-backend-api/sdk.gen'

vi.mock('@generated/doughnut-backend-api/sdk.gen', () => ({
  SearchController: {
    searchForRelationshipTarget: vi.fn(),
  },
}))

describe('find_most_relevant_note tool', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('searches all notebooks and returns the top note as JSON', async () => {
    const note = makeMe.aNoteSearchResult.id(123).title('Test Note').please()
    const mockSearch = vi.mocked(SearchController.searchForRelationshipTarget)
    mockSearch.mockResolvedValue({
      data: [
        {
          hitKind: 'NOTE',
          noteSearchResult: note,
        },
      ],
      error: undefined,
    } as Awaited<
      ReturnType<typeof SearchController.searchForRelationshipTarget>
    >)

    const result = await findTool('find_most_relevant_note').handle(
      createMockContext(),
      { query: 'query in query' }
    )

    expect(mockSearch).toHaveBeenCalledWith({
      body: {
        searchKey: 'query in query',
        allMyNotebooksAndSubscriptions: true,
      },
    })
    expect(result.content[0].text).toBe(JSON.stringify(note))
  })
})
