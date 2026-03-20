import { interactiveHelpMockState } from './interactiveTestMocks.js'
import { describe, test, expect, type vi, beforeEach, afterEach } from 'vitest'
import { resetRecallStateForTesting } from '../../src/interactive.js'
import {
  endTTYSession,
  pressKey,
  startTTYSessionWithoutRecallReset,
  tick,
  ttyOutput,
  typeString,
  type TTYStdin,
} from './interactiveTestHelpers.js'

describe('TTY mode slash command suggestions with scroll', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    resetRecallStateForTesting()
    interactiveHelpMockState.useManyCommandsForScrollTests = true
    ;({ stdin, writeSpy } = await startTTYSessionWithoutRecallReset())
  })

  afterEach(() => {
    interactiveHelpMockState.useManyCommandsForScrollTests = false
    endTTYSession(stdin)
  })

  test('shows "↑ more above" when scrolled down', async () => {
    writeSpy.mockClear()
    typeString(stdin, '/')
    await tick()
    for (let i = 0; i < 8; i++) {
      pressKey(stdin, 'down')
      await tick()
    }

    expect(ttyOutput(writeSpy)).toContain('↑ more above')
  })

  test('hides "↓ more below" when at bottom', async () => {
    writeSpy.mockClear()
    typeString(stdin, '/')
    await tick()
    for (let i = 0; i < 11; i++) {
      pressKey(stdin, 'down')
      await tick()
    }

    const output = ttyOutput(writeSpy)
    const lastMoreBelow = output.lastIndexOf('↓ more below')
    const lastCmd11 = output.lastIndexOf('/cmd11')
    expect(lastCmd11).toBeGreaterThan(lastMoreBelow)
  })
})
