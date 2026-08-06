import { describe, expect, it, vi } from 'vitest'
import { handleSelectListInkKey } from '../src/interactions/selectListInteraction.js'
import {
  emptyKey,
  slashAndNumber,
} from './selectListInteraction.testHelpers.js'

describe('handleSelectListInkKey (slash-and-number)', () => {
  it('typed digit does not submit until Enter', () => {
    const onSubmitWithLine = vi.fn()
    handleSelectListInkKey(
      '2',
      emptyKey,
      '',
      0,
      3,
      slashAndNumber(3),
      'signal-escape',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onSubmitWithLine,
        onEditChar: vi.fn(),
      }
    )
    expect(onSubmitWithLine).not.toHaveBeenCalled()
  })

  it('Enter submits parsed draft line', () => {
    const onSubmitWithLine = vi.fn()
    handleSelectListInkKey(
      '\r',
      { return: true },
      '2',
      0,
      3,
      slashAndNumber(3),
      'signal-escape',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onSubmitWithLine,
      }
    )
    expect(onSubmitWithLine).toHaveBeenCalledWith('2')
  })

  it('Escape signals only', () => {
    const onEscapeSignaled = vi.fn()
    handleSelectListInkKey(
      '\u001b',
      {},
      '',
      0,
      2,
      slashAndNumber(2),
      'signal-escape',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onEscapeSignaled,
      }
    )
    expect(onEscapeSignaled).toHaveBeenCalledOnce()
  })

  it('Enter trims draft before mapping', () => {
    const onSubmitWithLine = vi.fn()
    handleSelectListInkKey(
      '\r',
      { return: true },
      '  2  ',
      0,
      3,
      slashAndNumber(3),
      'signal-escape',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onSubmitWithLine,
      }
    )
    expect(onSubmitWithLine).toHaveBeenCalledWith('2')
  })

  it('invalid draft passes through when no invalid handler', () => {
    const onSubmitWithLine = vi.fn()
    handleSelectListInkKey(
      '\r',
      { return: true },
      '99',
      1,
      3,
      slashAndNumber(3),
      'signal-escape',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onSubmitWithLine,
      }
    )
    expect(onSubmitWithLine).toHaveBeenCalledWith('99')
  })

  it('invalid draft invokes onInvalidSelectListSubmitLine when provided', () => {
    const onSubmitWithLine = vi.fn()
    const onInvalidSelectListSubmitLine = vi.fn()
    handleSelectListInkKey(
      '\r',
      { return: true },
      '99',
      1,
      3,
      slashAndNumber(3),
      'signal-escape',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onSubmitWithLine,
        onInvalidSelectListSubmitLine,
      }
    )
    expect(onInvalidSelectListSubmitLine).toHaveBeenCalledOnce()
    expect(onSubmitWithLine).not.toHaveBeenCalled()
  })

  it('/stop still reaches onSubmitWithLine when onInvalid provided', () => {
    const onSubmitWithLine = vi.fn()
    const onInvalidSelectListSubmitLine = vi.fn()
    handleSelectListInkKey(
      '\r',
      { return: true },
      '  /stop  ',
      0,
      3,
      slashAndNumber(3),
      'signal-escape',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onSubmitWithLine,
        onInvalidSelectListSubmitLine,
      }
    )
    expect(onInvalidSelectListSubmitLine).not.toHaveBeenCalled()
    expect(onSubmitWithLine).toHaveBeenCalledWith('/stop')
  })

  it('backspace edits', () => {
    const onEditBackspace = vi.fn()
    handleSelectListInkKey(
      '',
      { backspace: true },
      '',
      0,
      2,
      slashAndNumber(2),
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onEditBackspace,
      }
    )
    expect(onEditBackspace).toHaveBeenCalledOnce()
  })

  it('typed char edits', () => {
    const onEditChar = vi.fn()
    handleSelectListInkKey(
      'a',
      {},
      '',
      0,
      2,
      slashAndNumber(2),
      'signal-escape',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onEditChar,
      }
    )
    expect(onEditChar).toHaveBeenCalledWith('a')
  })

  it('ctrl suppresses char edit and redraws', () => {
    const onRedraw = vi.fn()
    handleSelectListInkKey(
      'a',
      { ctrl: true },
      '',
      0,
      2,
      slashAndNumber(2),
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onRedraw,
      }
    )
    expect(onRedraw).toHaveBeenCalledOnce()
  })

  it('unhandled key redraws', () => {
    const onRedraw = vi.fn()
    handleSelectListInkKey(
      '',
      { name: 'f1' },
      '',
      0,
      2,
      slashAndNumber(2),
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onRedraw,
      }
    )
    expect(onRedraw).toHaveBeenCalledOnce()
  })
})
