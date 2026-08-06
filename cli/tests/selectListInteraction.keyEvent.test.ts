import { describe, expect, it } from 'vitest'
import { selectListKeyEventFromInk } from '../src/interactions/selectListInteraction.js'
import { emptyKey } from './selectListInteraction.testHelpers.js'

describe('selectListKeyEventFromInk', () => {
  it('treats bare CR/LF as submit, not typed text', () => {
    expect(selectListKeyEventFromInk('\n', emptyKey, 'draft')).toMatchObject({
      submitPressed: true,
      str: undefined,
      lineDraft: 'draft',
    })
    expect(selectListKeyEventFromInk('\r', emptyKey, '')).toMatchObject({
      submitPressed: true,
      str: undefined,
    })
  })

  it('maps a plain character with empty key flags', () => {
    expect(selectListKeyEventFromInk('a', emptyKey, '')).toMatchObject({
      submitPressed: false,
      str: 'a',
    })
  })
})
