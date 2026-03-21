import './interactiveTestMocks.js'
import { afterEach, beforeEach, describe, expect, test, type vi } from 'vitest'
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

  describe('recalling a prior line from in-memory history', () => {
    test('shows the previous submitted line in the input box after ↑↑ with a new draft', async () => {
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
})
