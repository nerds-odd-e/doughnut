import { describe, expect, test } from 'vitest'
import {
  EXPECT_GUIDANCE_MORE_BELOW,
  EXPECT_GUIDANCE_ROW_BUDGET,
  installMainInteractivePromptConfig,
  lineWithMainPrompt,
  renderMainInteractivePrompt,
} from './mainInteractivePrompt.testHelpers.js'

describe('MainInteractivePrompt slash guidance', () => {
  installMainInteractivePromptConfig()

  test('default shows static / commands hint', async () => {
    const { lastStrippedFrame } = await renderMainInteractivePrompt()
    const plain = lastStrippedFrame()
    expect(plain).toContain('/ commands')
    expect(plain).toContain('`exit` to quit.')
  })

  test('partial / prefix shows matching commands with first row bold-highlighted', async () => {
    const { stdin, lastFrame, waitForLastFrameToInclude, waitUntilLastFrame } =
      await renderMainInteractivePrompt()

    stdin.write('/')
    await waitForLastFrameToInclude('→ /')
    stdin.write('he')
    await waitUntilLastFrame(
      (f) => f.includes('/help') && f.includes('List available commands')
    )

    const helpLineRaw = (lastFrame() ?? '')
      .split('\n')
      .find((l) => l.includes('/help') && l.includes('List available'))
    expect(helpLineRaw, 'expected a visible /help completion row').toBeTruthy()
    expect(helpLineRaw).toContain('\x1b[1m')
    expect(helpLineRaw).not.toContain('\x1b[7m')
  })

  test('bare slash shows a fixed-height list with more-below when commands overflow budget', async () => {
    const { stdin, lastStrippedFrame, waitUntilLastFrame } =
      await renderMainInteractivePrompt()

    stdin.write('/')
    await waitUntilLastFrame(
      (f) =>
        lineWithMainPrompt(f).includes('→ /') &&
        f.includes(EXPECT_GUIDANCE_MORE_BELOW)
    )

    const listRows = lastStrippedFrame()
      .split('\n')
      .filter(
        (l) => l.includes(EXPECT_GUIDANCE_MORE_BELOW) || l.includes('  /')
      )
    expect(listRows.length).toBe(EXPECT_GUIDANCE_ROW_BUDGET)
  })

  test('trailing space after slash command shows hint only, not the completion list', async () => {
    const { stdin, lastStrippedFrame, waitForLastFrameToInclude } =
      await renderMainInteractivePrompt()

    stdin.write('/help ')
    await waitForLastFrameToInclude('→ /help ')
    const frame = lastStrippedFrame()
    expect(frame).toContain('/ commands')
    expect(frame).not.toMatch(/\/help\s+List available commands/)
  })
})
