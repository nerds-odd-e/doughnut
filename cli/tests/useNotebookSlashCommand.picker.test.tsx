import type { NotebookController } from 'donut-api'
import { describe, test } from 'vitest'
import { pressEscapeAndWaitForCancelledLine } from './inkTestHelpers.js'
import { myNotebooksApiRow } from './myNotebooksApiRow.js'
import {
  installUseNotebookStageConfig,
  renderNotebookStageWhenPickerVisible,
} from './useNotebookSlashCommand.testHelpers.js'

describe('useNotebookSlashCommand picker', () => {
  const { getMyNotebooksSpy } = installUseNotebookStageConfig()

  test('bare /use shows picker; Enter selects first notebook', async () => {
    getMyNotebooksSpy().mockResolvedValue({
      data: {
        notebooks: [myNotebooksApiRow('Alpha'), myNotebooksApiRow('Beta')],
      },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const { stdin, waitForFramesToInclude } =
      await renderNotebookStageWhenPickerVisible()

    stdin.write('\r')
    await waitForFramesToInclude('Active notebook: Alpha')
  })

  test('bare /use picker: Down + Enter selects second notebook', async () => {
    getMyNotebooksSpy().mockResolvedValue({
      data: {
        notebooks: [myNotebooksApiRow('Alpha'), myNotebooksApiRow('Beta')],
      },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const { stdin, waitForLastFrameToInclude, waitUntilLastFrame } =
      await renderNotebookStageWhenPickerVisible()

    stdin.write('\u001b[B')
    await waitUntilLastFrame((f) => f.includes('Beta') && f.includes('2.'))
    stdin.write('\r')
    await waitForLastFrameToInclude('Active notebook: Beta')
  })

  test('bare /use Esc on picker settles Cancelled', async () => {
    getMyNotebooksSpy().mockResolvedValue({
      data: {
        notebooks: [myNotebooksApiRow('Alpha'), myNotebooksApiRow('Beta')],
      },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const { stdin, lastStrippedFrame } =
      await renderNotebookStageWhenPickerVisible()

    await pressEscapeAndWaitForCancelledLine(stdin, lastStrippedFrame)
  })

  test('bare /use picker: typing filters list; Enter selects highlighted match', async () => {
    getMyNotebooksSpy().mockResolvedValue({
      data: {
        notebooks: [
          myNotebooksApiRow('Alpha'),
          myNotebooksApiRow('Beta'),
          myNotebooksApiRow('Alpaca'),
        ],
      },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const {
      stdin,
      waitForLastFrameToInclude,
      waitForFramesToInclude,
      waitUntilLastFrame,
    } = await renderNotebookStageWhenPickerVisible()

    stdin.write('b')
    await waitForLastFrameToInclude('Filter: b')
    stdin.write('e')
    await waitForLastFrameToInclude('Filter: be')
    stdin.write('t')
    await waitForLastFrameToInclude('Filter: bet')
    await waitUntilLastFrame(
      (f) => f.includes('Beta') && !f.includes('Alpha') && !f.includes('Alpaca')
    )

    stdin.write('\r')
    await waitForFramesToInclude('Active notebook: Beta')
  })

  test('bare /use picker: filter with no matches; backspace restores list', async () => {
    getMyNotebooksSpy().mockResolvedValue({
      data: {
        notebooks: [
          myNotebooksApiRow('Alpha'),
          myNotebooksApiRow('Beta'),
          myNotebooksApiRow('Alpaca'),
        ],
      },
    } as Awaited<ReturnType<typeof NotebookController.myNotebooks>>)

    const { stdin, waitForLastFrameToInclude, waitUntilLastFrame } =
      await renderNotebookStageWhenPickerVisible()

    stdin.write('xyz')
    await waitForLastFrameToInclude('Filter: xyz')
    await waitForLastFrameToInclude('No matching notebooks.')

    stdin.write('\x7f')
    await waitUntilLastFrame(
      (f) => f.includes('Filter: xy') && f.includes('No matching notebooks.')
    )
    stdin.write('\x7f')
    await waitUntilLastFrame(
      (f) => f.includes('Filter: x') && f.includes('No matching notebooks.')
    )
    stdin.write('\x7f')
    await waitUntilLastFrame(
      (f) =>
        f.includes('Alpha') &&
        f.includes('Beta') &&
        f.includes('Alpaca') &&
        !f.includes('No matching notebooks.')
    )
  })
})
