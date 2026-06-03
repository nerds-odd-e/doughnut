import { describe, test, expect, vi, beforeEach } from 'vitest'
import makeMe from 'doughnut-test-fixtures/makeMe'
import {
  createMockContext,
  findTool,
  setupMockApiClient,
} from '../helpers/index.js'
import { SearchController } from '@generated/doughnut-backend-api/sdk.gen'

vi.mock('@generated/doughnut-backend-api/sdk.gen', () => ({
  SearchController: {
    searchForRelationshipTarget: vi.fn(),
  },
}))

describe('find_most_relevant_note tool', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setupMockApiClient()
  })

  test('should extract query when args is an object with query property', async () => {
    const mockSearch = vi.mocked(SearchController.searchForRelationshipTarget)
    mockSearch.mockResolvedValue({
      data: [
        {
          hitKind: 'NOTE',
          noteSearchResult: makeMe.aNoteSearchResult
            .id(123)
            .title('Test Note')
            .please(),
        },
      ],
      error: undefined,
    } as Awaited<
      ReturnType<typeof SearchController.searchForRelationshipTarget>
    >)

    const tool = findTool('find_most_relevant_note')
    await tool.handle(createMockContext(), { query: 'query in query' })

    expect(mockSearch).toHaveBeenCalledWith({
      body: {
        searchKey: 'query in query',
        allMyNotebooksAndSubscriptions: true,
      },
    })
  })
})
