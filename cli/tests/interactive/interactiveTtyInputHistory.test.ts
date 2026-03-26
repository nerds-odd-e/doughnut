import './interactiveTestMocks.js'
import { afterEach, beforeEach, describe, expect, test, type vi } from 'vitest'
import { stripAnsi } from '../../src/renderer.js'
import {
  endTTYSession,
  pushTTYCommandBytes,
  pushTTYCommandEnter,
  pushTTYCommandKey,
  tick,
  ttyOutput,
  ttySessionWithSpies,
  type TTYStdin,
} from './interactiveTestHelpers.js'

describe('TTY: user input history (↑↓)', () => {
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
      pushTTYCommandBytes(stdin, marker)
      pushTTYCommandEnter(stdin)
      await tick()
      await tick()

      pushTTYCommandBytes(stdin, 'draft')
      await tick()
      pushTTYCommandKey(stdin, 'up')
      await tick()
      pushTTYCommandKey(stdin, 'up')
      await tick()

      expect(stripAnsi(ttyOutput(writeSpy))).toContain(`→ ${marker}`)
    })
  })
})
