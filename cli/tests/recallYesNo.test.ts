import { describe, expect, it } from 'vitest'
import {
  parseRecallPipedYesNo,
  recallStopConfirmInkModel,
  RECALL_STOP_CONFIRM_QUESTION_LINE,
} from '../src/interactions/recallYesNo.js'

describe('parseRecallPipedYesNo', () => {
  it('treats empty line as invalid', () => {
    expect(parseRecallPipedYesNo('')).toBe('invalid')
    expect(parseRecallPipedYesNo('   ')).toBe('invalid')
  })

  it('accepts y, yes, n, no case-insensitively', () => {
    expect(parseRecallPipedYesNo('y')).toBe('yes')
    expect(parseRecallPipedYesNo('YES')).toBe('yes')
    expect(parseRecallPipedYesNo('n')).toBe('no')
    expect(parseRecallPipedYesNo('No')).toBe('no')
  })

  it('rejects other input', () => {
    expect(parseRecallPipedYesNo('x')).toBe('invalid')
    expect(parseRecallPipedYesNo('maybe')).toBe('invalid')
  })
})

describe('recallStopConfirmInkModel', () => {
  it('builds prompt, placeholder, and confirm question lines for the Ink strip', () => {
    const model = recallStopConfirmInkModel('y or n; Esc to go back')
    expect(model.promptLines).toEqual([])
    expect(model.placeholder).toBe('y or n; Esc to go back')
    expect(model.confirmQuestionLines).toEqual([
      RECALL_STOP_CONFIRM_QUESTION_LINE,
    ])
  })
})
