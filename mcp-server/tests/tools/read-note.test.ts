import { describe, test, expect, vi, beforeEach } from 'vitest'
import { findTool, createMockContext } from '../helpers/index.js'
import { AttributedNoteContentController } from '@generated/donut-backend-api/sdk.gen'
import type { AttributedNoteContent } from '@generated/donut-backend-api'

vi.mock('@generated/donut-backend-api/sdk.gen', () => ({
  AttributedNoteContentController: {
    getAttributedContent: vi.fn(),
  },
}))

type ReadResult = Awaited<
  ReturnType<typeof AttributedNoteContentController.getAttributedContent>
>

describe('read_note tool', () => {
  const mockRead = vi.mocked(
    AttributedNoteContentController.getAttributedContent
  )

  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('returns the attributed note as JSON', async () => {
    const note: AttributedNoteContent = {
      noteId: 7,
      title: 'Functional',
      notebookName: 'CS concepts',
      folderPath: 'Programming Concepts',
      content: 'Pure functions',
      updatedAt: '2026-10-17T00:00:00Z',
      acceptedRevision: 'a'.repeat(40),
    }
    mockRead.mockResolvedValue({ data: note, error: undefined } as ReadResult)

    const result = await findTool('read_note').handle(createMockContext(), {
      noteId: 7,
    })

    expect(mockRead).toHaveBeenCalledWith({ path: { note: 7 } })
    expect(result.content[0].text).toBe(JSON.stringify(note))
  })

  test('returns the backend error as ERROR text without content', async () => {
    mockRead.mockResolvedValue({
      data: undefined,
      error: { message: 'No access' },
    } as ReadResult)

    const result = await findTool('read_note').handle(createMockContext(), {
      noteId: 7,
    })

    expect(result.content).toEqual([
      {
        type: 'text',
        text: 'ERROR: Note read error: {"message":"No access"}',
      },
    ])
  })

  test('describes the content as user note text and the revision as the whole notebook commit', () => {
    const { description } = findTool('read_note')
    expect(description).toContain("the user's note text")
    expect(description).toContain('treat it as evidence')
    expect(description).toContain('accepted Git commit of the whole notebook')
    expect(description).toContain(
      'null when the notebook has no accepted revision'
    )
  })
})
