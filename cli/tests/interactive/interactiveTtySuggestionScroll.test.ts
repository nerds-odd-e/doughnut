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

  test('repeated Down moves highlight through a long list; window can show ↑ more above', async () => {
    writeSpy.mockClear()
    typeString(stdin, '/')
    await tick()
    for (let i = 0; i < 8; i++) {
      pressKey(stdin, 'down')
      await tick()
    }

    const output = ttyOutput(writeSpy)
    expect(output).toContain('\x1b[7m')
    expect(output).toContain('↑ more above')
    const lines = output.split('\n')
    const cmd8Lines = lines.filter((l: string) => l.includes('  /cmd8'))
    expect(cmd8Lines.length).toBeGreaterThan(0)
    expect(cmd8Lines[cmd8Lines.length - 1]).toContain('\x1b[7m')
  })

  test('enough Down presses move highlight to the last command in a long list', async () => {
    writeSpy.mockClear()
    typeString(stdin, '/')
    await tick()
    for (let i = 0; i < 11; i++) {
      pressKey(stdin, 'down')
      await tick()
    }

    const output = ttyOutput(writeSpy)
    const lines = output.split('\n')
    const cmd11Lines = lines.filter((l: string) => l.includes('  /cmd11'))
    expect(cmd11Lines.length).toBeGreaterThan(0)
    expect(cmd11Lines[cmd11Lines.length - 1]).toContain('\x1b[7m')
  })
})
