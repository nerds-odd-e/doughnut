import { describe, expect, test } from 'vitest'
import {
  installMainInteractivePromptConfig,
  lineWithMainPrompt,
  renderMainInteractivePrompt,
} from './mainInteractivePrompt.testHelpers.js'

describe('MainInteractivePrompt Tab completion', () => {
  installMainInteractivePromptConfig()

  test('Tab with several usages sharing a prefix extends draft to longest common prefix', async () => {
    const { stdin, waitForLastFrameToInclude, waitUntilLastFrame } =
      await renderMainInteractivePrompt()

    stdin.write('/rec')
    await waitForLastFrameToInclude('→ /rec')
    stdin.write('\t')
    await waitUntilLastFrame((f) =>
      lineWithMainPrompt(f).trimEnd().includes('→ /recall')
    )
  })

  test('Tab with a unique matching usage completes to usage plus trailing space', async () => {
    const { stdin, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/hel')
    await waitForLastFrameToInclude('→ /hel')
    stdin.write('\t')
    await waitForLastFrameToInclude('→ /help ')
  })

  test('Tab with a unique match does not append <argument> placeholder to the draft', async () => {
    const { stdin, lastStrippedFrame, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/set-acc')
    await waitForLastFrameToInclude('→ /set-acc')
    stdin.write('\t')
    await waitForLastFrameToInclude('→ /set-access-token ')
    expect(lineWithMainPrompt(lastStrippedFrame())).not.toMatch(/<token>/)
  })

  test('Tab with no usage prefix match leaves draft unchanged', async () => {
    const { stdin, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/zzz')
    await waitForLastFrameToInclude('→ /zzz')
    stdin.write('\t')
    await waitForLastFrameToInclude('→ /zzz')
  })
})
