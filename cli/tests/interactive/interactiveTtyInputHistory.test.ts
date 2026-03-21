import './interactiveTestMocks.js'
import { describe, test, expect, type vi, beforeEach, afterEach } from 'vitest'
import { stripAnsi } from '../../src/renderer.js'
import {
  endTTYSession,
  pressEnter,
  pressKey,
  tick,
  ttyOutput,
  ttySessionWithSpies,
  typeString,
  type TTYStdin,
} from './interactiveTestHelpers.js'

describe('TTY: input command history (↑↓)', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    ;({ stdin, writeSpy } = await ttySessionWithSpies())
  })

  afterEach(() => {
    endTTYSession(stdin)
  })

  test('after submitting a line, ↑↑ recalls it into the input box', async () => {
    const marker = 'history-recall-xyz'
    typeString(stdin, marker)
    pressEnter(stdin)
    await tick()
    await tick()

    typeString(stdin, 'draft')
    await tick()
    pressKey(stdin, 'up')
    await tick()
    pressKey(stdin, 'up')
    await tick()

    expect(stripAnsi(ttyOutput(writeSpy))).toContain(`→ ${marker}`)
  })
})
