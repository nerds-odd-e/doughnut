import { describe, expect, test, vi } from 'vitest'
import { pressEscape } from './inkTestHelpers.js'
import {
  installMainInteractivePromptConfig,
  lineWithMainPrompt,
  mainPromptDraftAfterArrow,
  rawLineIncludesBoldMarker,
  renderMainInteractivePrompt,
} from './mainInteractivePrompt.testHelpers.js'

describe('MainInteractivePrompt Enter picks completion', () => {
  installMainInteractivePromptConfig()

  test('Enter with list open replaces draft with highlighted usage plus space and does not commit', async () => {
    const onCommittedLine = vi.fn()
    const {
      stdin,
      waitForLastFrameRaw,
      waitForLastFrameToInclude,
      waitUntilLastFrame,
    } = await renderMainInteractivePrompt(onCommittedLine)
    stdin.write('/re')
    await waitForLastFrameToInclude('/recall')
    stdin.write('\x1b[B')
    await waitForLastFrameRaw((raw) =>
      rawLineIncludesBoldMarker(
        raw,
        'Show how many notes are due for recall today'
      )
    )
    stdin.write('\r')
    await waitUntilLastFrame((f) =>
      lineWithMainPrompt(f).includes('/recall-status ')
    )
    expect(onCommittedLine).not.toHaveBeenCalled()
  })

  test('Enter with list open and default highlight picks first match usage plus space', async () => {
    const onCommittedLine = vi.fn()
    const { stdin, waitForLastFrameToInclude, waitUntilLastFrame } =
      await renderMainInteractivePrompt(onCommittedLine)
    stdin.write('/re')
    await waitForLastFrameToInclude('/recall')
    stdin.write('\r')
    await waitUntilLastFrame((f) => lineWithMainPrompt(f).includes('/recall '))
    expect(onCommittedLine).not.toHaveBeenCalled()
  })
})

describe('MainInteractivePrompt Esc dismiss', () => {
  installMainInteractivePromptConfig()

  test('Esc on bare / clears the draft', async () => {
    const {
      stdin,
      lastStrippedFrame,
      waitForLastFrameToInclude,
      waitUntilLastFrame,
    } = await renderMainInteractivePrompt()

    stdin.write('/')
    await waitForLastFrameToInclude('→ /')
    await pressEscape(stdin)
    await waitUntilLastFrame((f) => !lineWithMainPrompt(f).includes('/'))
    const promptLine = lineWithMainPrompt(lastStrippedFrame()).trimEnd()
    expect(promptLine).toContain('→')
    expect(promptLine).not.toContain('/')
  })

  test('Esc on /he with list hides list and keeps draft; typing restores list', async () => {
    const { stdin, waitUntilLastFrame } = await renderMainInteractivePrompt()

    stdin.write('/he')
    await waitUntilLastFrame(
      (f) => f.includes('/help') && f.includes('List available commands')
    )
    await pressEscape(stdin)
    await waitUntilLastFrame(
      (f) =>
        f.includes('/ commands') &&
        mainPromptDraftAfterArrow(f) === '/he' &&
        !f.includes('List available commands')
    )

    stdin.write('l')
    await waitUntilLastFrame(
      (f) => f.includes('/help') && f.includes('List available commands')
    )
  })
})
