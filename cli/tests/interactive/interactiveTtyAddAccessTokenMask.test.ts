import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest'

vi.mock('../../src/commands/accessToken.js', async (importOriginal) => {
  const actual =
    await importOriginal<typeof import('../../src/commands/accessToken.js')>()
  return {
    ...actual,
    addAccessToken: vi.fn().mockResolvedValue(undefined),
  }
})

import './interactiveTestMocks.js'
import { stripAnsi } from '../../src/renderer.js'
import {
  endTTYSession,
  pushTTYCommandBytes,
  pushTTYCommandEnter,
  tick,
  ttyOutput,
  ttySessionWithSpies,
  type TTYStdin,
} from './interactiveTestHelpers.js'

describe('TTY: /add-access-token masking in past user messages', () => {
  let writeSpy: ReturnType<typeof vi.spyOn>
  let stdin: TTYStdin

  beforeEach(async () => {
    ;({ stdin, writeSpy } = await ttySessionWithSpies())
  })

  afterEach(() => {
    endTTYSession(stdin)
  })

  test('history grey block shows redacted line, not the raw token', async () => {
    const secret = 'tty-mask-secret-never-leak'
    pushTTYCommandBytes(stdin, `/add-access-token ${secret}`)
    await tick()
    pushTTYCommandEnter(stdin)
    await tick()
    await tick()
    await tick()

    const out = stripAnsi(ttyOutput(writeSpy))
    expect(out).toContain('/add-access-token <redacted>')
    const fromCommittedHistory = out.slice(
      out.indexOf('/add-access-token <redacted>')
    )
    expect(fromCommittedHistory).toContain('Token added')
    expect(fromCommittedHistory).not.toContain(secret)
  })
})
