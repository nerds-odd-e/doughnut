import { render } from 'ink-testing-library'
import type { NotebookController } from 'donut-api'
import { describe, expect, test } from 'vitest'
import {
  extendInkRenderForInteractiveTests,
  pendingUntilAbort,
  pressEscapeAndWaitForCancelledLine,
} from './inkTestHelpers.js'
import { myNotebooksApiRow } from './myNotebooksApiRow.js'
import { waitNotebookStageActive } from './useNotebookSlashCommand.waits.js'
import {
  installUseNotebookStageConfig,
  notebookStageTestAppElement,
} from './useNotebookSlashCommand.testHelpers.js'

describe('useNotebookSlashCommand resolve', () => {
  const { getMyNotebooksSpy } = installUseNotebookStageConfig()

  test('active stage /exit clears notebook shell and records assistant line', async () => {
    getMyNotebooksSpy().mockResolvedValue({
      data: { notebooks: [myNotebooksApiRow('Top Maths')] },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const result = render(notebookStageTestAppElement('Top Maths'))
    const ink = extendInkRenderForInteractiveTests(result)
    await waitNotebookStageActive(ink, 'Top Maths')

    result.stdin.write('/exit\r')
    await ink.waitForLastFrameToInclude('Left notebook context.')
    expect(ink.lastStrippedFrame()).not.toContain('Active notebook: Top Maths')
  })

  test('unknown notebook name shows error and does not enter stage', async () => {
    getMyNotebooksSpy().mockResolvedValue({
      data: { notebooks: [myNotebooksApiRow('Other')] },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const result = render(notebookStageTestAppElement('Missing Notebook'))
    const { lastStrippedFrame, waitForFramesToInclude } = {
      ...result,
      ...extendInkRenderForInteractiveTests(result),
    }

    await waitForFramesToInclude('No notebook found with that name.')
    expect(lastStrippedFrame()).not.toContain('Active notebook:')
  })

  test('myNotebooks HTTP 401 maps to user-visible auth error', async () => {
    getMyNotebooksSpy().mockRejectedValue({ status: 401 })

    const result = render(notebookStageTestAppElement('Top Maths'))
    const { waitForFramesToInclude } = {
      ...result,
      ...extendInkRenderForInteractiveTests(result),
    }

    await waitForFramesToInclude(
      'Access token is invalid or expired. Run doughnut login or configure a new token.'
    )
  })

  test('Escape during notebook list load shows Cancelled when fetch honors signal', async () => {
    getMyNotebooksSpy().mockImplementation(
      async (options: { signal?: AbortSignal }) => {
        const { signal } = options
        if (signal === undefined) {
          throw new Error('expected AbortSignal from /use notebook resolve')
        }
        await pendingUntilAbort(signal)
      }
    )

    const result = render(notebookStageTestAppElement('Top Maths'))
    const { stdin, lastStrippedFrame, waitForFramesToInclude } = {
      ...result,
      ...extendInkRenderForInteractiveTests(result),
    }

    await waitForFramesToInclude('Loading notebooks')
    await pressEscapeAndWaitForCancelledLine(stdin, lastStrippedFrame)
  })

  test('duplicate notebook names show ambiguity error', async () => {
    getMyNotebooksSpy().mockResolvedValue({
      data: {
        notebooks: [myNotebooksApiRow('Same'), myNotebooksApiRow('Same')],
      },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const result = render(notebookStageTestAppElement('Same'))
    const { waitForFramesToInclude } = {
      ...result,
      ...extendInkRenderForInteractiveTests(result),
    }

    await waitForFramesToInclude('Multiple notebooks match')
    await waitForFramesToInclude('Same')
  })

  test('bare /use with empty notebook list shows error', async () => {
    getMyNotebooksSpy().mockResolvedValue({
      data: { notebooks: [] },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const result = render(notebookStageTestAppElement(undefined))
    const { waitForFramesToInclude } = {
      ...result,
      ...extendInkRenderForInteractiveTests(result),
    }

    await waitForFramesToInclude('No notebooks found.')
  })
})
