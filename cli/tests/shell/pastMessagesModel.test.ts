import { describe, expect, test } from 'vitest'
import {
  commandTurnBufferAppendLog,
  emptyCommandTurnBuffer,
  pastMessagesAppendCliAssistantBlock,
  pastMessagesCommitUserLine,
  pastMessagesFlushCommandTurnIfNonEmpty,
} from '../../src/shell/pastMessagesModel.js'

describe('pastMessagesModel', () => {
  test('pastMessages append CLI assistant block and user line are append-only', () => {
    let h = pastMessagesAppendCliAssistantBlock([], ['out'], 'plain')
    h = pastMessagesCommitUserLine(h, 'in')
    h = pastMessagesAppendCliAssistantBlock(h, ['x'], 'error')
    expect(h).toEqual([
      { role: 'cli-assistant', lines: ['out'], tone: 'plain' },
      { role: 'user', content: 'in' },
      { role: 'cli-assistant', lines: ['x'], tone: 'error' },
    ])
  })

  test('flush non-empty turn appends one CLI assistant block', () => {
    const turn = commandTurnBufferAppendLog(emptyCommandTurnBuffer(), 'line')
    const history = pastMessagesCommitUserLine([], 'x')
    const r = pastMessagesFlushCommandTurnIfNonEmpty(history, turn)
    expect(r.pastMessages).toEqual([
      { role: 'user', content: 'x' },
      { role: 'cli-assistant', lines: ['line'], tone: 'plain' },
    ])
    expect(r.turn.lines).toEqual([])
  })

  test('flush empty turn leaves past messages unchanged', () => {
    const r = pastMessagesFlushCommandTurnIfNonEmpty(
      [],
      emptyCommandTurnBuffer()
    )
    expect(r.pastMessages).toEqual([])
  })
})
