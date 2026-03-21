import { interactiveHelpMockState } from './interactiveTestMocks.js'
import { afterEach, beforeEach, describe, expect, test, type vi } from 'vitest'
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

  test('many down arrows do not scroll the long suggestion list; first item stays highlighted', async () => {
    writeSpy.mockClear()
    typeString(stdin, '/')
    await tick()
    for (let i = 0; i < 8; i++) {
      pressKey(stdin, 'down')
      await tick()
    }

    const output = ttyOutput(writeSpy)
    expect(output).toContain('\x1b[7m')
    expect(output).toContain('/cmd0')
    expect(output).not.toContain('↑ more above')
  })

  test('many down presses do not reveal last command in the guidance window', async () => {
    writeSpy.mockClear()
    typeString(stdin, '/')
    await tick()
    for (let i = 0; i < 11; i++) {
      pressKey(stdin, 'down')
      await tick()
    }

    const output = ttyOutput(writeSpy)
    expect(output).not.toContain('/cmd11')
  })
})
