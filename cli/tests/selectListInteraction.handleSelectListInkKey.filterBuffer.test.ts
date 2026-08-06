import { describe, expect, it, vi } from 'vitest'
import { handleSelectListInkKey } from '../src/interactions/selectListInteraction.js'
import { filterBuffer } from './selectListInteraction.testHelpers.js'

describe('handleSelectListInkKey (filter-buffer)', () => {
  it('typed char edits', () => {
    const onEditChar = vi.fn()
    handleSelectListInkKey('x', {}, '', 1, 3, filterBuffer, 'abort-list', {
      onSetHighlightIndex: vi.fn(),
      onSubmitHighlightIndex: vi.fn(),
      onEditChar,
    })
    expect(onEditChar).toHaveBeenCalledWith('x')
  })

  it('backspace edits', () => {
    const onEditBackspace = vi.fn()
    handleSelectListInkKey(
      '',
      { backspace: true },
      '',
      1,
      3,
      filterBuffer,
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onEditBackspace,
      }
    )
    expect(onEditBackspace).toHaveBeenCalledOnce()
  })

  it('Enter submits highlight', () => {
    const onSubmitHighlightIndex = vi.fn()
    handleSelectListInkKey(
      '\r',
      { return: true },
      '',
      2,
      3,
      filterBuffer,
      'abort-list',
      { onSetHighlightIndex: vi.fn(), onSubmitHighlightIndex }
    )
    expect(onSubmitHighlightIndex).toHaveBeenCalledWith(2)
  })

  it('Escape aborts', () => {
    const onAbortHighlightOnlyList = vi.fn()
    handleSelectListInkKey('\u001b', {}, '', 0, 2, filterBuffer, 'abort-list', {
      onSetHighlightIndex: vi.fn(),
      onSubmitHighlightIndex: vi.fn(),
      onAbortHighlightOnlyList,
    })
    expect(onAbortHighlightOnlyList).toHaveBeenCalledOnce()
  })

  it('unhandled key redraws', () => {
    const onRedraw = vi.fn()
    handleSelectListInkKey(
      '',
      { name: 'f1' },
      '',
      0,
      2,
      filterBuffer,
      'abort-list',
      {
        onSetHighlightIndex: vi.fn(),
        onSubmitHighlightIndex: vi.fn(),
        onRedraw,
      }
    )
    expect(onRedraw).toHaveBeenCalledOnce()
  })

  it('arrow and Enter are no-ops when list empty', () => {
    const onSetHighlightIndex = vi.fn()
    const onSubmitHighlightIndex = vi.fn()
    handleSelectListInkKey(
      '',
      { downArrow: true },
      '',
      0,
      0,
      filterBuffer,
      'abort-list',
      {
        onSetHighlightIndex,
        onSubmitHighlightIndex,
        onRedraw: vi.fn(),
      }
    )
    expect(onSetHighlightIndex).not.toHaveBeenCalled()

    handleSelectListInkKey(
      '\r',
      { return: true },
      '',
      0,
      0,
      filterBuffer,
      'abort-list',
      { onSetHighlightIndex: vi.fn(), onSubmitHighlightIndex }
    )
    expect(onSubmitHighlightIndex).not.toHaveBeenCalled()
  })
})
