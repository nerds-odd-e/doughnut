import { describe, test } from 'vitest'
import {
  installMainInteractivePromptConfig,
  mainPromptDraftAfterArrow,
  rawLineIncludesBoldMarker,
  renderMainInteractivePrompt,
  stripAnsi,
} from './mainInteractivePrompt.testHelpers.js'

describe('MainInteractivePrompt caret and slash arrows', () => {
  installMainInteractivePromptConfig()

  test('with list visible and caret at end, each down advances highlight to the next matching usage', async () => {
    const { stdin, waitForLastFrameRaw, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/re')
    await waitForLastFrameToInclude('/recall')

    stdin.write('\x1b[B')
    await waitForLastFrameRaw((raw) =>
      rawLineIncludesBoldMarker(
        raw,
        'Show how many notes are due for recall today'
      )
    )

    stdin.write('\x1b[B')
    await waitForLastFrameRaw((raw) =>
      rawLineIncludesBoldMarker(
        raw,
        'Recall the next due note (just review when no quiz is pe'
      )
    )
  })

  test('with list visible and caret in the middle, first down moves to end then down cycles highlight', async () => {
    const { stdin, waitForLastFrameRaw, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/re')
    await waitForLastFrameToInclude('/recall')

    stdin.write('\x1b[D')
    await waitForLastFrameToInclude('→ /re')

    stdin.write('\x1b[B')
    await waitForLastFrameRaw((raw) =>
      rawLineIncludesBoldMarker(
        raw,
        'Recall the next due note (just review when no quiz is pe'
      )
    )

    stdin.write('\x1b[B')
    await waitForLastFrameRaw((raw) =>
      rawLineIncludesBoldMarker(
        raw,
        'Show how many notes are due for recall today'
      )
    )
  })

  test('with list visible and caret in the middle, up arrow cycles highlight without moving caret home first', async () => {
    const { stdin, waitForLastFrameRaw, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/re')
    await waitForLastFrameToInclude('/recall')

    stdin.write('\x1b[D')
    await waitForLastFrameToInclude('→ /re')

    stdin.write('\x1b[A')
    try {
      await waitForLastFrameRaw(
        (r) =>
          rawLineIncludesBoldMarker(
            r,
            'Show how many notes are due for recall today'
          ) && mainPromptDraftAfterArrow(stripAnsi(r)) === '/re'
      )
    } catch (err) {
      throw new Error(
        'With slash list open and caret between /r and e, the first Up should wrap the completion highlight to the last matching row (/recall-status for draft /re) and keep the caret after /r, not move the caret to the start of the line (history-style home). ' +
          (err instanceof Error ? err.message : String(err))
      )
    }
  })
})
