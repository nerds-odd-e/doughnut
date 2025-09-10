import { describe, test, expect, vi } from 'vitest'
import { createMockApi, createMockContext, findTool } from '../helpers/index.js'

describe('get_relevant_note tool', () => {
  // Helper function to create mock API for get_relevant_note tests
  const createRelevantNoteMockApi = (searchResult: unknown[]) =>
    createMockApi({
      restSearchController: {
        searchForLinkTarget: vi.fn().mockResolvedValue(searchResult),
      },
    })

  // Helper function to run the test
  const runQueryExtractionTest = async (
    args: unknown,
    expectedSearchKey: string,
    shouldFindNote = true
  ) => {
    const getRelevantNoteTool = findTool('get_relevant_note')

    const searchResult = shouldFindNote
      ? [{ noteTopology: { id: 123, titleOrPredicate: 'Test Note' } }]
      : []
    const mockApi = createRelevantNoteMockApi(searchResult)
    const ctx = createMockContext(mockApi)

    // Call the tool's handle function
    const result = await getRelevantNoteTool.handle(
      ctx,
      args as unknown as Record<string, unknown>
    )

    // Assert search was called with correct arguments
    expect(
      mockApi.restSearchController.searchForLinkTarget
    ).toHaveBeenCalledWith({
      searchKey: expectedSearchKey,
      allMyNotebooksAndSubscriptions: true,
    })

    // Assert the response
    if (shouldFindNote) {
      // Now we expect the NoteSearchResult JSON structure
      const responseText = result.content[0].text
      expect(responseText).toContain('"noteTopology"')
      expect(responseText).toContain('"id":123')
    } else {
      expect(result.content[0].text).toBe('No relevant note found.')
    }
  }

  test('should extract query when args is an object with query property', async () => {
    const args = { query: 'query in query' }
    const expectedSearchKey = 'query in query'
    const shouldFindNote = true

    await runQueryExtractionTest(args, expectedSearchKey, shouldFindNote)
  })

  test('should be defined and have correct name', () => {
    const getRelevantNoteTool = findTool('get_relevant_note')
    expect(getRelevantNoteTool.name).toBe('get_relevant_note')
  })
})
