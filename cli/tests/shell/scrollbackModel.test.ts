import { describe, expect, test } from 'vitest'
import {
  commandTurnBufferAppendError,
  commandTurnBufferAppendLog,
  commandTurnBufferAppendUserNotice,
  emptyCommandTurnBuffer,
  scrollbackAppendOutput,
  scrollbackCommitInputLine,
  scrollbackFlushCommandTurnIfNonEmpty,
} from '../../src/shell/scrollbackModel.js'

describe('scrollbackModel', () => {
  test('command turn: log sets plain tone and splits newlines', () => {
    let b = emptyCommandTurnBuffer()
    b = commandTurnBufferAppendLog(b, 'a\nb')
    expect(b).toEqual({ tone: 'plain', lines: ['a', 'b'] })
    b = commandTurnBufferAppendLog(b, 'c')
    expect(b.tone).toBe('plain')
    expect(b.lines).toEqual(['a', 'b', 'c'])
  })

  test('command turn: log after error then log resets tone to plain', () => {
    let b = commandTurnBufferAppendError(
      emptyCommandTurnBuffer(),
      new Error('e')
    )
    expect(b.tone).toBe('error')
    b = commandTurnBufferAppendLog(b, 'ok')
    expect(b.tone).toBe('plain')
    expect(b.lines).toEqual(['e', 'ok'])
  })

  test('command turn: logUserNotice sets userNotice tone', () => {
    const b = commandTurnBufferAppendUserNotice(
      emptyCommandTurnBuffer(),
      'cancelled'
    )
    expect(b).toEqual({ tone: 'userNotice', lines: ['cancelled'] })
  })

  test('scrollback append output and input are append-only', () => {
    let h = scrollbackAppendOutput([], ['out'], 'plain')
    h = scrollbackCommitInputLine(h, 'in')
    h = scrollbackAppendOutput(h, ['x'], 'error')
    expect(h).toEqual([
      { type: 'output', lines: ['out'], tone: 'plain' },
      { type: 'input', content: 'in' },
      { type: 'output', lines: ['x'], tone: 'error' },
    ])
  })

  test('flush command turn when empty leaves history and buffer unchanged', () => {
    const history = scrollbackCommitInputLine([], 'x')
    const turn = emptyCommandTurnBuffer()
    const r = scrollbackFlushCommandTurnIfNonEmpty(history, turn)
    expect(r.history).toBe(history)
    expect(r.turn).toBe(turn)
  })

  test('flush command turn appends one output block and clears buffer', () => {
    const turn = commandTurnBufferAppendLog(emptyCommandTurnBuffer(), 'line')
    const r = scrollbackFlushCommandTurnIfNonEmpty([], turn)
    expect(r.history).toEqual([
      { type: 'output', lines: ['line'], tone: 'plain' },
    ])
    expect(r.turn).toEqual(emptyCommandTurnBuffer())
  })
})
